package com.fly.platform.alarm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警事件综合详情视图对象
 *
 * @author SoarCloud Team
 */
@Data
public class AlarmVO {

    /** 告警唯一ID */
    private Long alarmId;

    /** 告警流水号 */
    private String alarmSn;

    /** 关联飞行计划申报ID */
    private Long applyId;

    /** 关联无人机资产ID */
    private Long uavId;

    /** 关联无人机序列号编码 */
    private String uavSn;

    /** 关联运单ID */
    private Long orderId;

    /** 关联运单业务流水号 */
    private String orderSn;

    /** 告警级别数值：1-一般 2-严重 3-紧急 */
    private Integer level;

    /** 告警级别中文标签 */
    private String levelLabel;

    /** 告警来源：1-故障上报 2-手工登记 */
    private Integer source;

    /** 告警来源中文标签 */
    private String sourceLabel;

    /** 告警简述标题 */
    private String title;

    /** 详细描述内容 */
    private String content;

    /** 处置状态：0-未处置 1-处置中 2-已关闭 */
    private Integer state;

    /** 处置状态中文标签 */
    private String stateLabel;

    /** 结构化处置生命周期时间线列表 */
    private List<AlarmTimelineItemVO> timeline;

    /** 办结关闭时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime handledAt;

    /** 告警产生时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
