package com.fly.platform.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 智能调度无人机候选评估视图对象
 *
 * @author SoarCloud Team
 */
@Data
public class SmartDispatchCandidateVO {

    /** 无人机ID */
    private Long uavId;

    /** 设备编号 */
    private String uavSn;

    /** 机型名称 */
    private String model;

    /** 最大载重（千克） */
    private BigDecimal maxLoad;

    /** 满电最大航程（公里） */
    private BigDecimal maxRangeKm;

    /** 实时剩余电量百分比 (%) */
    private Integer batteryPct;

    /** 设备健康评分 */
    private Integer healthScore;

    /** 常驻起降场站ID */
    private Long homeSiteId;

    /** 常驻起降场站名称 */
    private String homeSiteName;

    /** 调度综合匹配得分 (0-100) */
    private Integer score;

    /** 匹配与推荐推荐理由 */
    private String matchReason;

    /** 是否作为排他性第一推荐 */
    private Boolean isRecommended = false;
}
