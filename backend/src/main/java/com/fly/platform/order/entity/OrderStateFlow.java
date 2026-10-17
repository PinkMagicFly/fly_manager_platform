package com.fly.platform.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单状态全生命周期变迁流水审计实体
 *
 * @author SoarCloud Team
 */
@Data
@TableName("order_state_flow")
public class OrderStateFlow implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态流水ID */
    @TableId(type = IdType.AUTO)
    private Long flowId;

    /** 所属业务订单ID */
    private Long orderId;

    /** 变迁前状态 */
    private Integer fromState;

    /** 变迁后目标状态 */
    private Integer toState;

    /** 触发操作人ID */
    private Long operatorId;

    /** 流转说明或异常原因 */
    private String remark;

    /** 状态变迁发生时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
