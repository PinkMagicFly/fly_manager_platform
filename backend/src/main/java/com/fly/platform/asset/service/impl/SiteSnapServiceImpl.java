package com.fly.platform.asset.service.impl;

import com.fly.platform.asset.entity.BaseSite;
import com.fly.platform.asset.service.BaseSiteService;
import com.fly.platform.asset.service.SiteSnapService;
import com.fly.platform.asset.vo.SiteSnapVO;
import com.fly.platform.common.exception.BusinessException;
import com.fly.platform.common.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 起降场站就近吸附与服务范围判别服务实现类
 *
 * @author SoarCloud Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SiteSnapServiceImpl implements SiteSnapService {

    private final BaseSiteService baseSiteService;

    @Override
    public BaseSite snapSite(double lng, double lat) {
        List<BaseSite> sites = baseSiteService.getSiteList();
        if (sites == null || sites.isEmpty()) {
            throw new BusinessException(400, "寄件/收件地址不在任何场站服务范围内");
        }

        BaseSite bestSite = null;
        double minDistance = Double.MAX_VALUE;

        for (BaseSite site : sites) {
            Double siteLng = site.getLongitude();
            Double siteLat = site.getLatitude();
            if (siteLng == null || siteLat == null) {
                continue;
            }

            double dist = GeoUtils.haversineMeters(lng, lat, siteLng, siteLat);
            double radius = site.getServiceRadius() != null ? site.getServiceRadius() : BaseSite.DEFAULT_SERVICE_RADIUS_METERS;

            if (dist <= radius && dist < minDistance) {
                minDistance = dist;
                bestSite = site;
            }
        }

        if (bestSite == null) {
            log.warn("坐标 [{}, {}] 未能吸附到任何服务范围内的起降场站", lng, lat);
            throw new BusinessException(400, "寄件/收件地址不在任何场站服务范围内");
        }

        return bestSite;
    }

    @Override
    public SiteSnapVO snapSiteWithDistance(double lng, double lat) {
        BaseSite site = snapSite(lng, lat);
        double dist = GeoUtils.haversineMeters(lng, lat, site.getLongitude(), site.getLatitude());

        SiteSnapVO vo = new SiteSnapVO();
        vo.setSiteId(site.getSiteId());
        vo.setSiteCode(site.getSiteCode());
        vo.setSiteName(site.getSiteName());
        vo.setLongitude(site.getLongitude());
        vo.setLatitude(site.getLatitude());
        vo.setCapacity(site.getCapacity());
        vo.setOccupied(site.getOccupied());
        vo.setSiteState(site.getSiteState());
        vo.setServiceRadius(site.getServiceRadius());
        vo.setDistanceMeters(BigDecimal.valueOf(dist).setScale(1, RoundingMode.HALF_UP).doubleValue());

        return vo;
    }
}
