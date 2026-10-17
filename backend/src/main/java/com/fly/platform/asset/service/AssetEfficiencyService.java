package com.fly.platform.asset.service;

import com.fly.platform.asset.vo.AssetEfficiencyVO;

/**
 * 资产效能分析大盘服务接口
 *
 * @author SoarCloud Team
 */
public interface AssetEfficiencyService {

    /** 获取机队资产效能分析综合指标大盘 */
    AssetEfficiencyVO getEfficiencyOverview(Long merchantId);
}
