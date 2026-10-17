package com.fly.platform.airspace.vo;

import lombok.Data;

import java.util.List;

/**
 * 航线电子围栏合规校验结果与避障纠偏建议视图对象
 *
 * @author SoarCloud Team
 */
@Data
public class AirspaceValidateResultVO {

    /** 是否合规放行：true-校验通过放行 false-拦截告警 */
    private Boolean pass;

    /** 冲突类别：NONE-无冲突 FORBIDDEN_ZONE-穿越禁飞区 ALTITUDE_EXCEEDED-超高 TEMPORARY_CONTROL-临时管制 */
    private String conflictType;

    /** 校验结果中文提示详情 */
    private String message;

    /** 冲突空域区块ID */
    private Long conflictBlockId;

    /** 冲突空域区块名称 */
    private String conflictBlockName;

    /** 智能避障绕行纠偏推荐航线序列 [[lng, lat], ...] */
    private List<List<Double>> suggestedRoute;

    /** 建议修正巡航高度（米） */
    private Integer suggestedAlt;
}
