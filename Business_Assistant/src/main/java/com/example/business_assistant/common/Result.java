package com.example.business_assistant.common;

import lombok.Data;
import java.util.UUID;

/**
 * 统一响应结果包装类
 * <p>
 * 所有 Controller 接口都返回这个类，保证前端拿到的 JSON 结构一致：
 * { code, message, data, traceId }
 * <p>
 * code 含义：
 *  - 200  成功
 *  - 400  参数错误
 *  - 401  未登录 / Token 失效
 *  - 403  无权限
 *  - 404  资源不存在
 *  - 409  冲突（如用户名已存在）
 *  - 500  服务器内部错误
 *  - 503  第三方服务不可用（如大模型 API 挂了）
 */
@Data
public class Result<T> {

    /** 业务状态码，前端根据这个判断弹什么提示 */
    private int code;

    /** 提示信息，可直接展示给用户 */
    private String message;

    /** 业务数据，失败时为 null */
    private T data;

    /** 链路追踪 ID，出问题时拿这个去日志里搜 */
    private String traceId;

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        r.setTraceId(shortTraceId());
        return r;
    }

    /**
     * 成功响应（不带数据，用于删除等操作）
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 失败响应
     * @param code    业务错误码
     * @param message 错误提示
     */
    public static <T> Result<T> error(int code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        r.setTraceId(shortTraceId());
        return r;
    }

    /**
     * 生成 8 位短 traceId
     */
    private static String shortTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
