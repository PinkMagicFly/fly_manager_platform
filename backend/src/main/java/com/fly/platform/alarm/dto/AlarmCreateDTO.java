package com.fly.platform.alarm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 手工登记告警事件入参 DTO
 *
 * @author SoarCloud Team
 */
@Data
public class AlarmCreateDTO {

    /** 告警严重级别：1-一般 2-严重 3-紧急 */
    @NotNull(message = "告警级别不能为空")
    @Min(value = 1, message = "告警级别数值非法")
    @Max(value = 3, message = "告警级别数值非法")
    private Integer level;

    /** 告警标题简述 */
    @NotBlank(message = "告警标题不能为空")
    private String title;

    /** 详细描述内容 */
    @NotBlank(message = "告警详细内容不能为空")
    private String content;

    /** 涉及无人机ID（可选） */
    private Long uavId;

    /** 涉及飞行计划ID（可选） */
    private Long applyId;
}
