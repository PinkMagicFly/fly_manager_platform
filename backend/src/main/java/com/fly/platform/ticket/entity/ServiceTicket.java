package com.fly.platform.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fly.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 客户服务与异常工单业务实体
 *
 * @author SoarCloud Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("service_ticket")
public class ServiceTicket extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工单唯一主键ID */
    @TableId(type = IdType.AUTO)
    @TableField("ticket_id")
    private Long ticketId;

    /** 工单业务流水号 */
    @TableField("ticket_sn")
    private String ticketSn;

    /** 提交客户账户ID */
    @TableField("account_id")
    private Long customerId;

    /** 关联货运订单ID（可选） */
    @TableField("order_id")
    private Long orderId;

    /** 工单类型：1-业务咨询 2-时效投诉 3-异常报修 */
    @TableField("ticket_type")
    private Integer type;

    /** 工单简述标题 */
    @TableField("title")
    private String title;

    /** 详细诉求内容 */
    @TableField("content")
    private String content;

    /** 工单状态：0-待处理 1-处理中 2-已关闭 */
    @TableField("ticket_state")
    private Integer state;

    /** 官方处理答复与流转记录 JSON 列表 */
    @TableField("reply_content")
    private String replies;

    /** 经办专员账户ID（可选） */
    @TableField("handled_by")
    private Long assigneeId;
}
