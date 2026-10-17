package com.fly.platform.asset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 外场/机长现场设备故障上报入参
 *
 * @author SoarCloud Team
 */
@Data
public class FaultReportDTO {

    /** 关联无人机ID */
    @NotNull(message = "无人机标识不可为空")
    private Long uavId;

    /** 故障现象描述 */
    @NotBlank(message = "故障现象描述不可为空")
    private String faultDesc;
}
