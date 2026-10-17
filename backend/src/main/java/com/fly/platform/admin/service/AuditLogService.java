package com.fly.platform.admin.service;

import com.fly.platform.admin.entity.AuditLog;
import java.util.List;

public interface AuditLogService {
    List<AuditLog> listLogs(String moduleName, String loginName, Integer limit);
}
