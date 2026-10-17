package com.fly.platform.asset.controller;

import com.fly.platform.asset.dto.CrewShiftDTO;
import com.fly.platform.asset.dto.CrewStaffDTO;
import com.fly.platform.asset.entity.CrewShift;
import com.fly.platform.asset.entity.CrewStaff;
import com.fly.platform.asset.service.CrewShiftService;
import com.fly.platform.asset.service.CrewStaffService;
import com.fly.platform.asset.vo.CrewShiftWeekVO;
import com.fly.platform.asset.vo.CrewStaffVO;
import com.fly.platform.common.api.Result;
import com.fly.platform.common.audit.Audit;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 机组人员资质台账与排班矩阵控制器
 *
 * @author SoarCloud Team
 */
@RestController
@RequestMapping("/api/v1/crew")
@RequiredArgsConstructor
public class CrewController {

    private final CrewStaffService crewStaffService;
    private final CrewShiftService crewShiftService;

    @GetMapping("/staff/list")
    public Result<List<CrewStaffVO>> listCrewStaff() {
        return Result.ok(crewStaffService.listCrewStaff());
    }

    @PostMapping("/staff")
    @Audit(module = "机组管理", action = "登记机组执勤人员档案")
    public Result<CrewStaff> createCrewStaff(@Valid @RequestBody CrewStaffDTO dto) {
        return Result.ok("机组人员档案登记成功", crewStaffService.createCrewStaff(dto));
    }

    @GetMapping("/shift/week")
    public Result<List<CrewShiftWeekVO>> getWeekSchedule(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate) {
        return Result.ok(crewShiftService.getWeekScheduleMatrix(startDate));
    }

    @PostMapping("/shift")
    @Audit(module = "机组管理", action = "录入机组执勤排班")
    public Result<CrewShift> scheduleShift(@Valid @RequestBody CrewShiftDTO dto) {
        CrewShift shift = crewShiftService.scheduleShift(dto);
        return Result.ok("机组执勤排班录入成功", shift);
    }

    @DeleteMapping("/shift/{shiftId}")
    @Audit(module = "机组管理", action = "调休/取消机组排班")
    public Result<Void> cancelShift(@PathVariable("shiftId") Long shiftId) {
        crewShiftService.cancelShift(shiftId);
        return Result.ok("排班已变更为请假调休", null);
    }
}
