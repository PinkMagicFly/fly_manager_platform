package com.fly.platform.asset.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 无人机五态流转跃迁请求参数
 *
 * @author SoarCloud Team
 */
@Data
public class UavStateChangeDTO {

    /** 目标设备状态：0-闲置 1-任务中 2-维保中 3-故障 4-离线 */
    @NotNull(message = "目标状态不可为空")
    private Integer targetState;

    /** 流转操作原因/备注 */
    private String remark;
}
