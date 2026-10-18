package com.fly.platform.alarm.enums;

import lombok.Getter;

/**
 * 告警级别枚举定义
 *
 * @author SoarCloud Team
 */
@Getter
public enum AlarmLevelEnum {

    NORMAL(1, "一般"),
    SERIOUS(2, "严重"),
    URGENT(3, "紧急");

    private final Integer code;
    private final String desc;

    AlarmLevelEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(Integer code) {
        if (code == null) return "未知级别";
        for (AlarmLevelEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getDesc();
            }
        }
        return "未知级别";
    }
}
