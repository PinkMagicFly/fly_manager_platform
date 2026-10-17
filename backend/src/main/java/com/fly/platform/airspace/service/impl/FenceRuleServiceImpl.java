package com.fly.platform.airspace.service.impl;

import com.fly.platform.airspace.dto.FenceRuleDTO;
import com.fly.platform.airspace.entity.FenceRule;
import com.fly.platform.airspace.mapper.FenceRuleMapper;
import com.fly.platform.airspace.service.FenceRuleService;
import com.fly.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 电子围栏校验规则服务实现类
 *
 * @author SoarCloud Team
 */
@Service
@RequiredArgsConstructor
public class FenceRuleServiceImpl implements FenceRuleService {

    private final FenceRuleMapper fenceRuleMapper;

    @Override
    public List<FenceRule> getRuleList() {
        return fenceRuleMapper.selectAllRules();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FenceRule updateRule(FenceRuleDTO dto) {
        FenceRule rule = fenceRuleMapper.selectById(dto.getRuleId());
        if (rule == null) {
            throw new BusinessException(404, "电子围栏规则不存在");
        }

        rule.setRuleName(dto.getRuleName());
        rule.setBufferDistance(dto.getBufferDistance());
        rule.setMaxAltitude(dto.getMaxAltitude());
        rule.setActionPolicy(dto.getActionPolicy());

        fenceRuleMapper.updateById(rule);
        return rule;
    }
}
