package com.fly.platform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fly.platform.auth.entity.UamRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色持久层 Mapper
 *
 * @author SoarCloud Team
 */
@Mapper
public interface UamRoleMapper extends BaseMapper<UamRole> {
}
