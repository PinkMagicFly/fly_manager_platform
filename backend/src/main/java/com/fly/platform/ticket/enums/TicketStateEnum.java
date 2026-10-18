package com.fly.platform.ticket.enums;

import lombok.Getter;

/**
 * 客服工单流转状态枚举
 *
 * @author SoarCloud Team
 */
@Getter
public enum TicketStateEnum {

    PENDING(0, "待处理"),
    PROCESSING(1, "处理中"),
    CLOSED(2, "已关闭");

    private final Integer code;
    private final String desc;

    TicketStateEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(Integer code) {
        if (code == null) return "未知状态";
        for (TicketStateEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getDesc();
            }
        }
        return "未知状态";
    }
}
