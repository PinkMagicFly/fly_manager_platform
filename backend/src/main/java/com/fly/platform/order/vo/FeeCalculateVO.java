package com.fly.platform.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 运费阶梯试算返回视图对象
 *
 * @author SoarCloud Team
 */
@Data
public class FeeCalculateVO {

    /** 航程直线距离（公里） */
    private BigDecimal distanceKm;

    /** 基础起步运费（元） */
    private BigDecimal feeBase;

    /** 空域通道占用费（元） */
    private BigDecimal feeAirspace;

    /** 加急优先配送附加费（元） */
    private BigDecimal feeUrgent;

    /** 优惠抵扣金额（元） */
    private BigDecimal feeDiscount;

    /** 应付总费用（元） */
    private BigDecimal feeTotal;

    /** 试算公式中文解释说明 */
    private String formulaDesc;
}
