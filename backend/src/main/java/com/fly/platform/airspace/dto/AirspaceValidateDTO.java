package com.fly.platform.airspace.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 航线合规碰撞即时检测请求传输对象
 *
 * @author SoarCloud Team
 */
@Data
public class AirspaceValidateDTO {

    /** 规划航线经纬度坐标序列 [[lng, lat], [lng, lat], ...] */
    @NotEmpty(message = "规划航线经纬度坐标序列不能为空")
    private List<List<Double>> routePoints;

    /** 计划飞行巡航高度（米） */
    @NotNull(message = "计划飞行高度不能为空")
    private Integer altPlanned;

    /** 计划起飞时间 */
    private LocalDateTime startAt;

    /** 计划降落时间 */
    private LocalDateTime endAt;
}
