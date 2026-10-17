package com.fly.platform.airspace.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.fly.platform.airspace.dto.FenceRuleDTO;
import com.fly.platform.airspace.entity.FenceRule;
import com.fly.platform.airspace.service.FenceRuleService;
import com.fly.platform.common.api.Result;
import com.fly.platform.common.audit.Audit;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 电子围栏合规校验规则字典控制器
 *
 * @author SoarCloud Team
 */
@RestController
@RequestMapping("/api/v1/fence-rule")
@RequiredArgsConstructor
public class FenceRuleController {

    private final FenceRuleService fenceRuleService;

    /**
     * 查询全量电子围栏规则
     */
    @GetMapping("/list")
    public Result<List<FenceRule>> getRuleList() {
        return Result.ok(fenceRuleService.getRuleList());
    }

    /**
     * 更新电子围栏规则参数 - 仅限空管与超管
     */
    @SaCheckLogin
    @SaCheckRole(value = {"ROLE_ATC", "ROLE_ADMIN"}, mode = SaMode.OR)
    @Audit(module = "围栏规则", action = "更新围栏参数")
    @PutMapping("/update")
    public Result<FenceRule> updateRule(@Valid @RequestBody FenceRuleDTO dto) {
        return Result.ok("电子围栏规则已更新生效", fenceRuleService.updateRule(dto));
    }
}

