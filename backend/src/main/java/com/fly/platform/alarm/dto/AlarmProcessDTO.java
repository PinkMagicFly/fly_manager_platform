package com.fly.platform.alarm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 告警处置操作方案与办结说明入参 DTO
 *
 * @author SoarCloud Team
 */
@Data
public class AlarmProcessDTO {

    /** 处置方案说明或办结解除记录 */
    @NotBlank(message = "处置说明不能为空")
    private String note;
}
