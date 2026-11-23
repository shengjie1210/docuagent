package com.example.business_assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** RAG 配置 + 向量库统计信息 */
@Data
@AllArgsConstructor
public class RagConfigVO {
    private int chunkSize;
    private int topK;
    private double similarityThreshold;
    private int maxChunksPerDoc;

    /** vectors.json 文件大小（字节） */
    private long vectorFileBytes;
    /** chunk 总数（文档表 chunk_count 之和） */
    private long totalChunks;
    /** 文档总数 */
    private long documentCount;
}
