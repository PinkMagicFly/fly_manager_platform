package com.fly.platform.ticket.enums;

import lombok.Getter;

/**
 * 客服工单类别枚举
 *
 * @author SoarCloud Team
 */
@Getter
public enum TicketTypeEnum {

    CONSULT(1, "业务咨询"),
    COMPLAINT(2, "时效投诉"),
    EXCEPTION(3, "异常报修");

    private final Integer code;
    private final String desc;

    TicketTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(Integer code) {
        if (code == null) return "未知类型";
        for (TicketTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getDesc();
            }
        }
        return "未知类型";
    }
}
