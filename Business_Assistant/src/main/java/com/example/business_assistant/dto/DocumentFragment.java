package com.example.business_assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档检索片段（RAG 检索结果）。
 * <p>
 * 实战项目改进点：
 *  1. 透出相似度分数 score，前端可按分数排序/过滤，避免把弱相关 chunk 当"引用来源"展示；
 *  2. 透出 chunkIndex，方便调试时定位是文档的哪一段；
 *  3. 增加 contentPreview，前端列表展示摘要时不必每次加载完整 content。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentFragment {

    /** 片段文本内容（完整） */
    private String content;

    /** 来源文档 ID */
    private Long docId;

    /** 来源文件名 */
    private String filename;

    /** 相似度分数，0~1，越高越相关；由向量检索器给出 */
    private Double score;

    /** 在文档中的 chunk 序号，从 0 开始 */
    private Integer chunkIndex;

    /** 前端列表用的预览文本（前 80 字），后端直接生成，避免前端再截 */
    private String contentPreview;

    public DocumentFragment(String content, Long docId, String filename) {
        this.content = content;
        this.docId = docId;
        this.filename = filename;
    }

    /** 便捷工厂：自动生成 preview */
    public static DocumentFragment of(String content, Long docId, String filename, Double score, Integer chunkIndex) {
        DocumentFragment f = new DocumentFragment();
        f.content = content;
        f.docId = docId;
        f.filename = filename;
        f.score = score;
        f.chunkIndex = chunkIndex;
        String trimmed = content == null ? "" : content.replaceAll("\\s+", " ").trim();
        f.contentPreview = trimmed.length() > 80 ? trimmed.substring(0, 80) + "…" : trimmed;
        return f;
    }
}