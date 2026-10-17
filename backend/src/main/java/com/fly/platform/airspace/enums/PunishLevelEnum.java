package com.fly.platform.airspace.enums;

import lombok.Getter;

/**
 * 行政处罚分级枚举
 * 1-警告留痕 2-企业罚款 3-停飞整顿 4-吊销资质
 *
 * @author SoarCloud Team
 */
@Getter
public enum PunishLevelEnum {

    WARNING(1, "警告留痕"),
    FINE(2, "企业罚款"),
    SUSPEND(3, "停飞整顿"),
    REVOKE(4, "吊销资质");

    private final Integer code;
    private final String desc;

    PunishLevelEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(Integer code) {
        if (code == null) return "待定责";
        for (PunishLevelEnum level : values()) {
            if (level.code.equals(code)) {
                return level.desc;
            }
        }
        return "待定责";
    }
}
