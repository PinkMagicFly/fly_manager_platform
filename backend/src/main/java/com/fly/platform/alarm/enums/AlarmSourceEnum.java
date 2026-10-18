package com.fly.platform.alarm.enums;

import lombok.Getter;

/**
 * 告警事件来源枚举
 *
 * @author SoarCloud Team
 */
@Getter
public enum AlarmSourceEnum {

    FAULT_REPORT(1, "故障上报"),
    MANUAL_RECORD(2, "手工登记");

    private final Integer code;
    private final String desc;

    AlarmSourceEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(Integer code) {
        if (code == null) return "未知来源";
        for (AlarmSourceEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getDesc();
            }
        }
        return "未知来源";
    }
}
