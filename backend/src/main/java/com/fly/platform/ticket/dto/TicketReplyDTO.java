package com.fly.platform.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 官方客服工单答复入参 DTO
 *
 * @author SoarCloud Team
 */
@Data
public class TicketReplyDTO {

    /** 官方答复内容文本 */
    @NotBlank(message = "回复内容不能为空")
    private String content;
}
