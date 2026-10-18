package com.fly.platform.trace.controller;

import com.fly.platform.common.api.Result;
import com.fly.platform.trace.dto.TraceBatchDTO;
import com.fly.platform.trace.service.FlyTraceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 飞行航迹实时上报与监控控制器
 *
 * @author SoarCloud Team
 */
@RestController
@RequestMapping("/api/v1/trace")
@RequiredArgsConstructor
public class TraceController {

    private final FlyTraceService flyTraceService;

    /**
     * 机长/飞控终端批量上报瞬时航迹数据
     */
    @PostMapping("/batch")
    public Result<Integer> batchReport(@Valid @RequestBody TraceBatchDTO dto) {
        int count = flyTraceService.batchReport(dto);
        return Result.ok("成功持久化 " + count + " 条航迹点", count);
    }
}
