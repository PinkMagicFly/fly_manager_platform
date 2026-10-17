package com.fly.platform.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.fly.platform.admin.entity.AuditLog;
import com.fly.platform.admin.service.AuditLogService;
import com.fly.platform.common.api.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 平台治理与操作审计接口
 *
 * @author SoarCloud Team
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping("/audit-logs")
    @SaCheckRole("ROLE_ADMIN")
    public Result<List<AuditLog>> listAuditLogs(
            @RequestParam(required = false) String moduleName,
            @RequestParam(required = false) String loginName,
            @RequestParam(required = false, defaultValue = "50") Integer limit) {
        return Result.ok(auditLogService.listLogs(moduleName, loginName, limit));
    }
}

