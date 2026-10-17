package com.fly.platform.asset.enums;

import lombok.Getter;

/**
 * 场站机位预约状态枚举
 *
 * @author SoarCloud Team
 */
@Getter
public enum BookingStateEnum {

    BOOKED(1, "已预约"),
    OCCUPIED(2, "已占用"),
    RELEASED(3, "已释放"),
    CANCELLED(4, "已取消");

    private final int code;
    private final String description;

    BookingStateEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
