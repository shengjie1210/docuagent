package com.example.business_assistant.bean;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI 服务商配置实体
 */
@Data
public class AiProviderConfig {
    private Long id;
    private Long userId;
    private String provider;
    private String name;
    private String baseUrl;
    private String apiKey;
    private String chatModel;
    private String embedModel;
    private Integer isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
