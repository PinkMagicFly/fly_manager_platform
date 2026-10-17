package com.fly.platform.order.enums;

import lombok.Getter;

/**
 * 订单支付方式枚举
 *
 * @author SoarCloud Team
 */
@Getter
public enum PayWayEnum {

    ONLINE(1, "在线支付（微信/支付宝）"),
    MONTHLY(2, "企业月结授信扣款");

    private final Integer code;
    private final String description;

    PayWayEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static PayWayEnum fromCode(Integer code) {
        if (code == null) return null;
        for (PayWayEnum way : values()) {
            if (way.getCode().equals(code)) {
                return way;
            }
        }
        return null;
    }
}
