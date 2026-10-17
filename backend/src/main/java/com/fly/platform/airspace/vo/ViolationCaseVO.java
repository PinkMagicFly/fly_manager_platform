package com.fly.platform.airspace.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 飞行违规案件列表展示视图对象
 *
 * @author SoarCloud Team
 */
@Data
public class ViolationCaseVO {

    /** 违规案件ID */
    private Long caseId;

    /** 违规案件流水号 */
    private String caseSn;

    /** 关联飞行计划申报ID */
    private Long applyId;

    /** 申报单编号 */
    private String applySn;

    /** 执飞无人机编号 */
    private String uavSn;

    /** 责任机长姓名 */
    private String pilotName;

    /** 责任所属商户 */
    private String merchantName;

    /** 违规行为类别：1-航线严重偏航 2-非法入侵禁飞区 3-超越法定限高 4-超时占用空域 */
    private Integer violateKind;

    /** 违规行为类别描述 */
    private String violateKindDesc;

    /** 行政处罚分级：1-警告留痕 2-企业罚款 3-停飞整顿 4-吊销资质 */
    private Integer level;

    /** 处罚分级描述 */
    private String levelDesc;

    /** 案件处置状态：0-待核查 1-核查中 2-已结案下达文书 */
    private Integer punishState;

    /** 处置状态描述 */
    private String punishStateDesc;

    /** 存证佐证数据说明 */
    private String evidence;

    /** 处置监察员ID */
    private Long handledBy;

    /** 处置监察员姓名 */
    private String handlerName;

    /** 办结时间 */
    private LocalDateTime handledAt;

    /** 案件记录时间 */
    private LocalDateTime createdAt;
}
