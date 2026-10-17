package com.fly.platform.common;

import com.fly.platform.common.util.IpUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class IpUtilsTest {

    @Test
    @DisplayName("测试 X-Forwarded-For 单IP解析")
    public void testSingleXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "115.198.22.33");
        assertThat(IpUtils.getClientIp(request)).isEqualTo("115.198.22.33");
    }

    @Test
    @DisplayName("测试 X-Forwarded-For 多级反向代理多IP解析首个真实IP")
    public void testMultipleXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "115.198.22.33, 10.0.0.1, 127.0.0.1");
        assertThat(IpUtils.getClientIp(request)).isEqualTo("115.198.22.33");
    }

    @Test
    @DisplayName("测试 X-Real-IP 解析")
    public void testXRealIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "223.5.5.5");
        assertThat(IpUtils.getClientIp(request)).isEqualTo("223.5.5.5");
    }

    @Test
    @DisplayName("测试兜底 RemoteAddr 解析")
    public void testFallbackRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        assertThat(IpUtils.getClientIp(request)).isEqualTo("192.168.1.100");
    }
}
