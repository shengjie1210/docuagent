package com.example.business_assistant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProviderSaveRequest {
    private Long id;  // 新增时为空

    @NotBlank(message = "配置名称不能为空")
    private String name;

    @NotBlank(message = "服务商类型不能为空")
    private String provider;  // dashscope/openai/ollama

    @NotBlank(message = "API 地址不能为空")
    private String baseUrl;

    private String apiKey;

    @NotBlank(message = "对话模型名不能为空")
    private String chatModel;

    private String embedModel;
}
