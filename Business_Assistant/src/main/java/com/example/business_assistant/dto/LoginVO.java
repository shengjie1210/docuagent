package com.example.business_assistant.dto;

import lombok.Data;

/**
 * 登录/注册成功后返回给前端的 VO
 * <p>
 * 不返回密码，只给 token、用户名和是否管理员。
 */
@Data
public class LoginVO {

    /** JWT Token，前端之后每个请求带在 Header 里 */
    private String token;

    /** 用户名，用于前端展示 */
    private String username;

    /** 是否管理员，前端据此显示"管理后台"入口 */
    private Boolean isAdmin;

    public LoginVO(String token, String username, Boolean isAdmin) {
        this.token = token;
        this.username = username;
        this.isAdmin = isAdmin;
    }
}
