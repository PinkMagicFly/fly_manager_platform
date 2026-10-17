package com.fly.platform.asset.enums;

import lombok.Getter;

/**
 * 无人机维保工单流转状态枚举
 *
 * @author SoarCloud Team
 */
@Getter
public enum MaintainStateEnum {

    PENDING(0, "待维保"),
    IN_PROGRESS(1, "维保中"),
    PASSED(2, "已验收合格");

    private final int code;
    private final String description;

    MaintainStateEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
