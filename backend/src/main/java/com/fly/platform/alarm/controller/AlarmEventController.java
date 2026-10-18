package com.fly.platform.alarm.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.fly.platform.alarm.dto.AlarmCreateDTO;
import com.fly.platform.alarm.dto.AlarmProcessDTO;
import com.fly.platform.alarm.dto.AlarmReportDTO;
import com.fly.platform.alarm.service.AlarmEventService;
import com.fly.platform.alarm.vo.AlarmVO;
import com.fly.platform.common.api.PageResult;
import com.fly.platform.common.api.Result;
import com.fly.platform.common.audit.Audit;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 实时告警应急处置控制器
 *
 * @author SoarCloud Team
 */
@RestController
@RequestMapping("/api/v1/alarm")
@RequiredArgsConstructor
public class AlarmEventController {

    private final AlarmEventService alarmEventService;

    /**
     * 分页查询告警事件列表
     */
    @GetMapping
    @SaCheckLogin
    public Result<PageResult<AlarmVO>> pageAlarms(
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) Integer state,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "1") int pageNo,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {
        return Result.ok(alarmEventService.page(level, state, keyword, pageNo, pageSize));
    }

    /**
     * 查询告警事件详情及处置时间线
     */
    @GetMapping("/{id}")
    @SaCheckLogin
    public Result<AlarmVO> getDetail(@PathVariable Long id) {
        return Result.ok(alarmEventService.getDetail(id));
    }

    /**
     * 手工登记异常告警
     */
    @PostMapping
    @SaCheckRole(value = {"ROLE_ADMIN", "ROLE_MERCHANT"}, mode = SaMode.OR)
    @Audit(module = "告警应急", action = "手工登记告警")
    public Result<Long> createManual(@Valid @RequestBody AlarmCreateDTO dto) {
        return Result.ok(alarmEventService.createManual(dto));
    }

    /**
     * 开始处置告警
     */
    @PutMapping("/{id}/process")
    @SaCheckRole(value = {"ROLE_ADMIN", "ROLE_MERCHANT"}, mode = SaMode.OR)
    @Audit(module = "告警应急", action = "开始处置告警")
    public Result<Void> process(@PathVariable Long id, @Valid @RequestBody AlarmProcessDTO dto) {
        alarmEventService.process(id, dto);
        return Result.ok();
    }

    /**
     * 办结关闭告警
     */
    @PutMapping("/{id}/close")
    @SaCheckRole(value = {"ROLE_ADMIN", "ROLE_MERCHANT"}, mode = SaMode.OR)
    @Audit(module = "告警应急", action = "办结解除告警")
    public Result<Void> close(@PathVariable Long id, @Valid @RequestBody AlarmProcessDTO dto) {
        alarmEventService.close(id, dto);
        return Result.ok();
    }

    /**
     * 机长/现场人员突发故障上报
     */
    @PostMapping("/report")
    @SaCheckLogin
    @Audit(module = "在途应急", action = "机长现场故障紧急上报")
    public Result<Map<String, Object>> reportFault(@Valid @RequestBody AlarmReportDTO dto) {
        return Result.ok(alarmEventService.reportFault(dto));
    }
}

