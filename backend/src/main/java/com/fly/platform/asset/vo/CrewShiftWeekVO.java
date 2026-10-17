package com.fly.platform.asset.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 周视图排班矩阵视图 VO
 *
 * @author SoarCloud Team
 */
@Data
public class CrewShiftWeekVO {

    /** 机组人员ID */
    private Long crewId;

    /** 机组人员真实姓名 */
    private String realName;

    /** 执照编号 */
    private String licenseNo;

    /** 岗位描述 */
    private String crewTypeDesc;

    /** 本周累计排班工时（小时） */
    private Double totalWeekHours;

    /** 本周具体排班单元列表 */
    private List<ShiftItemVO> shifts;

    @Data
    public static class ShiftItemVO {
        private Long shiftId;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate shiftDate;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime startTime;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime endTime;
        private Integer shiftState;
        private String shiftStateDesc;
    }
}
