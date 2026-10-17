package com.fly.platform.airspace.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 飞行违规案件全景详情与偏航对比视图对象
 *
 * @author SoarCloud Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ViolationDetailVO extends ViolationCaseVO {

    /** 申报批准航线坐标点 [[lng, lat], ...] */
    private List<List<Double>> plannedRoute;

    /** 实际飞行偏航雷达轨迹坐标点 [[lng, lat], ...] */
    private List<List<Double>> actualRoute;

    /** 最大偏离距离（米） */
    private Integer maxDeviationMeters;

    /** 实际最高巡航真高（米） */
    private Integer actualMaxAltitude;

    /** 行政处罚决定书正文存证 */
    private String punishDocText;
}
