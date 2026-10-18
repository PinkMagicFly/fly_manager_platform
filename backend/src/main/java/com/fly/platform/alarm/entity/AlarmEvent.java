package com.fly.platform.alarm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fly.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 低空运营实时告警事件业务实体
 *
 * @author SoarCloud Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("alarm_event")
public class AlarmEvent extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 告警唯一主键ID */
    @TableId(type = IdType.AUTO)
    @TableField("alarm_id")
    private Long alarmId;

    /** 告警业务流水号 */
    @TableField("alarm_sn")
    private String alarmSn;

    /** 关联飞行计划申报ID（可选） */
    @TableField("apply_id")
    private Long applyId;

    /** 关联无人机资产ID（可选） */
    @TableField("uav_id")
    private Long uavId;

    /** 告警严重级别：1-一般 2-严重 3-紧急 */
    @TableField("alarm_level")
    private Integer level;

    /** 告警类型与标题简述 */
    @TableField("alarm_type")
    private String title;

    /** 告警异常详细描述 */
    @TableField("content")
    private String content;

    /** 处置状态：0-未处置 1-处置中 2-已关闭 */
    @TableField("handle_state")
    private Integer state;

    /** 处置全生命周期时间线 JSON 数组字符 */
    @TableField("handle_note")
    private String timeline;

    /** 告警最终办结关闭时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("handled_at")
    private LocalDateTime handledAt;
}
