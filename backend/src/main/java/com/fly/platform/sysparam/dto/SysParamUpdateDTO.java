package com.fly.platform.sysparam.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 系统参数更新入参 DTO
 *
 * @author SoarCloud Team
 */
@Data
public class SysParamUpdateDTO {

    /** 参数Key */
    @NotBlank(message = "参数Key不能为空")
    private String paramKey;

    /** 参数生效值 */
    @NotBlank(message = "参数值不能为空")
    private String paramValue;

    /** 备注说明（可选） */
    private String remark;
}
