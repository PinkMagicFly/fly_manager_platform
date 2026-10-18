package com.fly.platform.ticket.dto;

import lombok.Data;

/**
 * 办结关闭工单入参 DTO
 *
 * @author SoarCloud Team
 */
@Data
public class TicketCloseDTO {

    /** 办结说明备注（可选） */
    private String note;
}
