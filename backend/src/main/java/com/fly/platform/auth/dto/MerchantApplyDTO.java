package com.fly.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 商户入驻申请提交 DTO
 *
 * @author SoarCloud Team
 */
@Data
public class MerchantApplyDTO {

    /** 企业全称 */
    @NotBlank(message = "企业全称不能为空")
    private String corpName;

    /** 统一社会信用代码/营业执照号 */
    @NotBlank(message = "统一社会信用代码不能为空")
    private String licenseNo;

    /** 企业联系人姓名 */
    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;

    /** 联系人手机号 */
    @NotBlank(message = "联系人手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号码格式不合法")
    private String contactPhone;

    /** 登录账号名称 (用于后续登录系统) */
    @NotBlank(message = "企业管理主账号登录名不能为空")
    private String loginName;

    /** 登录密码 */
    @NotBlank(message = "登录密码不能为空")
    private String password;

    /** 通用航空经营许可证/适航许可文号 */
    private String airworthinessCert;

    /** 资质证照上传文件路径 */
    private String certFilePath;
}
