package com.fly.platform.airspace.service;

import com.fly.platform.airspace.dto.FenceRuleDTO;
import com.fly.platform.airspace.entity.FenceRule;

import java.util.List;

/**
 * 电子围栏校验规则服务接口
 *
 * @author SoarCloud Team
 */
public interface FenceRuleService {

    /** 查询全量电子围栏校验规则 */
    List<FenceRule> getRuleList();

    /** 更新电子围栏规则参数 */
    FenceRule updateRule(FenceRuleDTO dto);
}
