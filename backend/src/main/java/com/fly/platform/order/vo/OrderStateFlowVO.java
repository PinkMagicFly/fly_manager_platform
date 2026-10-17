package com.fly.platform.order.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单状态变迁流水视图对象
 *
 * @author SoarCloud Team
 */
@Data
public class OrderStateFlowVO {

    /** 流水ID */
    private Long flowId;

    /** 所属订单ID */
    private Long orderId;

    /** 原状态编码 */
    private Integer fromState;

    /** 原状态描述 */
    private String fromStateDesc;

    /** 目标状态编码 */
    private Integer toState;

    /** 目标状态描述 */
    private String toStateDesc;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人账号/姓名 */
    private String operatorName;

    /** 流转备注说明 */
    private String remark;

    /** 发生时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
