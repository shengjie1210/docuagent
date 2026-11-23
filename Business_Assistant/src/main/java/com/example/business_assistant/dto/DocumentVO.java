package com.example.business_assistant.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文档信息返回 VO
 */
@Data
public class DocumentVO {

    private Long id;
    private String filename;
    private String fileType;
    private Long size;
    private String summary;
    private String tags;
    private Integer chunkCount;
    private String status;
    private LocalDateTime createdAt;
}
