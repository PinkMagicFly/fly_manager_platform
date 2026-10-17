package com.fly.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 客户实名认证提交与审核 DTO
 *
 * @author SoarCloud Team
 */
@Data
public class CustomerVerifyDTO {

    /** 客户档案ID (审核时必填) */
    private Long customerId;

    /** 真实姓名/企业名称 */
    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    /** 身份证号/信用代码 */
    @NotBlank(message = "身份证号不能为空")
    private String idCard;

    /** 授信额度（元） */
    @NotNull(message = "信用额度不能为空")
    private BigDecimal creditLimit;

    /** 实名状态：1-已认证 2-认证失败 */
    private Integer authState;
}
