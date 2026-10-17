package com.fly.platform.asset.service;

import com.fly.platform.asset.entity.BaseSite;
import com.fly.platform.asset.vo.SiteMapVO;

import java.util.List;

/**
 * 起降场站与智能换电设施服务接口
 *
 * @author SoarCloud Team
 */
public interface BaseSiteService {

    /** 获取场站一张图聚合视图列表 */
    List<SiteMapVO> getSiteMapList();

    /** 获取所有有效场站实体列表 */
    List<BaseSite> getSiteList();

    /** 获取场站详情 */
    BaseSite getSiteDetail(Long siteId);
}
