package com.fly.platform.order.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单智能调度与指派派单传输对象
 *
 * @author SoarCloud Team
 */
@Data
public class OrderDispatchDTO {

    /** 目标调度运单ID */
    @NotNull(message = "运单ID不能为空")
    private Long orderId;

    /** 指派执飞无人机资产ID */
    @NotNull(message = "执飞无人机资产不能为空")
    private Long uavId;

    /** 指派执飞机长ID */
    @NotNull(message = "执飞机长责任人不能为空")
    private Long pilotId;

    /** 规划巡航高度（米，默认100米） */
    private Integer altPlanned = 100;

    /** 预计起飞时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startAt;

    /** 预计降落时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endAt;

    /** 规划航线折线坐标序列 WKT (LINESTRING(lng lat, lng lat, ...)) */
    @jakarta.validation.constraints.NotBlank(message = "规划航线折线坐标序列不能为空")
    private String routeLine;
}
