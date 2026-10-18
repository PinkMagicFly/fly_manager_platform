package com.fly.platform.ticket.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户服务工单详情展示视图对象
 *
 * @author SoarCloud Team
 */
@Data
public class TicketVO {

    /** 工单ID */
    private Long ticketId;

    /** 工单业务流水号 */
    private String ticketSn;

    /** 提交客户账户ID */
    private Long customerId;

    /** 提交人登录名或姓名 */
    private String customerName;

    /** 关联运单ID */
    private Long orderId;

    /** 关联运单业务流水号 */
    private String orderSn;

    /** 工单类型：1-咨询 2-投诉 3-报修 */
    private Integer type;

    /** 工单类型中文标签 */
    private String typeLabel;

    /** 工单简述标题 */
    private String title;

    /** 详细诉求内容 */
    private String content;

    /** 处理状态：0-待处理 1-处理中 2-已关闭 */
    private Integer state;

    /** 状态中文标签 */
    private String stateLabel;

    /** 经办专员账户ID */
    private Long assigneeId;

    /** 经办专员登录名或姓名 */
    private String assigneeName;

    /** 结构化答复流水列表 */
    private List<TicketReplyItemVO> replies;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
