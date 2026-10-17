package com.fly.platform.airspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fly.platform.airspace.entity.FenceRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 电子围栏合规校验规则持久层 Mapper
 *
 * @author SoarCloud Team
 */
@Mapper
public interface FenceRuleMapper extends BaseMapper<FenceRule> {

    /** 查询所有启用的电子围栏规则 */
    @Select("SELECT * FROM fence_rule WHERE is_deleted = 0 ORDER BY rule_id ASC")
    List<FenceRule> selectAllRules();
}
