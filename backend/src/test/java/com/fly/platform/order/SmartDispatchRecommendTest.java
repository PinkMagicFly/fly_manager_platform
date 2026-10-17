package com.fly.platform.order;

import com.fly.platform.asset.entity.UavInfo;
import com.fly.platform.asset.enums.UavStateEnum;
import com.fly.platform.order.service.SmartDispatchRecommendService;
import com.fly.platform.order.vo.SmartDispatchCandidateVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 智能调度推荐算法排他性优选决策测试
 *
 * @author SoarCloud Team
 */
public class SmartDispatchRecommendTest {

    private final SmartDispatchRecommendService recommendService = new SmartDispatchRecommendService(null, null, null);

    @Test
    @DisplayName("智能调度排他性推荐测试: 15kg订单，A(10kg)、B(25kg,15%电量)、C(20kg,90%电量)，排他性推荐C")
    void testExclusiveRecommendation() {
        // 无人机 A：最大载重 10kg（不足 15kg），电量 95%
        UavInfo uavA = new UavInfo();
        uavA.setUavId(101L);
        uavA.setUavSn("UAV-A-101");
        uavA.setModel("轻型配送机-A");
        uavA.setMaxLoad(new BigDecimal("10.00"));
        uavA.setMaxRangeKm(new BigDecimal("25.0"));
        uavA.setBatteryPct(95);
        uavA.setHealthScore(98);
        uavA.setState(UavStateEnum.IDLE.getCode());

        // 无人机 B：最大载重 25kg，但电量仅 15%（低于 30% 安全红线）
        UavInfo uavB = new UavInfo();
        uavB.setUavId(102L);
        uavB.setUavSn("UAV-B-102");
        uavB.setModel("重型运输机-B");
        uavB.setMaxLoad(new BigDecimal("25.00"));
        uavB.setMaxRangeKm(new BigDecimal("40.0"));
        uavB.setBatteryPct(15);
        uavB.setHealthScore(90);
        uavB.setState(UavStateEnum.IDLE.getCode());

        // 无人机 C：最大载重 20kg，电量 90%，健康度 95分（最佳匹配）
        UavInfo uavC = new UavInfo();
        uavC.setUavId(103L);
        uavC.setUavSn("UAV-C-103");
        uavC.setModel("中型主力机-C");
        uavC.setMaxLoad(new BigDecimal("20.00"));
        uavC.setMaxRangeKm(new BigDecimal("35.0"));
        uavC.setBatteryPct(90);
        uavC.setHealthScore(95);
        uavC.setState(UavStateEnum.IDLE.getCode());

        List<UavInfo> candidates = Arrays.asList(uavA, uavB, uavC);
        BigDecimal orderWeight = new BigDecimal("15.00");
        BigDecimal distanceKm = new BigDecimal("8.50");

        List<SmartDispatchCandidateVO> results = recommendService.evaluateUavs(candidates, orderWeight, distanceKm);

        assertThat(results).hasSize(3);

        // A 超载被排除 (isRecommended = false, score = 0)
        SmartDispatchCandidateVO resA = results.stream().filter(r -> r.getUavId().equals(101L)).findFirst().orElseThrow();
        assertThat(resA.getIsRecommended()).isFalse();
        assertThat(resA.getScore()).isEqualTo(0);
        assertThat(resA.getMatchReason()).contains("超重不适配");

        // B 低电量被排除 (isRecommended = false, score = 0)
        SmartDispatchCandidateVO resB = results.stream().filter(r -> r.getUavId().equals(102L)).findFirst().orElseThrow();
        assertThat(resB.getIsRecommended()).isFalse();
        assertThat(resB.getScore()).isEqualTo(0);
        assertThat(resB.getMatchReason()).contains("低电量红线拦截");

        // C 排他性当选唯一推荐设备
        SmartDispatchCandidateVO resC = results.stream().filter(r -> r.getUavId().equals(103L)).findFirst().orElseThrow();
        assertThat(resC.getIsRecommended()).isTrue();
        assertThat(resC.getScore()).isGreaterThan(70);
        assertThat(resC.getMatchReason()).contains("综合匹配优异");
    }
}
