package com.fly.platform.sysparam.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.fly.platform.common.api.Result;
import com.fly.platform.common.audit.Audit;
import com.fly.platform.sysparam.dto.SysParamUpdateDTO;
import com.fly.platform.sysparam.service.SysParamService;
import com.fly.platform.sysparam.vo.SysParamVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 平台系统运行参数管理控制器
 *
 * @author SoarCloud Team
 */
@RestController
@RequestMapping("/api/v1/params")
@RequiredArgsConstructor
public class SysParamController {

    private final SysParamService sysParamService;

    /**
     * 获取全量系统运行参数列表
     */
    @GetMapping
    @SaCheckLogin
    public Result<List<SysParamVO>> listParams() {
        return Result.ok(sysParamService.listAll());
    }

    /**
     * 单个更新运行参数
     */
    @PutMapping("/{key}")
    @SaCheckRole("ROLE_ADMIN")
    @Audit(module = "系统参数", action = "单个修改运行参数")
    public Result<Void> updateParam(@PathVariable String key, @RequestBody SysParamUpdateDTO dto) {
        sysParamService.put(key, dto.getParamValue());
        return Result.ok();
    }

    /**
     * 批量更新系统运行参数
     */
    @PutMapping
    @SaCheckRole("ROLE_ADMIN")
    @Audit(module = "系统参数", action = "批量修改运行参数")
    public Result<Void> updateParams(@Valid @RequestBody List<SysParamUpdateDTO> params) {
        sysParamService.updateParams(params);
        return Result.ok();
    }
}

