package com.fly.platform.asset.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 维保工单验收合格入参
 *
 * @author SoarCloud Team
 */
@Data
public class MaintainAcceptDTO {

    /** 维修处理与配件更换记录说明 */
    @NotBlank(message = "维保检修内容不可为空")
    private String repairContent;

    /** 验收后评定的健康度分数（选填，为空则系统自算） */
    private Integer healthScore;
}
