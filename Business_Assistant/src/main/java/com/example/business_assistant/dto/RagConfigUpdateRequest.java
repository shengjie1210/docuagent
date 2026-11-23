package com.example.business_assistant.dto;

import lombok.Data;

/** 管理员更新 RAG 参数的请求体 */
@Data
public class RagConfigUpdateRequest {
    private Integer chunkSize;
    private Integer topK;
    private Double similarityThreshold;
    private Integer maxChunksPerDoc;
}
