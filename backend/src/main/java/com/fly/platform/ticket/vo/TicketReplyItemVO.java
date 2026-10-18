package com.fly.platform.ticket.vo;

import lombok.Data;

/**
 * 工单回复留言与时间线记录明细
 *
 * @author SoarCloud Team
 */
@Data
public class TicketReplyItemVO {

    /** 回复发生时间 */
    private String at;

    /** 回复人账号或姓名 */
    private String by;

    /** 回复人身份角色标签 */
    private String role;

    /** 答复文本主体内容 */
    private String content;
}
