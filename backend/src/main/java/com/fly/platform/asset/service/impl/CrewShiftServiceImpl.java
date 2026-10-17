package com.fly.platform.asset.service.impl;

import com.fly.platform.asset.dto.CrewShiftDTO;
import com.fly.platform.asset.entity.CrewShift;
import com.fly.platform.asset.entity.CrewStaff;
import com.fly.platform.asset.mapper.CrewShiftMapper;
import com.fly.platform.asset.mapper.CrewStaffMapper;
import com.fly.platform.asset.service.CrewShiftService;
import com.fly.platform.asset.vo.CrewShiftWeekVO;
import com.fly.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 机组排班计划业务实现类
 *
 * @author SoarCloud Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrewShiftServiceImpl implements CrewShiftService {

    private final CrewShiftMapper crewShiftMapper;
    private final CrewStaffMapper crewStaffMapper;

    @Override
    public List<CrewShiftWeekVO> getWeekScheduleMatrix(LocalDate startDate) {
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        LocalDate endDate = startDate.plusDays(6);

        List<CrewStaff> allStaff = crewStaffMapper.selectAllCrew();
        List<CrewShift> shifts = crewShiftMapper.selectShiftsByDateRange(startDate, endDate);

        Map<Long, List<CrewShift>> shiftGroup = shifts.stream()
                .collect(Collectors.groupingBy(CrewShift::getCrewId));

        List<CrewShiftWeekVO> result = new ArrayList<>();
        for (CrewStaff staff : allStaff) {
            CrewShiftWeekVO vo = new CrewShiftWeekVO();
            vo.setCrewId(staff.getCrewId());
            vo.setRealName(staff.getRealName());
            vo.setLicenseNo(staff.getLicenseNo());
            vo.setCrewTypeDesc(staff.getCrewType() == 1 ? "商用机长" : "运维专员");

            List<CrewShift> staffShifts = shiftGroup.getOrDefault(staff.getCrewId(), new ArrayList<>());
            double weekHours = 0.0;
            List<CrewShiftWeekVO.ShiftItemVO> items = new ArrayList<>();

            for (CrewShift s : staffShifts) {
                CrewShiftWeekVO.ShiftItemVO item = new CrewShiftWeekVO.ShiftItemVO();
                item.setShiftId(s.getShiftId());
                item.setShiftDate(s.getShiftDate());
                item.setStartTime(s.getStartTime());
                item.setEndTime(s.getEndTime());
                item.setShiftState(s.getShiftState());

                if (s.getShiftState() == 1) {
                    item.setShiftStateDesc("已排班");
                } else if (s.getShiftState() == 2) {
                    item.setShiftStateDesc("执勤中");
                } else if (s.getShiftState() == 3) {
                    item.setShiftStateDesc("已完成");
                } else {
                    item.setShiftStateDesc("调休请假");
                }

                if (s.getStartTime() != null && s.getEndTime() != null) {
                    long minutes = Duration.between(s.getStartTime(), s.getEndTime()).toMinutes();
                    if (minutes > 0) {
                        weekHours += (minutes / 60.0);
                    }
                }
                items.add(item);
            }

            vo.setTotalWeekHours(Math.round(weekHours * 10.0) / 10.0);
            vo.setShifts(items);
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrewShift scheduleShift(CrewShiftDTO dto) {
        if (dto.getStartTime() == null || dto.getEndTime() == null || !dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BusinessException(400, "排班起止时间无效：截止时刻必须晚于起始时刻");
        }

        // 1. 守卫一：执照有效期校验
        CrewStaff staff = crewStaffMapper.selectById(dto.getCrewId());
        if (staff == null) {
            throw new BusinessException(404, "机组人员档案不存在");
        }
        if (staff.getLicenseExpire() != null && staff.getLicenseExpire().isBefore(dto.getShiftDate())) {
            throw new BusinessException(400, "该机组人员执照已过期，不可排班");
        }

        // 2. 守卫二：排班时段重叠防重校验
        List<CrewShift> overlapping = crewShiftMapper.selectOverlappingShifts(
                dto.getCrewId(), dto.getShiftDate(), dto.getStartTime(), dto.getEndTime()
        );
        if (!overlapping.isEmpty()) {
            throw new BusinessException(409, "该机组人员在该时段已存在执勤班次，不可重复排班");
        }

        // 3. 落库排班
        CrewShift shift = new CrewShift();
        shift.setCrewId(dto.getCrewId());
        shift.setShiftDate(dto.getShiftDate());
        shift.setStartTime(dto.getStartTime());
        shift.setEndTime(dto.getEndTime());
        shift.setShiftState(1); // 1-已排班
        shift.initAudit();
        crewShiftMapper.insert(shift);

        log.info("机组排班成功: shiftId={}, crewId={}, date={}, time={}~{}",
                shift.getShiftId(), dto.getCrewId(), dto.getShiftDate(), dto.getStartTime(), dto.getEndTime());
        return shift;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelShift(Long shiftId) {
        CrewShift shift = crewShiftMapper.selectById(shiftId);
        if (shift == null) {
            throw new BusinessException(404, "排班记录不存在");
        }
        shift.setShiftState(4); // 4-请假调休
        shift.updateAudit();
        crewShiftMapper.updateById(shift);
    }
}
