package com.fly.platform.asset;

import com.fly.platform.asset.entity.BaseSite;
import com.fly.platform.asset.service.SiteSnapService;
import com.fly.platform.asset.vo.SiteSnapVO;
import com.fly.platform.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("集成测试: 起降场站就近吸附与服务范围判别服务")
public class SiteSnapServiceTest {

    @Autowired
    private SiteSnapService siteSnapService;

    @Test
    @DisplayName("测试用例1：就近吸附匹配正确 - 目标在南山科技园周边300米，成功吸附至 SITE-SZ-001")
    public void testSnapNearNanshan() {
        // 目标经纬度：南山科技园附近
        double testLng = 113.945;
        double testLat = 22.542;

        BaseSite site = siteSnapService.snapSite(testLng, testLat);
        assertThat(site).isNotNull();
        assertThat(site.getSiteCode()).isEqualTo("SITE-SZ-001");
        assertThat(site.getSiteName()).contains("南山科技园");

        SiteSnapVO snapVO = siteSnapService.snapSiteWithDistance(testLng, testLat);
        assertThat(snapVO).isNotNull();
        assertThat(snapVO.getSiteCode()).isEqualTo("SITE-SZ-001");
        assertThat(snapVO.getDistanceMeters()).isLessThan(500.0);
        assertThat(snapVO.getServiceRadius()).isEqualTo(5000.0);
    }

    @Test
    @DisplayName("测试用例2：服务覆盖半径外异常拦截 - 超出5000米范围抛出 400 业务异常")
    public void testSnapOutsideRadiusThrowsException() {
        // 偏远坐标（远离深圳核心场站 100km+）
        double farLng = 113.000;
        double farLat = 23.500;

        assertThatThrownBy(() -> siteSnapService.snapSite(farLng, farLat))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不在任何场站服务范围内");
    }

    @Test
    @DisplayName("测试用例3：多场站空间竞争时严格取距离最近者 - 离福田近南山远，断言吸附至 SITE-SZ-002")
    public void testSnapMultiSitesPicksClosest() {
        // 福田深业上城西侧约1公里 (114.050, 22.550)
        double nearFutianLng = 114.050;
        double nearFutianLat = 22.550;

        BaseSite site = siteSnapService.snapSite(nearFutianLng, nearFutianLat);
        assertThat(site).isNotNull();
        assertThat(site.getSiteCode()).isEqualTo("SITE-SZ-002");
        assertThat(site.getSiteName()).contains("福田");
    }
}
