package com.fly.platform.order.vo;

import lombok.Data;

/**
 * 订单看板九态统计数据视图对象
 *
 * @author SoarCloud Team
 */
@Data
public class OrderBoardStatsVO {

    /** 全量运单总数 */
    private Integer totalCount = 0;

    /** 待支付订单数 (10) */
    private Integer pendingPayCount = 0;

    /** 待接单订单数 (20) */
    private Integer pendingAcceptCount = 0;

    /** 待调度订单数 (30) */
    private Integer pendingDispatchCount = 0;

    /** 待起飞订单数 (40) */
    private Integer pendingTakeoffCount = 0;

    /** 飞行中在途订单数 (50) */
    private Integer flyingCount = 0;

    /** 已完成送达订单数 (60) */
    private Integer deliveredCount = 0;

    /** 已取消订单数 (70) */
    private Integer cancelledCount = 0;

    /** 异常中断订单数 (80) */
    private Integer abortedCount = 0;
}
