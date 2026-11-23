package com.example.business_assistant.bean;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文档实体类，对应数据库 document 表
 */
@Data
public class Document {

    /** 主键 ID */
    private Long id;

    /** 所属用户 ID */
    private Long userId;

    /** 原始文件名，如"退货政策.pdf" */
    private String filename;

    /** 文件类型：pdf / docx / md / txt */
    private String fileType;

    /** 存储相对路径，如 upload/xxx.pdf */
    private String filePath;

    /** 文件大小（字节） */
    private Long size;

    /** AI 生成的摘要 */
    private String summary;

    /** 标签，逗号分隔 */
    private String tags;

    /** 切分块数 */
    private Integer chunkCount;

    /** 状态：processing 处理中 / ready 就绪 / failed 失败 */
    private String status;

    /** 上传时间 */
    private LocalDateTime createdAt;
}
