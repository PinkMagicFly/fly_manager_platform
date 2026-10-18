package com.fly.platform.airspace.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能避障航线自动规划与校验结果视图对象
 *
 * @author SoarCloud Team
 */
@Data
public class RoutePlanResultVO {

    /** 规划后的航线经纬度折线坐标数组 [[lng, lat], ...] */
    private List<List<Double>> routeLine = new ArrayList<>();

    /** 冲突违规详情说明列表 */
    private List<String> violations = new ArrayList<>();

    /** 是否合规放行 */
    private Boolean pass = true;

    /** 审核或建议提示信息 */
    private String message;

    /** 建议修正巡航高度（米） */
    private Integer suggestedAlt;
}
