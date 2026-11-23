package com.example.business_assistant.service.impl;

import com.example.business_assistant.bean.Document;
import com.example.business_assistant.config.RagConfig;
import com.example.business_assistant.dto.DocumentFragment;
import com.example.business_assistant.service.RagService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RAG Service 实现类
 * <p>
 * 核心流程：
 *  1. indexDocument：读文件文本 -> 按段落切分（每块约 500 字）-> 每块生成向量 -> 存 VectorStore
 *  2. retrieve：问题向量化 -> VectorStore 搜 top-5 -> 过滤出当前用户的片段
 */
@Slf4j
@Service
public class RagServiceImpl implements RagService {

    @Autowired
    private VectorStore vectorStore;

    /** RAG 可调参数（管理员可热更新） */
    @Autowired
    private RagConfig ragConfig;

    @Value("${docuagent.upload-dir:./upload}")
    private String uploadDir;

    // 参数改为从 ragConfig 读取，实现热更新。保留局部取值方法，语义更清晰。
    private int topK() { return ragConfig.getTopK(); }
    private double similarityThreshold() { return ragConfig.getSimilarityThreshold(); }
    private int maxChunksPerDoc() { return ragConfig.getMaxChunksPerDoc(); }
    private int chunkSize() { return ragConfig.getChunkSize(); }

    /**
     * docId -> 该文档已入库的向量文档 ID 列表。
     * SimpleVectorStore.delete 只支持按向量 ID 删除，
     * 所以索引时要把每个 chunk 的生成 ID 记下来，删文档时按清单清除。
     */
    private final Map<Long, List<String>> docVectorIds = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 文档入库索引。
     * @param doc 文档实体
     * @return 实际切分出的块数；解析失败抛异常，由调用方捕获
     */
    @Override
    public int indexDocument(Document doc) {
        // 1. 读取文件文本：txt/md 直接读，pdf/docx 走 Tika 解析
        Path filePath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(doc.getFilePath());
        String text = extractText(filePath, doc.getFileType());

        if (text.isBlank()) {
            log.warn("文档内容为空，跳过索引: {}", doc.getFilename());
            return 0;
        }

        // 2. 按段落切分，每块不超过 chunkSize 字
        List<String> chunks = splitText(text);
        log.info("文档 {} 切分成 {} 块", doc.getFilename(), chunks.size());

        // 3. 每块包装成 Spring AI 的 Document，带上元数据（docId、userId、filename）
        List<org.springframework.ai.document.Document> documents = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> metadata = Map.of(
                    "docId", doc.getId(),
                    "userId", doc.getUserId(),
                    "filename", doc.getFilename(),
                    "chunkIndex", i
            );
            org.springframework.ai.document.Document d =
                    new org.springframework.ai.document.Document(chunks.get(i), metadata);
            documents.add(d);
        }

        // 4. 批量存入向量库（内部自动调 Embedding 接口）
        vectorStore.add(documents);

        // 5. 记录 docId -> 向量 ID 清单，供删除时使用
        List<String> ids = documents.stream()
                .map(org.springframework.ai.document.Document::getId)
                .toList();
        docVectorIds.put(doc.getId(), ids);
        return chunks.size();
    }

    /**
     * 抽取文件纯文本。
     *  - txt / md：按 UTF-8 直接读
     *  - pdf / docx：用 Apache Tika 解析（TikaDocumentReader 内部会自动处理）
     */
    private String extractText(Path filePath, String fileType) {
        try {
            if ("txt".equals(fileType) || "md".equals(fileType)) {
                return Files.readString(filePath);
            }
            // pdf / docx 走 Tika（必须用 FileSystemResource，否则会按类路径找文件）
            TikaDocumentReader reader = new TikaDocumentReader(
                    new org.springframework.core.io.FileSystemResource(filePath.toFile())
            );
            List<org.springframework.ai.document.Document> docs = reader.get();
            StringBuilder sb = new StringBuilder();
            for (org.springframework.ai.document.Document d : docs) {
                sb.append(d.getText()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("文档解析失败: {}", filePath, e);
            throw new RuntimeException("文档解析失败: " + e.getMessage(), e);
        }
    }

@Override
    public List<DocumentFragment> retrieve(String question, Long userId) {
        try {
            // 1. 向量检索：topK 个最相似片段（注意：SearchRequest 内部会按 score 降序返回）
            List<org.springframework.ai.document.Document> results = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(question)
                            .topK(topK() * 3)  // 多召回一些，后面再过滤
                            .similarityThreshold(similarityThreshold())
                            .build()
            );

            if (results == null || results.isEmpty()) {
                log.info("RAG 检索无结果: question={}, userId={}", question, userId);
                return List.of();
            }

            // 2. 数据隔离：只保留当前用户的 chunk
            List<org.springframework.ai.document.Document> userOwned = new ArrayList<>();
            for (org.springframework.ai.document.Document doc : results) {
                Map<String, Object> metadata = doc.getMetadata();
                Object metaUserId = metadata.get("userId");
                if (metaUserId != null && Long.valueOf(metaUserId.toString()).equals(userId)) {
                    userOwned.add(doc);
                }
            }

            // 3. 按 score 降序排序（SearchRequest 默认已排，但这里保险一下）
            userOwned.sort((a, b) -> {
                Double sa = a.getScore() == null ? 0.0 : a.getScore();
                Double sb = b.getScore() == null ? 0.0 : b.getScore();
                return Double.compare(sb, sa);
            });

            // 4. 同一文档最多保留 maxChunksPerDoc 个 chunk，避免同一文档刷屏
            Map<Long, Integer> docChunkCount = new java.util.LinkedHashMap<>();
            List<DocumentFragment> fragments = new ArrayList<>();
            for (org.springframework.ai.document.Document doc : userOwned) {
                Map<String, Object> metadata = doc.getMetadata();
                Long docId = Long.valueOf(metadata.get("docId").toString());
                Integer count = docChunkCount.getOrDefault(docId, 0);
                if (count >= maxChunksPerDoc()) {
                    continue;
                }
                docChunkCount.put(docId, count + 1);

                Double score = doc.getScore() == null ? 0.0 : doc.getScore();
                Integer chunkIndex = metadata.get("chunkIndex") == null
                        ? 0 : Integer.valueOf(metadata.get("chunkIndex").toString());
                String filename = (String) metadata.get("filename");

                fragments.add(DocumentFragment.of(
                        doc.getText(),
                        docId,
                        filename,
                        score,
                        chunkIndex
                ));
            }

            // 5. 截断到 topK
            if (fragments.size() > topK()) {
                fragments = new ArrayList<>(fragments.subList(0, topK()));
            }

            // 6. 日志：方便调试时看召回质量
            log.info("RAG 召回 {} 个 chunk: question={}, userId={}, topK={}, threshold={}",
                    fragments.size(), question, userId, topK(), similarityThreshold());
            for (DocumentFragment f : fragments) {
                log.info("  - [score={}, doc={}, chunk={}] {}",
                        String.format("%.3f", f.getScore()),
                        f.getFilename(),
                        f.getChunkIndex(),
                        f.getContentPreview());
            }

            return fragments;

        } catch (Exception e) {
            log.error("向量检索失败", e);
            return List.of();
        }
    }

    /**
     * 删除某篇文档在向量库中的全部片段。
     */
    @Override
    public void deleteByDocId(Long docId) {
        List<String> ids = docVectorIds.remove(docId);
        if (ids == null || ids.isEmpty()) {
            log.info("文档 {} 无向量片段需要清理（可能未索引或已清理）", docId);
            return;
        }
        try {
            vectorStore.delete(ids);
            log.info("文档 {} 的 {} 个向量片段已删除", docId, ids.size());
        } catch (Exception e) {
            log.error("删除文档向量片段失败: docId={}", docId, e);
        }
    }

    /**
     * 简单的文本切分：
     *  - 先按段落（双换行）切
     *  - 太长的段落再按字数硬切
     *  - 避免把一句话从中间切断太严重
     */
    private List<String> splitText(String text) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\n\s*\n");

        StringBuilder current = new StringBuilder();
        for (String para : paragraphs) {
            para = para.trim();
            if (para.isEmpty()) continue;

            // 如果当前块 + 新段落不超限，就拼一起
            if (current.length() + para.length() < chunkSize()) {
                if (current.length() > 0) current.append("\n\n");
                current.append(para);
            } else {
                // 先把当前块存起来
                if (current.length() > 0) {
                    chunks.add(current.toString());
                    current = new StringBuilder();
                }
                // 段落本身超长，硬切
                if (para.length() > chunkSize()) {
                    for (int i = 0; i < para.length(); i += chunkSize()) {
                        int end = Math.min(i + chunkSize(), para.length());
                        chunks.add(para.substring(i, end));
                    }
                } else {
                    current.append(para);
                }
            }
        }
        if (current.length() > 0) chunks.add(current.toString());
        return chunks;
    }
}