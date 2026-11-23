package com.example.business_assistant.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理后台解锁拦截器
 * <p>
 * 只拦截 /api/admin/** 路径。鉴权方式已从"账号 isAdmin 角色"改造为
 * "共享管理员密码解锁"：
 * - 任何登录用户在前端输入共享管理员密码，调 /api/auth/admin-unlock 验证
 * - 验证通过后拿到一个独立的解锁 token，前端存到 localStorage
 * - 后续 /api/admin/** 请求用 X-Admin-Token 头带上该 token
 * - 这里校验该 token 是否有效（签名正确、未过期、含 adminUnlocked=true）
 * - 无 token / token 无效 -> 403，提示先输入管理员密码
 * <p>
 * 注意：本拦截器不检查账号的 isAdmin 字段，所有用户鉴权完全等价，
 * 只取决于是否持有有效的解锁 token。
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    /** 前端传递解锁 token 的请求头名称 */
    public static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String adminToken = request.getHeader(ADMIN_TOKEN_HEADER);
        if (adminToken == null || adminToken.isBlank()) {
            throw BusinessException.forbidden("请先输入管理员密码解锁管理后台");
        }
        if (!jwtUtil.parseAdminUnlocked(adminToken)) {
            throw BusinessException.forbidden("管理后台解锁已过期，请重新输入管理员密码");
        }
        // 标记本次请求已通过管理后台鉴权，供 Controller/Service 使用
        UserContext.setIsAdmin(true);
        return true;
    }
}
