package com.fly.platform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fly.platform.auth.entity.UamAccountRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账户角色关联 Mapper
 *
 * @author SoarCloud Team
 */
@Mapper
public interface UamAccountRoleMapper extends BaseMapper<UamAccountRole> {
}
