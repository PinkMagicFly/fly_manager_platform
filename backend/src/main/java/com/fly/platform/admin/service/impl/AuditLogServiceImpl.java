package com.fly.platform.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fly.platform.admin.entity.AuditLog;
import com.fly.platform.admin.mapper.AuditLogMapper;
import com.fly.platform.admin.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;

    @Override
    public List<AuditLog> listLogs(String moduleName, String loginName, Integer limit) {
        LambdaQueryWrapper<AuditLog> qw = new LambdaQueryWrapper<>();
        if (moduleName != null && !moduleName.isBlank()) {
            qw.like(AuditLog::getModuleName, moduleName);
        }
        if (loginName != null && !loginName.isBlank()) {
            qw.like(AuditLog::getLoginName, loginName);
        }
        qw.orderByDesc(AuditLog::getCreatedAt);
        qw.last("LIMIT " + (limit != null && limit > 0 ? limit : 50));
        return auditLogMapper.selectList(qw);
    }
}
