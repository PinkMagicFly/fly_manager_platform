package com.fly.platform.asset.enums;

import lombok.Getter;

/**
 * 无人机设备五态模型枚举
 *
 * @author SoarCloud Team
 */
@Getter
public enum UavStateEnum {

    IDLE(0, "闲置"),
    ON_TASK(1, "任务中"),
    MAINTAINING(2, "维保中"),
    FAULT(3, "故障"),
    OFFLINE(4, "离线");

    private final int code;
    private final String description;

    UavStateEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static UavStateEnum fromCode(Integer code) {
        if (code == null) return null;
        for (UavStateEnum state : values()) {
            if (state.getCode() == code) return state;
        }
        return null;
    }

    public static String getDescByCode(Integer code) {
        UavStateEnum state = fromCode(code);
        return state != null ? state.getDescription() : "未知状态";
    }
}
