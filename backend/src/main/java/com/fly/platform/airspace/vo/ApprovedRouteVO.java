package com.fly.platform.airspace.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 已批准飞行航线视图 VO
 *
 * @author SoarCloud Team
 */
@Data
public class ApprovedRouteVO {

    /** 飞行任务ID */
    private Long applyId;

    /** 申报任务编号 */
    private String applySn;

    /** 关联订单ID */
    private Long orderId;

    /** 执飞机号 */
    private String uavSn;

    /** 执飞机长姓名 */
    private String pilotName;

    /** 巡航规划高度（米） */
    private Integer altPlanned;

    /** 计划起飞时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startAt;

    /** 计划降落时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endAt;

    /** 航线经纬度坐标折线序列 [[lng, lat], ...] */
    private List<List<Double>> routePoints = new ArrayList<>();

    /** 任务审批状态码 */
    private Integer approveState;

    /** 任务审批状态中文标签 */
    private String approveStateDesc;
}
