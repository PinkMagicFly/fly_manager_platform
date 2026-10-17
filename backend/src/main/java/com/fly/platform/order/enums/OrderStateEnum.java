package com.fly.platform.order.enums;

import lombok.Getter;

/**
 * 订单九态流转枚举定义
 *
 * @author SoarCloud Team
 */
@Getter
public enum OrderStateEnum {

    PENDING_PAY(10, "待支付"),
    PENDING_ACCEPT(20, "待接单"),
    PENDING_DISPATCH(30, "待调度"),
    PENDING_TAKEOFF(40, "待起飞"),
    FLYING(50, "飞行中"),
    DELIVERED(60, "已送达"),
    CANCELLED(70, "已取消"),
    ABORTED(80, "异常中断");

    private final Integer code;
    private final String description;

    OrderStateEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static OrderStateEnum fromCode(Integer code) {
        if (code == null) return null;
        for (OrderStateEnum state : values()) {
            if (state.getCode().equals(code)) {
                return state;
            }
        }
        return null;
    }

    public static String getDescByCode(Integer code) {
        OrderStateEnum state = fromCode(code);
        return state != null ? state.getDescription() : "未知状态";
    }
}
