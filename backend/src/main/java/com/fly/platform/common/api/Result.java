package com.fly.platform.common.api;

import lombok.Data;
import java.io.Serializable;

/**
 * 平台统一全局出参响应包装器
 *
 * @param <T> 数据泛型
 * @author SoarCloud Team
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务响应码 (200成功，其他为异常) */
    private int code;

    /** 友好中文提示消息 */
    private String message;

    /** 实际业务承载数据 */
    private T data;

    /** 接口响应时间戳 (毫秒) */
    private long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(ResultCode.BUSINESS_ERROR.getCode(), message, null);
    }
}
