package com.example.business_assistant.service.impl;

import com.example.business_assistant.common.UserContext;
import com.example.business_assistant.dto.SystemInfoVO;
import com.example.business_assistant.mapper.ConversationMapper;
import com.example.business_assistant.mapper.DocumentMapper;
import com.example.business_assistant.mapper.MessageMapper;
import com.example.business_assistant.service.AdminSystemService;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** 系统监控服务实现 */
@Slf4j
@Service
public class AdminSystemServiceImpl implements AdminSystemService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long MB = 1024L * 1024L;

    @Autowired private DocumentMapper documentMapper;
    @Autowired private ConversationMapper conversationMapper;
    @Autowired private MessageMapper messageMapper;
    @Autowired private DataSource dataSource;

    @Override
    public SystemInfoVO getSystemInfo() {
        SystemInfoVO vo = new SystemInfoVO();

        // JVM 内存
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = mem.getHeapMemoryUsage();
        MemoryUsage nonHeap = mem.getNonHeapMemoryUsage();
        vo.setHeapUsedMb(heap.getUsed() / MB);
        vo.setHeapMaxMb(heap.getMax() / MB);
        vo.setHeapUsedPercent(heap.getMax() > 0 ? round(heap.getUsed() * 100.0 / heap.getMax()) : 0);
        vo.setNonHeapUsedMb(nonHeap.getUsed() / MB);
        vo.setNonHeapMaxMb(nonHeap.getMax() / MB);

        // CPU 使用率（OperatingSystemMXBean，取不到则为 -1）
        double cpu = -1;
        try {
            com.sun.management.OperatingSystemMXBean os =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            cpu = round(os.getProcessCpuLoad() * 100);
        } catch (Exception e) {
            log.warn("读取 CPU 使用率失败: {}", e.getMessage());
        }
        vo.setCpuUsagePercent(cpu);
        vo.setThreadCount(Thread.activeCount());

        // 运行时长
        RuntimeMXBean rt = ManagementFactory.getRuntimeMXBean();
        long start = rt.getStartTime();
        long uptime = rt.getUptime();
        vo.setStartTime(start);
        vo.setStartTimeText(LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(start),
                java.time.ZoneId.systemDefault()).format(DTF));
        vo.setUptimeMs(uptime);
        vo.setUptimeText(formatDuration(uptime));

        // DB 连接池
        try {
            if (dataSource instanceof HikariDataSource hds) {
                HikariPoolMXBean pool = hds.getHikariPoolMXBean();
                if (pool != null) {
                    vo.setDbActiveConnections(pool.getActiveConnections());
                    vo.setDbIdleConnections(pool.getIdleConnections());
                    vo.setDbTotalConnections(pool.getTotalConnections());
                    vo.setDbMaxConnections(hds.getMaximumPoolSize());
                }
            }
        } catch (Exception e) {
            log.warn("读取 HikariCP 连接池信息失败: {}", e.getMessage());
        }

        // 业务数据量
        vo.setDocumentCount(documentMapper.countAll());
        // 当前登录用户自己的文档数（与"文档管理"页显示口径一致）
        Long currentUserId = UserContext.getUserId();
        vo.setMyDocumentCount(currentUserId == null ? 0 : documentMapper.count(currentUserId, null));
        vo.setConversationCount(conversationMapper.countAll());
        vo.setMessageCount(messageMapper.countAll());

        // 健康检查
        vo.setServerTime(LocalDateTime.now().format(DTF));
        vo.setAppVersion(resolveVersion());
        vo.setJavaVersion(System.getProperty("java.version"));
        vo.setOsName(System.getProperty("os.name"));
        return vo;
    }

    /** 从 pom.xml 读版本号，读不到用兜底值 */
    private String resolveVersion() {
        try {
            Path pom = Paths.get("pom.xml");
            if (Files.exists(pom)) {
                String content = Files.readString(pom, StandardCharsets.UTF_8);
                int i = content.indexOf("<artifactId>Business_Assistant</artifactId>");
                int v = content.indexOf("<version>", i);
                if (v > 0) {
                    int end = content.indexOf("</version>", v);
                    return content.substring(v + 9, end).trim();
                }
            }
        } catch (Exception e) {
            log.warn("读取版本号失败: {}", e.getMessage());
        }
        return "0.0.1-SNAPSHOT";
    }

    @Override
    public List<String> readLogs(String type, int lines) {
        if (lines < 1) lines = 100;
        if (lines > 1000) lines = 1000;
        String file = "error".equalsIgnoreCase(type) ? "logs/error.log" : "logs/info.log";
        Path path = Paths.get(file);
        if (!Files.exists(path)) return Collections.emptyList();
        try {
            return readLastLines(path, lines);
        } catch (IOException e) {
            log.warn("读取日志失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 用 RandomAccessFile 从文件尾部"分块倒读"，避免 Files.readAllLines() 把整个大文件加载进内存。
     * <p>
     * 关键：在【字节层面】按 \n 定位每一行的完整字节区间，再逐行用 UTF-8 解码。
     * 这样即使 8192 字节的块边界切在一个中文字符中间，也不会产生乱码——
     * 因为被切断的那一行字节会与下一块拼接后、作为完整的一行一次性解码。
     * <p>
     * 返回顺序为正常时间顺序（旧->新），前端再倒序展示。
     */
    private List<String> readLastLines(Path path, int maxLines) throws IOException {
        List<String> lines = new ArrayList<>();
        final int BLOCK = 8192;
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
            long fileLength = raf.length();
            if (fileLength == 0) return lines;

            long pos = fileLength;
            byte[] leftover = new byte[0]; // 更靠后那块里、开头被截断的半行字节
            while (pos > 0 && lines.size() < maxLines) {
                int len = (int) Math.min(BLOCK, pos);
                pos -= len;
                raf.seek(pos);
                byte[] block = new byte[len];
                raf.readFully(block);

                // 合并：block(更靠前) + leftover(更靠后) = 从当前位置到文件末尾的连续字节
                byte[] data = new byte[block.length + leftover.length];
                System.arraycopy(block, 0, data, 0, block.length);
                System.arraycopy(leftover, 0, data, block.length, leftover.length);

                // 在字节层面找所有 \n 的位置
                List<Integer> nlPos = new ArrayList<>();
                for (int i = 0; i < data.length; i++) {
                    if (data[i] == '\n') nlPos.add(i);
                }
                if (nlPos.isEmpty()) {
                    // 整块没有换行，全部暂存，继续往前读
                    leftover = data;
                    continue;
                }

                // 从后往前，把每个 \n 之后到行尾的字节作为完整行解码
                int lineEnd = data.length;
                for (int i = nlPos.size() - 1; i >= 0 && lines.size() < maxLines; i--) {
                    int lineStart = nlPos.get(i) + 1;
                    if (lineEnd > lineStart) {
                        String line = new String(data, lineStart, lineEnd - lineStart, StandardCharsets.UTF_8);
                        line = stripCR(line);
                        if (!line.isEmpty()) lines.add(line);
                    }
                    lineEnd = nlPos.get(i); // 下一行（更老）以当前 \n 位置为结束
                }
                // 第一个 \n 之前的字节是半行，保留与下一块拼接
                leftover = Arrays.copyOfRange(data, 0, nlPos.get(0));
            }
            // 文件开头剩余的半行
            if (leftover.length > 0 && lines.size() < maxLines) {
                String line = stripCR(new String(leftover, StandardCharsets.UTF_8));
                if (!line.isEmpty()) lines.add(line);
            }
        }
        // 上面是倒序收集的，翻转为正常顺序
        Collections.reverse(lines);
        return lines;
    }

    private String stripCR(String s) {
        return s.endsWith("\r") ? s.substring(0, s.length() - 1) : s;
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private String formatDuration(long ms) {
        long s = ms / 1000;
        long days = s / 86400;
        long hours = (s % 86400) / 3600;
        long mins = (s % 3600) / 60;
        long secs = s % 60;
        if (days > 0) return days + "天 " + hours + "小时 " + mins + "分";
        if (hours > 0) return hours + "小时 " + mins + "分 " + secs + "秒";
        if (mins > 0) return mins + "分 " + secs + "秒";
        return secs + "秒";
    }
}
