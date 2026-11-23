package com.example.business_assistant.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 * <p>
 * 负责生成和解析 Token：
 * - 登录成功后 generateToken() 生成 token 返回给前端
 * - 拦截器里 parseUserId() / parseIsAdmin() 解析 token 拿到身份信息
 * <p>
 * Token 结构：header.payload.signature
 *  - payload 里存了 userId（subject）、username、isAdmin
 *  - 用 HMAC-SHA256 签名，防止篡改
 * <p>
 * 注意：老 token 没有 isAdmin claim，parseIsAdmin 会返回 false，
 *      需要用户重新登录才能拿到带管理员标记的 token。
 */
@Component
public class JwtUtil {

    /** 签名密钥，从 application.yml 读取，生产环境必须改 */
    @Value("${docuagent.jwt-secret}")
    private String secret;

    /** 过期时间（毫秒），默认 2 小时 */
    @Value("${docuagent.jwt-expire}")
    private long expire;

    /** 签名 key 缓存，避免每次解析都重新生成 */
    private volatile SecretKey key;

    /**
     * 根据密钥生成签名 Key
     */
    private SecretKey getKey() {
        if (key == null) {
            synchronized (this) {
                if (key == null) {
                    key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
        return key;
    }

    /**
     * 生成 JWT Token（带管理员标记）
     * @param userId   用户 ID（存在 subject 里）
     * @param username 用户名（存在 claim 里）
     * @param isAdmin  是否管理员（存在 claim 里）
     */
    public String generateToken(Long userId, String username, boolean isAdmin) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expire);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("isAdmin", isAdmin)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getKey())
                .compact();
    }

    /**
     * 兼容旧调用：不带 isAdmin 的 token（普通用户）
     */
    public String generateToken(Long userId, String username) {
        return generateToken(userId, username, false);
    }

    /**
     * 解析 Token 的全部 claims
     * @throws BusinessException 401 如果 token 过期、签名错误、格式不对
     */
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            // 任何解析失败都统一返回"登录已过期"，不告诉前端具体原因（安全）
            throw BusinessException.unauthorized("登录已过期，请重新登录");
        }
    }

    /**
     * 解析 Token，拿到用户 ID
     */
    public Long parseUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    /**
     * 解析 Token 里的管理员标记。
     * 老 token 没有 isAdmin claim，返回 false（安全默认），需要重新登录。
     */
    public boolean parseIsAdmin(String token) {
        Object v = parseClaims(token).get("isAdmin");
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        return Boolean.parseBoolean(v.toString());
    }

    /**
     * 生成"管理后台解锁 Token"。
     * 与登录 Token 分开：任何登录用户输入共享管理员密码验证通过后，
     * 都会拿到一个独立的解锁 token，后续 /api/admin/** 请求用 X-Admin-Token 头带上。
     * 这样管理后台不再依赖账号的 isAdmin 角色，而是依赖是否持有有效解锁 token。
     */
    public String generateAdminToken() {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expire);
        return Jwts.builder()
                .subject("admin-unlock")
                .claim("adminUnlocked", true)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getKey())
                .compact();
    }

    /**
     * 校验解锁 Token 是否有效（签名正确、未过期、含 adminUnlocked=true）。
     * 任何异常都返回 false（安全默认）。
     */
    public boolean parseAdminUnlocked(String token) {
        try {
            Claims claims = parseClaims(token);
            Object v = claims.get("adminUnlocked");
            if (v instanceof Boolean b) return b;
            return v != null && Boolean.parseBoolean(v.toString());
        } catch (Exception e) {
            return false;
        }
    }
}
