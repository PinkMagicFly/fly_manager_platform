package com.fly.platform.asset.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 机组排班录入入参
 *
 * @author SoarCloud Team
 */
@Data
public class CrewShiftDTO {

    /** 机组人员ID */
    @NotNull(message = "机组人员不可为空")
    private Long crewId;

    /** 排班执勤日期 */
    @NotNull(message = "排班执勤日期不可为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate shiftDate;

    /** 值班起始时刻 */
    @NotNull(message = "值班起始时刻不可为空")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    /** 值班截止时刻 */
    @NotNull(message = "值班截止时刻不可为空")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;
}
