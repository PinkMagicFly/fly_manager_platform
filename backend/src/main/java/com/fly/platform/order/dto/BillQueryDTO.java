package com.fly.platform.order.dto;

import lombok.Data;

/**
 * 财务对账单据检索入参对象
 *
 * @author SoarCloud Team
 */
@Data
public class BillQueryDTO {

    /** 结算商户企业ID */
    private Long merchantId;

    /** 账单状态筛选：0-待出账 1-待对账 2-已对账 3-已开票 */
    private Integer billState;

    /** 对账单号精确查询 */
    private String billSn;

    /** 页码 */
    private Integer page = 1;

    /** 每页条数 */
    private Integer size = 10;
}
