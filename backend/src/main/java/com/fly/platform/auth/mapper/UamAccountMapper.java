package com.fly.platform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fly.platform.auth.entity.UamAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * 统一账户持久层 Mapper
 *
 * @author SoarCloud Team
 */
@Mapper
public interface UamAccountMapper extends BaseMapper<UamAccount> {
}
