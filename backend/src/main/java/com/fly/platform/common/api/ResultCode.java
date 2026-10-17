package com.fly.platform.common.api;

import lombok.Getter;

/**
 * 统一接口返回业务响应码枚举
 *
 * @author SoarCloud Team
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误或非法"),
    UNAUTHORIZED(401, "身份凭证无效或会话已过期，请重新登录"),
    FORBIDDEN(403, "无权执行该操作（权限不足）"),
    NOT_FOUND(404, "请求的资源不存在"),
    BUSINESS_ERROR(1001, "业务逻辑异常"),
    ACCOUNT_NOT_EXIST(1002, "账号不存在"),
    PASSWORD_ERROR(1003, "密码错误，请重新输入"),
    ACCOUNT_FROZEN(1004, "该账号已被冻结或处于待审核状态，请联系管理员"),
    ACCOUNT_EXISTS(1005, "账号名称或手机号已存在"),
    VERIFY_CODE_ERROR(1006, "手机短信验证码错误或已失效"),
    SYSTEM_ERROR(500, "系统内部执行异常，请稍后重试");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
