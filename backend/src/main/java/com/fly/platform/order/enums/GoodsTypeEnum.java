package com.fly.platform.order.enums;

import lombok.Getter;

/**
 * 低空运载货物类别枚举
 *
 * @author SoarCloud Team
 */
@Getter
public enum GoodsTypeEnum {

    GENERAL_DOC(1, "普通文件与档案"),
    BIOMEDICAL(2, "生物医疗与样本急救"),
    FRESH_DIGITAL(3, "数码高科与生鲜"),
    HIGH_VALUE_INDUSTRIAL(4, "高附加值精密工业件");

    private final Integer code;
    private final String description;

    GoodsTypeEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static GoodsTypeEnum fromCode(Integer code) {
        if (code == null) return null;
        for (GoodsTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    public static String getDescByCode(Integer code) {
        GoodsTypeEnum type = fromCode(code);
        return type != null ? type.getDescription() : "未知类别";
    }
}
