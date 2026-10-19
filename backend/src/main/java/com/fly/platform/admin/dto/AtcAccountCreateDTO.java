package com.fly.platform.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 管理员新建空管监察员账号传输对象
 *
 * @author SoarCloud Team
 */
@Data
public class AtcAccountCreateDTO {

    /** 登录用户名 */
    @NotBlank(message = "登录账号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "账号需为3-20位字母、数字或下划线")
    private String loginName;

    /** 联络手机号 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号码格式不正确")
    private String mobile;

    /** 电子邮箱 */
    private String email;

    /** 所属空管单位或姓名 */
    private String orgName;

    /** 初始密码 (默认 123456) */
    private String password = "123456";
}
