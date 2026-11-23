package com.example.business_assistant.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 管理员视角的供应商配置 VO —— apiKey 已脱敏，绝不回显明文。
 */
@Data
public class ProviderVO {
    private Long id;
    private Long userId;
    private String username;
    private String provider;
    private String name;
    private String baseUrl;
    /** 脱敏后的 apiKey，如 sk-abc1******9x2z */
    private String apiKey;
    private String chatModel;
    private String embedModel;
    private Integer isDefault;
    private LocalDateTime updatedAt;
}
