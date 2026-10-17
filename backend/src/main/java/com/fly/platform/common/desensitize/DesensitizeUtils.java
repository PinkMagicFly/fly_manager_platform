package com.fly.platform.common.desensitize;

/**
 * 敏感数据脱敏工具类（手机号、身份证、姓名）
 *
 * @author SoarCloud Team
 */
public class DesensitizeUtils {

    /**
     * 手机号脱敏：前三后四，中间四位为星号 (如 138****0000)
     */
    public static String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 11) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }

    /**
     * 身份证脱敏：前六后四 (如 440301********1234)
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) {
            return idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }

    /**
     * 姓名脱敏：保留首位字符 (如 张*、诸**)
     */
    public static String maskRealName(String name) {
        if (name == null || name.length() < 2) {
            return name;
        }
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }
}
