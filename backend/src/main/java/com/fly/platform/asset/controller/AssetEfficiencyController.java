package com.fly.platform.asset.controller;

import com.fly.platform.asset.service.AssetEfficiencyService;
import com.fly.platform.asset.vo.AssetEfficiencyVO;
import com.fly.platform.common.api.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 机队资产效能分析大盘控制器
 *
 * @author SoarCloud Team
 */
@RestController
@RequestMapping("/api/v1/asset/efficiency")
@RequiredArgsConstructor
public class AssetEfficiencyController {

    private final AssetEfficiencyService assetEfficiencyService;

    @GetMapping("/overview")
    public Result<AssetEfficiencyVO> getEfficiencyOverview(
            @RequestParam(required = false) Long merchantId) {
        return Result.ok(assetEfficiencyService.getEfficiencyOverview(merchantId));
    }
}
