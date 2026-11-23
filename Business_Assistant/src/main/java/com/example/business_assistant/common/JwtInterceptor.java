package com.example.business_assistant.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 登录拦截器
 * <p>
 * 每个请求进来先经过这里：
 *  1. 从 Header 取 Authorization: Bearer xxx
 *  2. 解析 token 拿到 userId
 *  3. 把 userId 存到 UserContext（ThreadLocal）
 *  4. 放行，Controller/Service 直接从 UserContext 取当前用户
 * <p>
 * 登录/注册接口在 WebConfig 里配置了放行，不经过这里。
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 取 Authorization 头
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw BusinessException.unauthorized("未登录");
        }
        // 去掉 "Bearer " 前缀，解析 token
        String token = auth.substring(7);
        Long userId = jwtUtil.parseUserId(token);
        boolean isAdmin = jwtUtil.parseIsAdmin(token);
        // 存到 ThreadLocal，后续业务代码用
        UserContext.setUserId(userId);
        UserContext.setIsAdmin(isAdmin);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束必须清掉 ThreadLocal，否则 Tomcat 线程池复用线程会串号
        UserContext.clear();
    }
}
