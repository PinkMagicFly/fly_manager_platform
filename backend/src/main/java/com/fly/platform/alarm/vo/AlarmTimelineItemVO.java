package com.fly.platform.alarm.vo;

import lombok.Data;

/**
 * 告警生命周期时间线节点明细
 *
 * @author SoarCloud Team
 */
@Data
public class AlarmTimelineItemVO {

    /** 动作发生时间戳 (yyyy-MM-dd HH:mm:ss) */
    private String at;

    /** 执行人员姓名或主体 */
    private String by;

    /** 操作动作类型 */
    private String action;

    /** 处置方案或详细说明 */
    private String note;
}
