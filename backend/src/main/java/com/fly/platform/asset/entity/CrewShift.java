package com.fly.platform.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fly.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 机组排班计划实体
 *
 * @author SoarCloud Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crew_shift")
public class CrewShift extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 排班唯一标识 */
    @TableId(type = IdType.AUTO)
    private Long shiftId;

    /** 机组人员ID */
    private Long crewId;

    /** 排班执勤日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate shiftDate;

    /** 值班起始时刻 */
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    /** 值班截止时刻 */
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;

    /** 排班状态：1-已排班 2-执勤中 3-已完成 4-请假调休 */
    private Integer shiftState;
}
