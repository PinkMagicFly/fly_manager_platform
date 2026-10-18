package com.fly.platform.sysparam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fly.platform.sysparam.entity.SysParam;
import org.apache.ibatis.annotations.Mapper;

/**
 * 平台运行参数持久化 Mapper
 *
 * @author SoarCloud Team
 */
@Mapper
public interface SysParamMapper extends BaseMapper<SysParam> {
}
