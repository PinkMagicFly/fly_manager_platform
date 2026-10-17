package com.fly.platform.asset.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 机队资产效能分析大盘 VO
 *
 * @author SoarCloud Team
 */
@Data
public class AssetEfficiencyVO {

    /** 机队总规模数 */
    private Integer totalUavs;

    /** 任务中飞行架数 */
    private Integer onTaskUavs;

    /** 平均设备稼动率百分比 (e.g. 75.5) */
    private Double avgUtilizationRate;

    /** 机队累计安全飞行总时长 (小时) */
    private Integer totalFlyHours;

    /** 设备五态数量分布 (饼图用，如 [{"name": "闲置", "value": 2}, ...]) */
    private List<Map<String, Object>> stateDistribution;

    /** 7日设备稼动率趋势折线数据 */
    private TrendDataVO utilizationTrend;

    /** 累计飞行时长 Top 5 排行 (柱状图) */
    private List<Map<String, Object>> topFlyHours;

    /** 健康综合评分梯度分布 (优秀>90, 良好80-90, 预警<80) */
    private List<Map<String, Object>> healthDistribution;

    @Data
    public static class TrendDataVO {
        private List<String> dates;
        private List<Double> rates;
    }
}
