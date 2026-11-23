package com.example.business_assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量上传结果。
 * <p>
 * 实战项目设计：
 *  - 单个文件失败不中断整批（fail-tolerant）；
 *  - 每个文件单独返回成功/失败状态与原因，前端可精确展示；
 *  - 汇总字段方便前端弹"成功 N 个，失败 M 个"的提示。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchUploadResult {

    /** 每个文件的处理结果 */
    private List<Item> items;

    /** 成功数量（冗余字段，前端不用循环计数） */
    private int successCount;

    /** 失败数量 */
    private int failureCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        /** 原始文件名 */
        private String filename;
        /** 是否上传成功 */
        private boolean success;
        /** 成功时返回的文档 VO（可能为 null，索引失败时仍算 upload 成功） */
        private DocumentVO document;
        /** 失败原因（成功时为 null） */
        private String errorMsg;
    }
}