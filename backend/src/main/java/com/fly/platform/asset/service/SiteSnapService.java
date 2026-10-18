package com.fly.platform.asset.service;

import com.fly.platform.asset.entity.BaseSite;
import com.fly.platform.asset.vo.SiteSnapVO;

/**
 * 起降场站就近吸附与服务范围判别服务
 *
 * @author SoarCloud Team
 */
public interface SiteSnapService {

    /**
     * 根据给定的经纬度坐标，就近吸附匹配最近的服务范围内场站
     * 若超出所有场站的服务覆盖半径，抛出 BusinessException(400)
     *
     * @param lng 目标经度
     * @param lat 目标纬度
     * @return 距离最近的匹配起降场站实体
     */
    BaseSite snapSite(double lng, double lat);

    /**
     * 根据给定的经纬度坐标，就近吸附匹配最近的场站并计算具体距离
     *
     * @param lng 目标经度
     * @param lat 目标纬度
     * @return 场站吸附详情视图对象（含场站信息与距离米）
     */
    SiteSnapVO snapSiteWithDistance(double lng, double lat);
}
