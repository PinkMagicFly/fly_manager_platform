package com.fly.platform.airspace.enums;

import lombok.Getter;

/**
 * 空域管制区块类别枚举
 *
 * @author SoarCloud Team
 */
@Getter
public enum AirspaceKindEnum {

    PERMANENT_FORBIDDEN(1, "永久禁飞区"),
    DYNAMIC_LIMITED(2, "动态限飞区"),
    TEMPORARY_CONTROL(3, "临时管制区");

    private final Integer code;
    private final String desc;

    AirspaceKindEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(Integer code) {
        if (code == null) return "未知空域";
        for (AirspaceKindEnum kind : values()) {
            if (kind.code.equals(code)) {
                return kind.desc;
            }
        }
        return "未知空域";
    }
}
