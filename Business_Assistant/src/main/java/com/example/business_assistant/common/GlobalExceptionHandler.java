package com.example.business_assistant.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * <p>
 * 拦截所有 Controller 抛出的异常，统一转成 Result 格式返回前端。
 * - 自定义业务异常：按异常里的 code 返回
 * - 参数校验异常：提取第一条错误信息返回 400
 * - 其他未捕获异常：记日志，返回 500 友好提示，不暴露堆栈给前端
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常（最常见）
     */
    @ExceptionHandler(BusinessException.class)
    public Result handleBusiness(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理 @RequestBody + @Valid 校验失败
     * 比如 RegisterRequest 里 @NotBlank 没通过
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "参数错误";
        return Result.error(400, msg);
    }

    /**
     * 处理表单参数绑定校验失败
     */
    @ExceptionHandler(BindException.class)
    public Result handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "参数错误";
        return Result.error(400, msg);
    }

    /**
     * 兜底：所有没被上面处理的异常
     * 记 error 日志方便排障，前端只看到"系统繁忙"
     */
    @ExceptionHandler(Exception.class)
    public Result handleOther(Exception e) {
        log.error("未处理异常", e);
        return Result.error(500, "系统繁忙，请稍后重试");
    }
}
