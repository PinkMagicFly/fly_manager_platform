package com.fly.platform.common;

import com.fly.platform.common.desensitize.DesensitizeUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("单元测试: 敏感字段脱敏工具")
public class DesensitizeUtilsTest {

    @Test
    @DisplayName("断言手机号按 138****0000 规范脱敏")
    public void testMobileMask() {
        String mobile = "13812345678";
        assertThat(DesensitizeUtils.maskMobile(mobile)).isEqualTo("138****5678");
    }

    @Test
    @DisplayName("断言身份证前六后四脱敏")
    public void testIdCardMask() {
        String idCard = "440301199001011234";
        assertThat(DesensitizeUtils.maskIdCard(idCard)).isEqualTo("440301********1234");
    }
}
