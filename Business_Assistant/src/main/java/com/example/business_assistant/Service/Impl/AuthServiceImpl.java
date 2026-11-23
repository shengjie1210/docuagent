package com.example.business_assistant.service.impl;

import com.example.business_assistant.bean.User;
import com.example.business_assistant.common.BusinessException;
import com.example.business_assistant.common.JwtUtil;
import com.example.business_assistant.dto.LoginRequest;
import com.example.business_assistant.dto.LoginVO;
import com.example.business_assistant.dto.RegisterRequest;
import com.example.business_assistant.mapper.UserMapper;
import com.example.business_assistant.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证 Service 实现类
 * <p>
 * 职责：
 *  1. 注册：查重 -> BCrypt 加密密码 -> 存库 -> 发 token
 *  2. 登录：查用户 -> BCrypt 比对密码 -> 发 token
 */
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 密码加密器（BCrypt 自带盐，不需要自己存盐）
     */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 注册
     */
    @Override
    public LoginVO register(RegisterRequest req) {
        // 1. 检查用户名是否已存在
        User existing = userMapper.selectByUsername(req.getUsername());
        if (existing != null) {
            throw BusinessException.conflict("用户名已被注册");
        }

        // 2. 加密密码并存库
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        userMapper.insert(user);

        // 3. 直接返回 token（注册后自动登录，新注册用户不是管理员）
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), false);
        return new LoginVO(token, user.getUsername(), false);
    }

    /**
     * 登录
     */
    @Override
    public LoginVO login(LoginRequest req) {
        // 1. 按用户名查
        User user = userMapper.selectByUsername(req.getUsername());
        if (user == null) {
            throw BusinessException.unauthorized("用户名或密码错误");
        }

        // 2. BCrypt 比对密码（不直接 equals，因为加密带盐）
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw BusinessException.unauthorized("用户名或密码错误");
        }

        // 3. 发 token（带上管理员标记，老 token 无此 claim 需重新登录）
        boolean isAdmin = user.getIsAdmin() != null && user.getIsAdmin() == 1;
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), isAdmin);
        return new LoginVO(token, user.getUsername(), isAdmin);
    }
}
