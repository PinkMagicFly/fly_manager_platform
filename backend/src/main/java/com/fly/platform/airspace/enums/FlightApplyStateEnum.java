package com.fly.platform.airspace.enums;

import lombok.Getter;

/**
 * 飞行计划申报审批六态枚举
 * 0-待审批 1-已批准 2-已驳回 3-已取消 4-执行中 5-已完成
 *
 * @author SoarCloud Team
 */
@Getter
public enum FlightApplyStateEnum {

    PENDING(0, "待审批"),
    APPROVED(1, "已批准"),
    REJECTED(2, "已驳回"),
    CANCELED(3, "已取消"),
    EXECUTING(4, "执行中"),
    COMPLETED(5, "已完成");

    private final Integer code;
    private final String desc;

    FlightApplyStateEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(Integer code) {
        if (code == null) return "未知状态";
        for (FlightApplyStateEnum state : values()) {
            if (state.code.equals(code)) {
                return state.desc;
            }
        }
        return "未知状态";
    }

    /**
     * 判断是否为任务终态（已驳回/已取消/已完成）
     * 终态任务不再占用装备时段，且禁止再次起飞或流转
     */
    public static boolean isTerminal(Integer state) {
        if (state == null) return false;
        return state.equals(REJECTED.code)
                || state.equals(CANCELED.code)
                || state.equals(COMPLETED.code);
    }
}
