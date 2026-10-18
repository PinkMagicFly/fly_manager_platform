package com.fly.platform.trace.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单飞行轨迹全景回放 VO
 * 包含申报航线折线点序列与实际回放航迹点序列
 *
 * @author SoarCloud Team
 */
@Data
public class OrderTraceVO {

    /** 所属订单ID */
    private Long orderId;

    /** 关联飞行任务申报ID */
    private Long applyId;

    /** 申报任务编号 */
    private String applySn;

    /** 执飞机号 */
    private String uavSn;

    /** 执飞机型 */
    private String uavModel;

    /** 执飞机长姓名 */
    private String pilotName;

    /** 订单当前状态 */
    private Integer orderState;

    /** 订单当前状态中文描述 */
    private String orderStateDesc;

    /** 规划巡航高度（米） */
    private Integer altPlanned;

    /** 规划申报航线点序列 [[lng, lat], ...] */
    private List<List<Double>> planLine = new ArrayList<>();

    /** 实际飞行轨迹点序列（按采样时间升序） */
    private List<TracePointVO> actual = new ArrayList<>();
}
