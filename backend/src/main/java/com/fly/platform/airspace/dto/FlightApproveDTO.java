package com.fly.platform.airspace.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 飞行计划空管在线审批操作传输对象
 *
 * @author SoarCloud Team
 */
@Data
public class FlightApproveDTO {

    /** 申报单ID */
    @NotNull(message = "申报单ID不能为空")
    private Long applyId;

    /** 审批决策状态：1-已通过 2-已驳回 */
    @NotNull(message = "审批决策状态不能为空")
    private Integer approveState;

    /** 审批批复意见或限高建议说明 */
    private String approveNote;

    /** 限高批准巡航高度（米，可选） */
    private Integer suggestedAlt;
}
