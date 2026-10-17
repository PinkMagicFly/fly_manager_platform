package com.fly.platform.asset.service.impl;

import com.fly.platform.asset.entity.BaseSite;
import com.fly.platform.asset.mapper.BaseSiteMapper;
import com.fly.platform.asset.service.BaseSiteService;
import com.fly.platform.asset.vo.SiteMapVO;
import com.fly.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 起降场站与智能换电设施服务实现类
 *
 * @author SoarCloud Team
 */
@Service
@RequiredArgsConstructor
public class BaseSiteServiceImpl implements BaseSiteService {

    private final BaseSiteMapper baseSiteMapper;

    @Override
    public List<SiteMapVO> getSiteMapList() {
        List<BaseSite> sites = baseSiteMapper.selectAllActiveSites();
        List<SiteMapVO> result = new ArrayList<>();
        for (BaseSite site : sites) {
            SiteMapVO vo = new SiteMapVO();
            vo.setSiteId(site.getSiteId());
            vo.setSiteCode(site.getSiteCode());
            vo.setSiteName(site.getSiteName());
            vo.setLongitude(site.getLongitude());
            vo.setLatitude(site.getLatitude());
            vo.setCapacity(site.getCapacity());
            vo.setOccupied(site.getOccupied());
            vo.setAvailable(Math.max(0, site.getCapacity() - site.getOccupied()));
            vo.setSiteState(site.getSiteState());

            if (site.getSiteState() == 1) {
                vo.setSiteStateDesc("正常运营");
            } else if (site.getSiteState() == 2) {
                vo.setSiteStateDesc("临时停运");
            } else {
                vo.setSiteStateDesc("设备维护");
            }

            // 智能充电机柜模拟储备状态
            vo.setBatteryStock(site.getCapacity() * 2);
            vo.setChargingCount(site.getOccupied());
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<BaseSite> getSiteList() {
        return baseSiteMapper.selectAllActiveSites();
    }

    @Override
    public BaseSite getSiteDetail(Long siteId) {
        BaseSite site = baseSiteMapper.selectById(siteId);
        if (site == null) {
            throw new BusinessException(404, "场站不存在");
        }
        return site;
    }
}
