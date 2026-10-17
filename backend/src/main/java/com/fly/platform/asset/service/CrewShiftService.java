package com.fly.platform.asset.service;

import com.fly.platform.asset.dto.CrewShiftDTO;
import com.fly.platform.asset.entity.CrewShift;
import com.fly.platform.asset.vo.CrewShiftWeekVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 机组排班计划服务接口
 *
 * @author SoarCloud Team
 */
public interface CrewShiftService {

    /** 查询指定起始日期的周视图排班矩阵 (包含7天排班及周累计工时) */
    List<CrewShiftWeekVO> getWeekScheduleMatrix(LocalDate startDate);

    /** 提交新增机组排班（含执照有效期守卫与重叠时段防重检测） */
    CrewShift scheduleShift(CrewShiftDTO dto);

    /** 取消指定排班 */
    void cancelShift(Long shiftId);
}
