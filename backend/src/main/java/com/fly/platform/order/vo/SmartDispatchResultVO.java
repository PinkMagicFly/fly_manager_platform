package com.fly.platform.order.vo;

import lombok.Data;

import java.util.List;

/**
 * 智能调度推荐综合决策结果视图对象
 *
 * @author SoarCloud Team
 */
@Data
public class SmartDispatchResultVO {

    /** 目标运单ID */
    private Long orderId;

    /** 货物总重 */
    private Double goodsWeight;

    /** 直线运距 */
    private Double distanceKm;

    /** 首选最优推荐无人机 */
    private SmartDispatchCandidateVO recommendedUav;

    /** 全量候选无人机列表（按得分降序排序） */
    private List<SmartDispatchCandidateVO> candidateUavs;

    /** 执勤值班可用机长列表 */
    private List<PilotOptionVO> availablePilots;

    /**
     * 值班执飞机长简要选项
     */
    @Data
    public static class PilotOptionVO {
        private Long crewId;
        private Long accountId;
        private String realName;
        private String licenseNo;
        private Integer flyHours;
        private String dutyDesc;
    }
}
