package com.example.business_assistant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 管理后台解锁请求 */
@Data
public class AdminUnlockRequest {
    /** 共享管理员密码 */
    @NotBlank(message = "请输入管理员密码")
    private String password;
}
