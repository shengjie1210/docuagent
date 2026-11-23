package com.example.business_assistant.common;

import lombok.Getter;

/**
 * 自定义业务异常
 * <p>
 * Service 层遇到业务错误时直接抛出这个异常，
 * 由 GlobalExceptionHandler 统一捕获并转成 Result 返回，
 * 不用每个 Controller 都写 try-catch。
 * <p>
 * 用法：
 * <pre>
 *   throw BusinessException.notFound("文档不存在");
 *   throw BusinessException.conflict("用户名已被注册");
 * </pre>
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务错误码 */
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /** 参数错误（400） */
    public static BusinessException badRequest(String msg) {
        return new BusinessException(400, msg);
    }

    /** 未登录 / Token 失效（401） */
    public static BusinessException unauthorized(String msg) {
        return new BusinessException(401, msg);
    }

    /** 无权限访问该资源（403） */
    public static BusinessException forbidden(String msg) {
        return new BusinessException(403, msg);
    }

    /** 资源不存在（404） */
    public static BusinessException notFound(String msg) {
        return new BusinessException(404, msg);
    }

    /** 冲突，如用户名已存在（409） */
    public static BusinessException conflict(String msg) {
        return new BusinessException(409, msg);
    }
}
