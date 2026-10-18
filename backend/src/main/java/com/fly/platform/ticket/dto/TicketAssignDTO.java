package com.fly.platform.ticket.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 工单转派经办客服专员入参 DTO
 *
 * @author SoarCloud Team
 */
@Data
public class TicketAssignDTO {

    /** 经办专员账户ID */
    @NotNull(message = "分派专员ID不能为空")
    private Long assigneeId;
}
