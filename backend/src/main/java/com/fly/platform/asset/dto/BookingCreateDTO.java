package com.fly.platform.asset.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 场站机位预约创建入参
 *
 * @author SoarCloud Team
 */
@Data
public class BookingCreateDTO {

    /** 目标场站ID */
    @NotNull(message = "目标场站ID不可为空")
    private Long siteId;

    /** 预约无人机ID */
    @NotNull(message = "预约无人机ID不可为空")
    private Long uavId;

    /** 关联飞行申报单ID (选填) */
    private Long applyId;

    /** 预约占用起始时间 */
    @NotNull(message = "预约起始时间不可为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    /** 预约占用截止时间 */
    @NotNull(message = "预约截止时间不可为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;
}
