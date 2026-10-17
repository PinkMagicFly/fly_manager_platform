package com.fly.platform.common.exception;

import com.fly.platform.common.api.ResultCode;
import lombok.Getter;

/**
 * 统一业务异常封装类
 *
 * @author SoarCloud Team
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }
}
