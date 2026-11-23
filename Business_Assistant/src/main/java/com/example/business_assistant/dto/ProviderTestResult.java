package com.example.business_assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 供应商连通性测试结果
 */
@Data
@AllArgsConstructor
public class ProviderTestResult {
    /** 是否连通成功 */
    private boolean success;
    /** 响应耗时（毫秒） */
    private long latencyMs;
    /** 结果说明：HTTP 状态码 / 模型回复摘要 / 错误信息 */
    private String message;
}
