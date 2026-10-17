package com.fly.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 商户创建企业子账号 DTO
 *
 * @author SoarCloud Team
 */
@Data
public class SubAccountCreateDTO {

    /** 子账号登录名 */
    @NotBlank(message = "子账号登录名不能为空")
    private String loginName;

    /** 初始密码 */
    @NotBlank(message = "初始密码不能为空")
    private String password;

    /** 关联手机号 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号码格式不合法")
    private String mobile;

    /** 电子邮箱 */
    private String email;

    /** 分配的角色编码或角色ID列表 */
    @NotEmpty(message = "请至少分配一个岗位角色")
    private List<Long> roleIds;
}
