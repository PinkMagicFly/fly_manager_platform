package com.fly.platform.order.enums;

import lombok.Getter;

/**
 * 订单支付状态枚举
 *
 * @author SoarCloud Team
 */
@Getter
public enum PayStateEnum {

    UNPAID(0, "未支付"),
    PAID(1, "已支付"),
    REFUNDED(2, "已退款");

    private final Integer code;
    private final String description;

    PayStateEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static PayStateEnum fromCode(Integer code) {
        if (code == null) return null;
        for (PayStateEnum state : values()) {
            if (state.getCode().equals(code)) {
                return state;
            }
        }
        return null;
    }
}
