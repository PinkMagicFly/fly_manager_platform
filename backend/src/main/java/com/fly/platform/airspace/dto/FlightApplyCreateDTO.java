package com.fly.platform.airspace.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 飞行计划在线申报填报传输对象
 *
 * @author SoarCloud Team
 */
@Data
public class FlightApplyCreateDTO {

    /** 关联履约订单ID（独立填报时可为空） */
    private Long orderId;

    /** 申报商户企业ID（未提供时根据当前登录上下文自动填充） */
    private Long merchantId;

    /** 执飞无人机资产ID */
    @NotNull(message = "执飞无人机资产不能为空")
    private Long uavId;

    /** 执飞机长责任人ID */
    @NotNull(message = "执飞机长责任人不能为空")
    private Long pilotId;

    /** 规划航线经纬度折线坐标序列 [[lng, lat], ...] */
    @NotEmpty(message = "规划航线折线坐标序列不能为空")
    private List<List<Double>> routePoints;

    /** 申报巡航高度（米） */
    @NotNull(message = "申报巡航高度不能为空")
    private Integer altPlanned;

    /** 计划起飞时间 */
    @NotNull(message = "计划起飞时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startAt;

    /** 计划降落时间 */
    @NotNull(message = "计划降落时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endAt;
}

