package com.fly.platform.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端真实IP解析工具类 (支持多级反向代理穿透)
 *
 * @author SoarCloud Team
 */
public class IpUtils {

    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";
    private static final String LOCALHOST_IPV4 = "127.0.0.1";

    /**
     * 获取客户端真实公网/局域网IP地址
     *
     * @param request HTTP请求
     * @return 真实IP地址
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return LOCALHOST_IPV4;
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理多级反向代理情况，多个IP以逗号分隔，第一个为客户端真实IP
        if (ip != null && ip.contains(",")) {
            String[] ips = ip.split(",");
            for (String subIp : ips) {
                subIp = subIp.trim();
                if (!subIp.isEmpty() && !UNKNOWN.equalsIgnoreCase(subIp)) {
                    ip = subIp;
                    break;
                }
            }
        }

        if (LOCALHOST_IPV6.equals(ip)) {
            ip = LOCALHOST_IPV4;
        }

        return ip != null ? ip : LOCALHOST_IPV4;
    }
}
