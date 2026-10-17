package com.fly.platform.airspace.enums;

import lombok.Getter;

/**
 * 飞行计划申报审批四态枚举
 * 0-待审批 1-已通过 2-已驳回 3-已撤销
 *
 * @author SoarCloud Team
 */
@Getter
public enum FlightApplyStateEnum {

    PENDING(0, "待审批"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已驳回"),
    CANCELED(3, "已撤销");

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
}
