package com.fly.platform.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fly.platform.asset.entity.CrewShift;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 机组排班计划持久层 Mapper
 *
 * @author SoarCloud Team
 */
@Mapper
public interface CrewShiftMapper extends BaseMapper<CrewShift> {

    /**
     * 检测指定机组人员在特定日期与时段是否存在重叠的排班
     * 重叠条件：(start_time < #{endTime}) AND (end_time > #{startTime})
     * 并且排班状态不为已取消/请假调休(4)
     */
    @Select("SELECT * FROM crew_shift WHERE crew_id = #{crewId} " +
            "AND shift_date = #{shiftDate} AND shift_state != 4 AND is_deleted = 0 " +
            "AND start_time < #{endTime} AND end_time > #{startTime}")
    List<CrewShift> selectOverlappingShifts(@Param("crewId") Long crewId,
                                            @Param("shiftDate") LocalDate shiftDate,
                                            @Param("startTime") LocalTime startTime,
                                            @Param("endTime") LocalTime endTime);

    /** 查询指定日期范围内的所有机组排班（周视图矩阵） */
    @Select("SELECT * FROM crew_shift WHERE shift_date >= #{startDate} AND shift_date <= #{endDate} " +
            "AND is_deleted = 0 ORDER BY shift_date ASC, start_time ASC")
    List<CrewShift> selectShiftsByDateRange(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);
}
