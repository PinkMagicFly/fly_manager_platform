package com.fly.platform.asset.controller;

import com.fly.platform.asset.dto.FaultReportDTO;
import com.fly.platform.asset.dto.MaintainAcceptDTO;
import com.fly.platform.asset.dto.UavCreateUpdateDTO;
import com.fly.platform.asset.dto.UavStateChangeDTO;
import com.fly.platform.asset.entity.UavInfo;
import com.fly.platform.asset.entity.UavMaintain;
import com.fly.platform.asset.service.UavService;
import com.fly.platform.asset.vo.UavDetailVO;
import com.fly.platform.common.api.Result;
import com.fly.platform.common.audit.Audit;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 无人机资产台账与五态管理控制器
 *
 * @author SoarCloud Team
 */
@RestController
@RequestMapping("/api/v1/uav")
@RequiredArgsConstructor
public class UavController {

    private final UavService uavService;

    @GetMapping("/list")
    public Result<List<UavDetailVO>> listUavs(
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) Integer state) {
        return Result.ok(uavService.listUavs(merchantId, model, state));
    }

    @GetMapping("/{id}")
    public Result<UavDetailVO> getUavDetail(@PathVariable("id") Long id) {
        return Result.ok(uavService.getUavDetail(id));
    }

    @PostMapping
    @Audit(module = "机队资产", action = "登记新增无人机")
    public Result<UavInfo> createUav(@Valid @RequestBody UavCreateUpdateDTO dto) {
        return Result.ok("无人机设备资产登记成功", uavService.createUav(dto));
    }

    @PutMapping("/{id}")
    @Audit(module = "机队资产", action = "修订无人机档案")
    public Result<UavInfo> updateUav(@PathVariable("id") Long id, @Valid @RequestBody UavCreateUpdateDTO dto) {
        return Result.ok("无人机资产档案修订成功", uavService.updateUav(id, dto));
    }

    @PutMapping("/{id}/state")
    @Audit(module = "机队资产", action = "驱动设备五态流转")
    public Result<Void> changeUavState(@PathVariable("id") Long id, @Valid @RequestBody UavStateChangeDTO dto) {
        uavService.changeUavState(id, dto.getTargetState(), dto.getRemark());
        return Result.ok("设备运行状态流转成功", null);
    }

    @PostMapping("/fault")
    @Audit(module = "维保管理", action = "外场现场提报故障")
    public Result<UavMaintain> reportFault(@Valid @RequestBody FaultReportDTO dto) {
        UavMaintain maintain = uavService.reportFault(dto);
        return Result.ok("故障工单已受理，设备已自动切换为故障待修状态", maintain);
    }

    @GetMapping("/maintain/list")
    public Result<List<UavMaintain>> listMaintains(@RequestParam(required = false) Long uavId) {
        return Result.ok(uavService.listMaintains(uavId));
    }

    @PutMapping("/maintain/{maintainId}/accept")
    @Audit(module = "维保管理", action = "维保工单验收合格")
    public Result<Void> acceptMaintain(@PathVariable("maintainId") Long maintainId,
                                      @Valid @RequestBody MaintainAcceptDTO dto) {
        uavService.acceptMaintain(maintainId, dto);
        return Result.ok("工单验收合格，关联无人机已恢复闲置就绪", null);
    }
}
