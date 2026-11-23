package com.example.business_assistant.service.impl;

import com.example.business_assistant.bean.Document;
import com.example.business_assistant.common.BusinessException;
import com.example.business_assistant.common.PageResult;
import com.example.business_assistant.dto.BatchUploadResult;
import com.example.business_assistant.dto.DocumentVO;
import com.example.business_assistant.mapper.DocumentMapper;
import com.example.business_assistant.service.DocumentService;
import com.example.business_assistant.service.RagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 文档 Service 实现类
 */
@Slf4j
@Service
public class DocumentServiceImpl implements DocumentService {

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private RagService ragService;

    @Autowired
    private org.springframework.ai.chat.client.ChatClient chatClient;

    @Value("${docuagent.upload-dir:./upload}")
    private String uploadDir;

    private static final Set<String> ALLOWED_TYPES = Set.of("pdf", "docx", "md", "txt");
    private static final long MAX_SIZE = 20 * 1024 * 1024;

    @Override
    public DocumentVO upload(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("请选择要上传的文件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw BusinessException.badRequest("文件大小不能超过 20MB");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            throw BusinessException.badRequest("文件格式不支持");
        }
        String ext = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_TYPES.contains(ext)) {
            throw BusinessException.badRequest("仅支持 PDF / Word / Markdown / TXT 格式");
        }

        String savedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dirPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dirPath);
            File dest = new File(dirPath.toFile(), savedName);
            file.transferTo(dest);
        } catch (IOException e) {
            throw BusinessException.badRequest("文件保存失败");
        }

        Document doc = new Document();
        doc.setUserId(userId);
        doc.setFilename(originalName);
        doc.setFileType(ext);
        doc.setFilePath(savedName);
        doc.setSize(file.getSize());
        doc.setStatus("ready");
        documentMapper.insert(doc);

        // 上传成功后自动做 RAG 索引（切分 + 向量化）
        try {
            int chunks = ragService.indexDocument(doc);
            doc.setChunkCount(chunks);
            doc.setStatus("ready");
            // 让 AI 生成 100 字以内的文档摘要
            doc.setSummary(generateSummary(doc));
        } catch (Exception e) {
            log.warn("文档索引失败: {}", e.getMessage());
            doc.setStatus("failed");
        }
        documentMapper.updateIndexResult(doc.getId(), doc.getChunkCount(), doc.getStatus(), doc.getSummary());

        return toVO(doc);
    }

    @Override
    public BatchUploadResult uploadBatch(MultipartFile[] files, Long userId) {
        // 1. 参数校验：空 / 超过上限
        if (files == null || files.length == 0) {
            throw BusinessException.badRequest("请选择要上传的文件");
        }
        int maxBatch = 10;
        if (files.length > maxBatch) {
            throw BusinessException.badRequest("一次最多上传 " + maxBatch + " 个文件，当前 " + files.length + " 个");
        }

        // 2. 逐个处理：单个失败不中断整批（fail-tolerant）
        List<BatchUploadResult.Item> items = new ArrayList<>();
        int success = 0;
        int failure = 0;
        for (MultipartFile file : files) {
            String originalName = file.getOriginalFilename();
            try {
                DocumentVO vo = upload(file, userId);
                items.add(new BatchUploadResult.Item(originalName, true, vo, null));
                success++;
            } catch (Exception e) {
                String msg = e.getMessage() == null ? "未知错误" : e.getMessage();
                items.add(new BatchUploadResult.Item(originalName, false, null, msg));
                failure++;
                log.warn("批量上传中 {} 失败: {}", originalName, msg);
            }
        }
        log.info("批量上传完成: 共 {} 个, 成功 {}, 失败 {}", files.length, success, failure);
        return new BatchUploadResult(items, success, failure);
    }

    @Override
    public PageResult<DocumentVO> list(String keyword, int page, int size, Long userId) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;
        int offset = (page - 1) * size;

        List<Document> docs = documentMapper.selectPage(userId, keyword, offset, size);
        long total = documentMapper.count(userId, keyword);

        List<DocumentVO> voList = docs.stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(voList, total, page, size);
    }

    @Override
    public DocumentVO getById(Long id, Long userId) {
        Document doc = getDocumentOrThrow(id, userId);
        return toVO(doc);
    }

    @Override
    public String getContent(Long id, Long userId) {
        Document doc = getDocumentOrThrow(id, userId);
        if ("txt".equals(doc.getFileType()) || "md".equals(doc.getFileType())) {
            try {
                Path path = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(doc.getFilePath());
                return Files.readString(path, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw BusinessException.badRequest("读取文件失败");
            }
        }
        return "[" + doc.getFileType().toUpperCase() + " 格式原文预览将在下个版本支持]";
    }

    @Override
    public void delete(Long id, Long userId) {
        Document doc = getDocumentOrThrow(id, userId);
        // 先删向量库中的片段，避免残留被检索到
        ragService.deleteByDocId(id);
        documentMapper.deleteByIdAndUserId(id, userId);
        try {
            Files.deleteIfExists(Paths.get(uploadDir).toAbsolutePath().normalize().resolve(doc.getFilePath()));
        } catch (IOException e) {
            log.warn("物理文件删除失败: {}", e.getMessage());
        }
    }

    /**
     * 重新索引指定文档（用于历史文档补切分 / 解析失败后重试）
     */
    public DocumentVO reindex(Long id, Long userId) {
        Document doc = getDocumentOrThrow(id, userId);
        // 先清掉旧向量，避免重复入库
        ragService.deleteByDocId(id);
        try {
            int chunks = ragService.indexDocument(doc);
            doc.setChunkCount(chunks);
            doc.setStatus("ready");
            doc.setSummary(generateSummary(doc));
        } catch (Exception e) {
            log.warn("重新索引失败: {}", e.getMessage());
            doc.setStatus("failed");
        }
        documentMapper.updateIndexResult(doc.getId(), doc.getChunkCount(), doc.getStatus(), doc.getSummary());
        return toVO(doc);
    }

    /**
     * 调用大模型生成 100 字以内的文档摘要。
     * 失败时返回 null，不影响主流程。
     */
    private String generateSummary(Document doc) {
        try {
            Path filePath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(doc.getFilePath());
            String text;
            if ("txt".equals(doc.getFileType()) || "md".equals(doc.getFileType())) {
                text = Files.readString(filePath);
            } else {
                org.springframework.ai.reader.tika.TikaDocumentReader reader =
                        new org.springframework.ai.reader.tika.TikaDocumentReader(
                                new org.springframework.core.io.FileSystemResource(filePath.toFile()));
                StringBuilder sb = new StringBuilder();
                for (org.springframework.ai.document.Document d : reader.get()) {
                    sb.append(d.getText()).append("\n");
                }
                text = sb.toString();
            }
            // 只取前 1500 字送摘要，省 token
            String sample = text.length() > 1500 ? text.substring(0, 1500) : text;
            String summary = chatClient.prompt()
                    .user("请用 2~3 句话（不超过 100 字）概括以下文档的核心内容，直接输出摘要，不要前缀：\n\n" + sample)
                    .call()
                    .content();
            return summary == null ? null : summary.trim();
        } catch (Exception e) {
            log.warn("生成摘要失败: {}", e.getMessage());
            return null;
        }
    }

    private Document getDocumentOrThrow(Long id, Long userId) {
        Document doc = documentMapper.selectByIdAndUserId(id, userId);
        if (doc == null) {
            throw BusinessException.notFound("文档不存在或无权访问");
        }
        return doc;
    }

    private DocumentVO toVO(Document doc) {
        DocumentVO vo = new DocumentVO();
        BeanUtils.copyProperties(doc, vo);
        return vo;
    }
}
