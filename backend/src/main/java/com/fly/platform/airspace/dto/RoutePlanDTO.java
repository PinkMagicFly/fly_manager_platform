package com.fly.platform.airspace.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 智能避障航线自动规划传输对象
 *
 * @author SoarCloud Team
 */
@Data
public class RoutePlanDTO {

    /** 起点经度 */
    @NotNull(message = "起点经度不能为空")
    private Double startLng;

    /** 起点纬度 */
    @NotNull(message = "起点纬度不能为空")
    private Double startLat;

    /** 终点经度 */
    @NotNull(message = "终点经度不能为空")
    private Double endLng;

    /** 终点纬度 */
    @NotNull(message = "终点纬度不能为空")
    private Double endLat;

    /** 计划巡航高度（米，默认100米） */
    private Integer altPlanned = 100;
}
