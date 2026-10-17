package com.fly.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户多方式登录请求 DTO
 *
 * @author SoarCloud Team
 */
@Data
public class LoginRequestDTO {

    /** 登录用户名或手机号 */
    @NotBlank(message = "登录账号或手机号不能为空")
    private String username;

    /** 登录明文密码 (密码模式必传) */
    private String password;

    /** 手机短信验证码 (短信模式必传) */
    private String smsCode;

    /** 登录认证方式：password-账号密码登录 sms-短信验证码登录 */
    private String loginType = "password";

    /** 登录目标角色类别选择 (可选，辅助校验) */
    private Integer accountKind;
}
