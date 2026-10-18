package com.fly.platform.ticket.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 客户创建服务工单入参 DTO
 *
 * @author SoarCloud Team
 */
@Data
public class TicketCreateDTO {

    /** 工单类型：1-咨询 2-投诉 3-报修 */
    @NotNull(message = "工单类型不能为空")
    @Min(value = 1, message = "工单类型非法")
    @Max(value = 3, message = "工单类型非法")
    private Integer type;

    /** 工单简述标题 */
    @NotBlank(message = "工单标题不能为空")
    private String title;

    /** 详细诉求内容 */
    @NotBlank(message = "诉求详细内容不能为空")
    private String content;

    /** 涉及运单ID（可选） */
    private Long orderId;
}
