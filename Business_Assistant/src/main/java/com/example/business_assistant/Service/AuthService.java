package com.example.business_assistant.service;

import com.example.business_assistant.dto.LoginRequest;
import com.example.business_assistant.dto.LoginVO;
import com.example.business_assistant.dto.RegisterRequest;

/**
 * 认证 Service 接口
 * <p>
 * 定义注册、登录的业务契约，具体实现在 impl 包。
 */
public interface AuthService {

    /**
     * 注册新用户
     * @throws com.example.business_assistant.common.BusinessException 409 用户名已存在
     */
    LoginVO register(RegisterRequest req);

    /**
     * 登录，校验密码后返回 token
     * @throws com.example.business_assistant.common.BusinessException 401 用户名或密码错误
     */
    LoginVO login(LoginRequest req);
}
