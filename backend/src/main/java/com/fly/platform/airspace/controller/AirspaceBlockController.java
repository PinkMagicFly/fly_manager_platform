package com.fly.platform.airspace.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.fly.platform.airspace.dto.AirspaceBlockDTO;
import com.fly.platform.airspace.dto.AirspaceValidateDTO;
import com.fly.platform.airspace.dto.RoutePlanDTO;
import com.fly.platform.airspace.entity.AirspaceBlock;
import com.fly.platform.airspace.service.AirspaceBlockService;
import com.fly.platform.airspace.vo.AirspaceBlockVO;
import com.fly.platform.airspace.vo.AirspaceValidateResultVO;
import com.fly.platform.airspace.vo.RoutePlanResultVO;
import com.fly.platform.common.api.Result;
import com.fly.platform.common.audit.Audit;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 低空空域管制网格与电子围栏态势控制器
 *
 * @author SoarCloud Team
 */
@RestController
@RequestMapping("/api/v1/airspace")
@RequiredArgsConstructor
public class AirspaceBlockController {

    private final AirspaceBlockService airspaceBlockService;

    /**
     * 查询全量空域管制区块（含三区分区着色坐标序列）
     */
    @GetMapping("/blocks")
    public Result<List<AirspaceBlockVO>> getAllBlocks() {
        return Result.ok(airspaceBlockService.getAllBlocks());
    }

    /**
     * 航线合规碰撞毫秒级即时校验与避障纠偏
     */
    @PostMapping("/validate")
    public Result<AirspaceValidateResultVO> validateRoute(@Valid @RequestBody AirspaceValidateDTO dto) {
        return Result.ok(airspaceBlockService.validateRoute(dto));
    }

    /**
     * 智能自动规划航线（两点送入避障规划器，支持智能绕行与合规初筛）
     */
    @PostMapping("/plan-route")
    public Result<RoutePlanResultVO> planRoute(@Valid @RequestBody RoutePlanDTO dto) {
        return Result.ok(airspaceBlockService.planRoute(dto));
    }

    /**
     * 新增或编辑空域管制区块 - 仅限空管监察员与超级管理员
     */
    @SaCheckLogin
    @SaCheckRole(value = {"ROLE_ATC", "ROLE_ADMIN"}, mode = SaMode.OR)
    @Audit(module = "空域网格", action = "保存空域区块")
    @PostMapping("/blocks/save")
    public Result<AirspaceBlock> saveBlock(@Valid @RequestBody AirspaceBlockDTO dto) {
        return Result.ok("空域网格区块保存成功", airspaceBlockService.saveBlock(dto));
    }

    /**
     * 逻辑删除空域区块 - 仅限空管监察员与超级管理员
     */
    @SaCheckLogin
    @SaCheckRole(value = {"ROLE_ATC", "ROLE_ADMIN"}, mode = SaMode.OR)
    @Audit(module = "空域网格", action = "删除空域区块")
    @DeleteMapping("/blocks/delete/{id}")
    public Result<Void> deleteBlock(@PathVariable("id") Long id) {
        airspaceBlockService.deleteBlock(id);
        return Result.ok("空域区块已删除", null);
    }
}

