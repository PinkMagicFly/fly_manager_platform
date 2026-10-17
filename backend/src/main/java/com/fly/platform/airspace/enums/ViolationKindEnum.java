package com.fly.platform.airspace.enums;

import lombok.Getter;

/**
 * 飞行违规行为类别枚举
 * 1-航线严重偏航 2-非法入侵禁飞区 3-超越法定限高 4-超时占用空域
 *
 * @author SoarCloud Team
 */
@Getter
public enum ViolationKindEnum {

    DEVIATION(1, "航线严重偏航"),
    INTRUSION(2, "非法入侵禁飞区"),
    OVER_HEIGHT(3, "超越法定限高"),
    OVER_TIME(4, "超时占用空域");

    private final Integer code;
    private final String desc;

    ViolationKindEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(Integer code) {
        if (code == null) return "未知违规";
        for (ViolationKindEnum kind : values()) {
            if (kind.code.equals(code)) {
                return kind.desc;
            }
        }
        return "未知违规";
    }
}
