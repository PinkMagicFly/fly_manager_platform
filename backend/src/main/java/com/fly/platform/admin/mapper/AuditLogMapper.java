package com.fly.platform.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fly.platform.admin.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作审计日志持久层 Mapper
 *
 * @author SoarCloud Team
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
