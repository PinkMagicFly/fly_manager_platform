package com.fly.platform.asset.enums;

import lombok.Getter;

/**
 * 场站运行状态枚举
 *
 * @author SoarCloud Team
 */
@Getter
public enum SiteStateEnum {

    NORMAL(1, "正常运营"),
    SUSPENDED(2, "临时停运"),
    MAINTAINING(3, "设备维护");

    private final int code;
    private final String description;

    SiteStateEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
