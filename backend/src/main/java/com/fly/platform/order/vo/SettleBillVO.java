package com.fly.platform.order.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 财务结算账单视图对象
 *
 * @author SoarCloud Team
 */
@Data
public class SettleBillVO {

    /** 账单ID */
    private Long billId;

    /** 对账单流水号 */
    private String billSn;

    /** 关联订单ID */
    private Long orderId;

    /** 关联订单编号 */
    private String orderSn;

    /** 结算商户ID */
    private Long merchantId;

    /** 结算商户名称 */
    private String merchantName;

    /** 结算总金额（元） */
    private BigDecimal amount;

    /** 账单状态编码：0-待出账 1-待对账 2-已对账 3-已开票 */
    private Integer billState;

    /** 账单状态描述 */
    private String billStateDesc;

    /** 发票开具标记：0-未开票 1-已开票 */
    private Integer invoiceFlag;

    /** 系统出账时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime settleAt;

    /** 记录创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
