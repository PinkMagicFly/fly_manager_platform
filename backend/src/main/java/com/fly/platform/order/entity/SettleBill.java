package com.fly.platform.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fly.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 货运结算与财务对账单据实体
 *
 * @author SoarCloud Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("settle_bill")
public class SettleBill extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 账单唯一标识 */
    @TableId(type = IdType.AUTO)
    private Long billId;

    /** 对账单编号 (如 BILL202610170001) */
    private String billSn;

    /** 关联订单ID */
    private Long orderId;

    /** 结算商户ID */
    private Long merchantId;

    /** 结算应付总额（元） */
    private BigDecimal amount;

    /** 账单状态：0-待出账 1-待对账 2-已对账 3-已开票 */
    private Integer billState;

    /** 发票开具标记：0-未开票 1-已开票 */
    private Integer invoiceFlag;

    /** 系统出账时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime settleAt;
}
