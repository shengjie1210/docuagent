package com.example.business_assistant.controller;

import com.example.business_assistant.common.BusinessException;
import com.example.business_assistant.common.JwtUtil;
import com.example.business_assistant.common.Result;
import com.example.business_assistant.dto.AdminUnlockRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台解锁接口
 * <p>
 * 任何登录用户输入共享管理员密码，验证通过后拿到一个独立的解锁 token。
 * 后续 /api/admin/** 请求用 X-Admin-Token 头带上该 token 即可访问，
 * 不再依赖账号的 isAdmin 角色。
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "管理后台鉴权")
public class AdminAuthController {

    @Autowired
    private JwtUtil jwtUtil;

    /** 共享管理员密码，从配置读取，默认 123456 */
    @Value("${docuagent.admin-password:123456}")
    private String adminPassword;

    /**
     * 解锁管理后台
     * POST /api/admin/unlock
     * body: { password }
     * 成功返回解锁 token（前端存 localStorage，后续放 X-Admin-Token 头）
     */
    @Operation(summary = "输入管理员密码解锁")
    @PostMapping("/unlock")
    public Result<String> unlock(@RequestBody @Valid AdminUnlockRequest req) {
        if (!adminPassword.equals(req.getPassword())) {
            throw BusinessException.forbidden("管理员密码错误");
        }
        return Result.success(jwtUtil.generateAdminToken());
    }
}
