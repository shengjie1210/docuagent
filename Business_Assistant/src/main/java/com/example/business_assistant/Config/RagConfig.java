package com.example.business_assistant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * RAG 可调参数配置 Bean
 * <p>
 * 关键：启动时从 application.yml 读初始值，但字段是可变的（非 final、非 @Value 只读快照），
 * 管理员通过 PUT /api/admin/rag-config 直接改这个 bean 的字段，实现热更新（无需重启）。
 * <p>
 * 注意：热更新只在内存中生效，应用重启后恢复为 application.yml 的值。
 * CHUNK_SIZE 只影响"新索引"的文档，已有向量需重新索引才会按新值切分。
 */
@Component
public class RagConfig {

    /** 每个 chunk 最大字符数 */
    private int chunkSize;

    /** 最终返回给模型/前端的 chunk 数 */
    private int topK;

    /** 相似度阈值，低于此分数直接丢弃 */
    private double similarityThreshold;

    /** 同一文档最多保留几个 chunk */
    private int maxChunksPerDoc;

    public RagConfig(
            @Value("${docuagent.chunk-size:800}") int chunkSize,
            @Value("${docuagent.top-k:4}") int topK,
            @Value("${docuagent.similarity-threshold:0.55}") double similarityThreshold,
            @Value("${docuagent.max-chunks-per-doc:2}") int maxChunksPerDoc) {
        this.chunkSize = chunkSize;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
        this.maxChunksPerDoc = maxChunksPerDoc;
    }

    public int getChunkSize() { return chunkSize; }
    public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }

    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }

    public double getSimilarityThreshold() { return similarityThreshold; }
    public void setSimilarityThreshold(double similarityThreshold) { this.similarityThreshold = similarityThreshold; }

    public int getMaxChunksPerDoc() { return maxChunksPerDoc; }
    public void setMaxChunksPerDoc(int maxChunksPerDoc) { this.maxChunksPerDoc = maxChunksPerDoc; }
}
