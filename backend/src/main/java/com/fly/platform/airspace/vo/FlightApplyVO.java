package com.fly.platform.airspace.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 飞行计划申报与合规审批列表视图对象
 *
 * @author SoarCloud Team
 */
@Data
public class FlightApplyVO {

    /** 申报唯一标识 */
    private Long applyId;

    /** 申报编号 */
    private String applySn;

    /** 关联订单ID */
    private Long orderId;

    /** 申报商户ID */
    private Long merchantId;

    /** 申报商户名称 */
    private String merchantName;

    /** 执飞无人机资产ID */
    private Long uavId;

    /** 执飞无人机编号 */
    private String uavSn;

    /** 无人机机型 */
    private String uavModel;

    /** 执飞机长ID */
    private Long pilotId;

    /** 执飞机长姓名 */
    private String pilotName;

    /** 规划航线经纬度坐标序列 [[lng, lat], ...] */
    private List<List<Double>> routePoints;

    /** 申报巡航高度（米） */
    private Integer altPlanned;

    /** 计划起飞时间 */
    private LocalDateTime startAt;

    /** 计划降落时间 */
    private LocalDateTime endAt;

    /** 围栏校验状态：0-未校验 1-校验通过 2-冲突告警 */
    private Integer checkState;

    /** 围栏校验状态描述 */
    private String checkStateDesc;

    /** 审批四态：0-待审批 1-已通过 2-已驳回 3-已撤销 */
    private Integer approveState;

    /** 审批状态中文描述 */
    private String approveStateDesc;

    /** 审批人ID */
    private Long approverId;

    /** 审批人姓名 */
    private String approverName;

    /** 审批批复意见或限高建议 */
    private String approveNote;

    /** 审批时间 */
    private LocalDateTime approveAt;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
