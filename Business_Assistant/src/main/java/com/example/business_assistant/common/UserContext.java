package com.example.business_assistant.common;

/**
 * 用户上下文（ThreadLocal）
 * <p>
 * JWT 拦截器解析出 userId / isAdmin 后存到 ThreadLocal 里，
 * 后续 Service 层随时能拿到当前登录用户是谁、是不是管理员。
 * <p>
 * 注意：请求结束后必须 clear()，否则线程池复用线程会串号。
 */
public class UserContext {

    /** 每个线程独立存一份 userId */
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    /** 每个线程独立存一份管理员标记 */
    private static final ThreadLocal<Boolean> ADMIN = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static void setIsAdmin(boolean isAdmin) {
        ADMIN.set(isAdmin);
    }

    /** 当前用户是否管理员 */
    public static boolean isAdmin() {
        return Boolean.TRUE.equals(ADMIN.get());
    }

    public static void clear() {
        USER_ID.remove();
        ADMIN.remove();
    }
}
