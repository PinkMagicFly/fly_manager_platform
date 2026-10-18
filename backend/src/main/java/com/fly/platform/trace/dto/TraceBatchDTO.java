package com.fly.platform.trace.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量飞行轨迹上报入参 DTO
 *
 * @author SoarCloud Team
 */
@Data
public class TraceBatchDTO {

    /** 关联飞行申报单ID */
    @NotNull(message = "飞行申报任务ID不能为空")
    private Long applyId;

    /** 批次防重唯一标识 UUID (弱网重传防重) */
    private String batchUuid;

    /** 采集的轨迹点列表 */
    @NotEmpty(message = "航迹点列表不能为空")
    @Valid
    private List<TracePointDTO> points;
}
