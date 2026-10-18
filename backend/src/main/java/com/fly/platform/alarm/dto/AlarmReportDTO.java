package com.fly.platform.alarm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 机长在途现场故障上报入参 DTO
 *
 * @author SoarCloud Team
 */
@Data
public class AlarmReportDTO {

    /** 涉及运单ID（可选，若处于飞行中自动联动异常中断） */
    private Long orderId;

    /** 执飞无人机资产ID（可选） */
    private Long uavId;

    /** 故障类型名称（如：动力电机高温、指南针异常、传感器失联） */
    @NotBlank(message = "故障类型不能为空")
    private String faultType;

    /** 故障现场详细描述 */
    @NotBlank(message = "故障详细描述不能为空")
    private String description;
}
