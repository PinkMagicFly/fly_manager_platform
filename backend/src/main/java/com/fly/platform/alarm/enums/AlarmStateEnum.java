package com.fly.platform.alarm.enums;

import lombok.Getter;

/**
 * 告警处置状态枚举
 *
 * @author SoarCloud Team
 */
@Getter
public enum AlarmStateEnum {

    PENDING(0, "未处置"),
    PROCESSING(1, "处置中"),
    CLOSED(2, "已关闭");

    private final Integer code;
    private final String desc;

    AlarmStateEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(Integer code) {
        if (code == null) return "未知状态";
        for (AlarmStateEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getDesc();
            }
        }
        return "未知状态";
    }
}
