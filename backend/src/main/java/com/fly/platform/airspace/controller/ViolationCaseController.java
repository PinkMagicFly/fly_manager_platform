package com.fly.platform.airspace.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.fly.platform.airspace.dto.ViolationHandleDTO;
import com.fly.platform.airspace.service.ViolationCaseService;
import com.fly.platform.airspace.vo.ViolationCaseVO;
import com.fly.platform.airspace.vo.ViolationDetailVO;
import com.fly.platform.common.api.Result;
import com.fly.platform.common.audit.Audit;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 飞行违规案件调查与定责处罚控制器
 *
 * @author SoarCloud Team
 */
@RestController
@RequestMapping("/api/v1/violation")
@RequiredArgsConstructor
public class ViolationCaseController {

    private final ViolationCaseService violationCaseService;

    /**
     * 条件检索违规案件列表
     */
    @GetMapping("/list")
    public Result<List<ViolationCaseVO>> getCaseList(@RequestParam(value = "violateKind", required = false) Integer violateKind,
                                                     @RequestParam(value = "punishState", required = false) Integer punishState) {
        return Result.ok(violationCaseService.getCaseList(violateKind, punishState));
    }

    /**
     * 获取案件全景详情与偏航对比
     */
    @GetMapping("/detail/{id}")
    public Result<ViolationDetailVO> getCaseDetail(@PathVariable("id") Long id) {
        return Result.ok(violationCaseService.getCaseDetail(id));
    }

    /**
     * 空管定责并下达行政处罚通知书
     */
    @SaCheckLogin
    @Audit(module = "违规处置", action = "下达行政处罚通知书")
    @PostMapping("/handle")
    public Result<Void> handleCase(@Valid @RequestBody ViolationHandleDTO dto) {
        Long handlerId = StpUtil.getLoginIdAsLong();
        violationCaseService.handleCase(dto, handlerId);
        return Result.ok("行政处罚决定书已生成并归档送达", null);
    }
}
