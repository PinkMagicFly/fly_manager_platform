package com.fly.platform.airspace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fly.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 低空飞行违规调查与行政执法留痕实体
 *
 * @author SoarCloud Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("violation_case")
public class ViolationCase extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 违规案件唯一标识 */
    @TableId(type = IdType.AUTO)
    private Long caseId;

    /** 案件编号（业务唯一流水号，如 VC202610170001） */
    private String caseSn;

    /** 关联飞行计划申报单ID */
    private Long applyId;

    /** 违规行为类别：1-航线严重偏航 2-非法入侵禁飞区 3-超越法定限高 4-超时占用空域 */
    private Integer violateKind;

    /** 行政处罚分级：1-警告留痕 2-企业罚款 3-停飞整顿 4-吊销资质 */
    private Integer level;

    /** 案件处置状态：0-待核查 1-核查中 2-已结案下达文书 */
    private Integer punishState;

    /** 存证佐证数据与违规说明 */
    private String evidence;

    /** 处置监察员ID */
    private Long handledBy;

    /** 案件办结时间 */
    private LocalDateTime handledAt;
}
