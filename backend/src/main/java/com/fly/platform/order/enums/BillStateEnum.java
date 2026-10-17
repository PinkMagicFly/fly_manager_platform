package com.fly.platform.order.enums;

import lombok.Getter;

/**
 * 财务结算账单状态枚举
 *
 * @author SoarCloud Team
 */
@Getter
public enum BillStateEnum {

    UNISSUED(0, "待出账"),
    PENDING_RECONCILE(1, "待对账"),
    RECONCILED(2, "已对账"),
    INVOICED(3, "已开票");

    private final Integer code;
    private final String description;

    BillStateEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static BillStateEnum fromCode(Integer code) {
        if (code == null) return null;
        for (BillStateEnum state : values()) {
            if (state.getCode().equals(code)) {
                return state;
            }
        }
        return null;
    }

    public static String getDescByCode(Integer code) {
        BillStateEnum state = fromCode(code);
        return state != null ? state.getDescription() : "未知状态";
    }
}
