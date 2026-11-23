package com.example.business_assistant.config;

import com.example.business_assistant.common.AdminAuthInterceptor;
import com.example.business_assistant.common.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：注册拦截器
 * <p>
 * 对 /api/** 的所有接口做 JWT 校验，
 * 但放行登录、注册、API 文档（Knife4j）相关路径。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Autowired
    private AdminAuthInterceptor adminAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 先做登录校验（解析 userId / isAdmin 存入 UserContext）
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",          // 登录、注册不需要 token
                        "/doc.html",             // Knife4j 文档页
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**"
                );
        // 2. 再做管理后台解锁校验（拦 /api/admin/**，但放行 unlock 本身和 auth 登录接口）
        //    unlock 仍需登录（走 JwtInterceptor），但不该被自己拦住，否则无法解锁
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/unlock");
    }
}
