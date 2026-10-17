package com.fly.platform.asset;

import com.fly.platform.asset.domain.UavHealthScoreCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 单元测试: 无人机健康综合评分模型计算与边界守护
 *
 * @author SoarCloud Team
 */
@DisplayName("资产中心: 无人机健康综合评分动态计算模型测试")
public class UavHealthScoreCalculatorTest {

    @Test
    @DisplayName("测试全新出厂设备默认满分 100 分")
    public void testBrandNewUavScore() {
        int score = UavHealthScoreCalculator.calculate(0, 0, 0, 100);
        assertThat(score).isEqualTo(100);
    }

    @Test
    @DisplayName("测试飞行时长扣分与故障扣分叠加计算")
    public void testFlyHoursAndFaultDeduction() {
        // 飞行 250 小时扣 4 分，发生 2 次故障扣 10 分 => 100 - 4 - 10 = 86
        int score = UavHealthScoreCalculator.calculate(250, 2, 0, 90);
        assertThat(score).isEqualTo(86);
    }

    @Test
    @DisplayName("测试逾期维保与亏电扣分")
    public void testOverdueMaintainAndLowBatteryDeduction() {
        // 飞行 100 小时扣 2 分，逾期 5 天扣 10 分，电量 15% 额外扣 5 分 => 100 - 2 - 10 - 5 = 83
        int score = UavHealthScoreCalculator.calculate(100, 0, 5, 15);
        assertThat(score).isEqualTo(83);
    }

    @Test
    @DisplayName("测试极端工况下健康度分值下限边界 (0~100分)")
    public void testScoreBoundary() {
        // 极端严重工况：飞行 3000小时，故障 50次，逾期 100天，亏电
        int score = UavHealthScoreCalculator.calculate(3000, 50, 100, 5);
        assertThat(score).isGreaterThanOrEqualTo(0);
        assertThat(score).isEqualTo(0);
    }
}
