package com.fly.platform.asset.domain;

/**
 * 无人机健康综合评分动态计算模型
 * 依据累计执飞时长、历史故障频次、逾期维保天数与电池状态综合推演 (0-100分)
 *
 * @author SoarCloud Team
 */
public class UavHealthScoreCalculator {

    /**
     * 计算设备健康评分
     *
     * @param flyHours 累计执飞时长（小时）
     * @param faultCount 历史故障次数
     * @param overdueDays 逾期维保天数 (未逾期传0)
     * @param batteryPct 当前电池电量 (0-100)
     * @return 综合健康分值 (0-100)
     */
    public static int calculate(int flyHours, int faultCount, int overdueDays, int batteryPct) {
        int score = 100;

        // 1. 飞行小时数损耗：每 100 小时扣 2 分，上限扣 20 分
        if (flyHours > 0) {
            int hourDeduct = Math.min(20, (flyHours / 100) * 2);
            score -= hourDeduct;
        }

        // 2. 故障扣分：每发生一次故障扣 5 分
        if (faultCount > 0) {
            score -= faultCount * 5;
        }

        // 3. 逾期维保扣分：每天逾期扣 2 分，上限扣 30 分
        if (overdueDays > 0) {
            int overdueDeduct = Math.min(30, overdueDays * 2);
            score -= overdueDeduct;
        }

        // 4. 深度亏电扣分：若电量低于 20%，额外扣 5 分
        if (batteryPct < 20) {
            score -= 5;
        }

        // 边界保护：分值严格限定在 0 到 100 之间
        if (score < 0) {
            score = 0;
        } else if (score > 100) {
            score = 100;
        }

        return score;
    }
}
