package com.example.business_assistant.bean;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体类，对应数据库 user 表
 */
@Data
public class User {

    /** 主键 ID，自增 */
    private Long id;

    /** 用户名，唯一 */
    private String username;

    /** 密码（BCrypt 加密后的密文，不可逆） */
    private String password;

    /** 是否管理员：1=管理员，0=普通用户 */
    private Integer isAdmin;

    /** 注册时间 */
    private LocalDateTime createdAt;
}
