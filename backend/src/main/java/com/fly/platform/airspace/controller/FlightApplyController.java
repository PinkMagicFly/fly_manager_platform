package com.fly.platform.airspace.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import com.fly.platform.airspace.dto.FlightApplyCreateDTO;
import com.fly.platform.airspace.dto.FlightApproveDTO;
import com.fly.platform.airspace.entity.FlightApply;
import com.fly.platform.airspace.service.FlightApplyService;
import com.fly.platform.airspace.vo.FlightApplyVO;
import com.fly.platform.common.api.Result;
import com.fly.platform.common.audit.Audit;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 飞行计划申报与空管合规审批控制器
 *
 * @author SoarCloud Team
 */
@RestController
@RequestMapping("/api/v1/flight-apply")
@RequiredArgsConstructor
public class FlightApplyController {

    private final FlightApplyService flightApplyService;

    /**
     * 查询飞行计划申报列表
     */
    @GetMapping("/list")
    public Result<List<FlightApplyVO>> getApplyList(@RequestParam(value = "merchantId", required = false) Long merchantId,
                                                    @RequestParam(value = "approveState", required = false) Integer approveState) {
        return Result.ok(flightApplyService.getApplyList(merchantId, approveState));
    }

    /**
     * 查询飞行申报单详情
     */
    @GetMapping("/detail/{id}")
    public Result<FlightApplyVO> getApplyDetail(@PathVariable("id") Long id) {
        return Result.ok(flightApplyService.getApplyDetail(id));
    }

    /**
     * 提交新飞行计划在线申报
     */
    @SaCheckLogin
    @Audit(module = "飞行申报", action = "提交飞行计划申请")
    @PostMapping("/create")
    public Result<FlightApply> createApply(@Valid @RequestBody FlightApplyCreateDTO dto) {
        Long operatorId = StpUtil.getLoginIdAsLong();
        FlightApply apply = flightApplyService.createApply(dto, operatorId);
        return Result.ok("飞行计划已提交申报，进入待审批队列", apply);
    }

    /**
     * 空管在线审批处理（通过/驳回）- 严格限制仅限空管监察员与超管
     */
    @SaCheckLogin
    @SaCheckRole(value = {"ROLE_ATC", "ROLE_ADMIN"}, mode = SaMode.OR)
    @Audit(module = "飞行申报", action = "空管在线审批")
    @PostMapping("/approve")
    public Result<Void> approveApply(@Valid @RequestBody FlightApproveDTO dto) {
        Long approverId = StpUtil.getLoginIdAsLong();
        flightApplyService.approveApply(dto, approverId);
        return Result.ok("审批决策已下达并存证", null);
    }

    /**
     * 申报人撤回申报申请
     */
    @SaCheckLogin
    @Audit(module = "飞行申报", action = "撤销飞行计划")
    @PostMapping("/cancel/{id}")
    public Result<Void> cancelApply(@PathVariable("id") Long id) {
        Long operatorId = StpUtil.getLoginIdAsLong();
        flightApplyService.cancelApply(id, operatorId);
        return Result.ok("飞行计划申报已撤销", null);
    }
}

