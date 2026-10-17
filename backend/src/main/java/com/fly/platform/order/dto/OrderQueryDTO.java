package com.fly.platform.order.dto;

import lombok.Data;

/**
 * 订单分页综合检索入参传输对象
 *
 * @author SoarCloud Team
 */
@Data
public class OrderQueryDTO {

    /** 商户企业ID */
    private Long merchantId;

    /** 客户档案ID */
    private Long customerId;

    /** 订单九态状态筛选 */
    private Integer orderState;

    /** 订单流水号精确检索 */
    private String orderSn;

    /** 模糊关键字（地址或货物） */
    private String keyword;

    /** 页码 */
    private Integer page = 1;

    /** 每页条数 */
    private Integer size = 10;
}
