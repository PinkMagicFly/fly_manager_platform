package com.fly.platform.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.fly.platform.common.api.Result;
import com.fly.platform.common.api.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局统一异常捕获处理器
 *
 * @author SoarCloud Team
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务已知异常处理
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务规则校验拦截: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数强校验异常 (JSR-303) 与表单绑定异常统一拦截
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidationException(Exception e) {
        FieldError fieldError = null;
        if (e instanceof MethodArgumentNotValidException me) {
            fieldError = me.getBindingResult().getFieldError();
        } else if (e instanceof BindException be) {
            fieldError = be.getFieldError();
        }
        String message = fieldError != null ? fieldError.getDefaultMessage() : "请求参数格式校验未通过";
        log.warn("参数强校验拦截: {}", message);
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * Sa-Token 未登录异常拦截
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e) {
        log.warn("用户凭证失效或未登录: type={}, message={}", e.getType(), e.getMessage());
        return Result.fail(ResultCode.UNAUTHORIZED.getCode(), "请先登录系统再进行操作");
    }

    /**
     * Sa-Token 角色不足拦截
     */
    @ExceptionHandler(NotRoleException.class)
    public Result<Void> handleNotRoleException(NotRoleException e) {
        log.warn("用户角色越权访问: role={}", e.getRole());
        return Result.fail(ResultCode.FORBIDDEN.getCode(), "当前角色权限不足，禁止访问");
    }

    /**
     * Sa-Token 按钮权限不足拦截
     */
    @ExceptionHandler(NotPermissionException.class)
    public Result<Void> handleNotPermissionException(NotPermissionException e) {
        log.warn("用户按钮权限越权拦截: permission={}", e.getPermission());
        return Result.fail(ResultCode.FORBIDDEN.getCode(), "无权调用此业务操作权限");
    }

    /**
     * 系统未知运行异常兜底
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknownException(Exception e) {
        log.error("系统发生未知未捕获异常", e);
        return Result.fail(ResultCode.SYSTEM_ERROR.getCode(), "系统繁忙，请稍后重试");
    }
}
