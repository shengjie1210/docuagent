package com.example.business_assistant.controller;

import com.example.business_assistant.common.Result;
import com.example.business_assistant.dto.LoginRequest;
import com.example.business_assistant.dto.LoginVO;
import com.example.business_assistant.dto.RegisterRequest;
import com.example.business_assistant.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证 Controller
 * <p>
 * 提供注册、登录两个接口，不需要 token（在 WebConfig 里放行）。
 * 接口文档：启动后访问 http://localhost:8080/doc.html
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证接口")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 注册
     * POST /api/auth/register
     * body: { username, password }
     */
    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result<LoginVO> register(@RequestBody @Valid RegisterRequest req) {
        return Result.success(authService.register(req));
    }

    /**
     * 登录
     * POST /api/auth/login
     * body: { username, password }
     */
    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginRequest req) {
        return Result.success(authService.login(req));
    }
}
