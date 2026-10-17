package com.fly.platform.asset.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 移动作业端设备巡检打卡入参
 *
 * @author SoarCloud Team
 */
@Data
public class UavInspectDTO {

    /** 巡检目标无人机ID */
    @NotNull(message = "无人机标识不可为空")
    private Long uavId;

    /** 巡检所在场站ID */
    private Long siteId;

    /** 巡检核查清单总结文本 */
    private String itemsSummary;

    /** 巡检自检放行结论与备注 */
    private String inspectNote;
}
