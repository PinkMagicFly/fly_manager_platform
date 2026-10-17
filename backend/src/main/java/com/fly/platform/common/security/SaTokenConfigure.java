package com.fly.platform.common.security;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 路由鉴权与跨域全局配置
 *
 * @author SoarCloud Team
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    /**
     * 注册 Sa-Token 路由拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
                    // 全局校验登录（白名单路由除外）
                    StpUtil.checkLogin();
                }))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/v1/auth/login",
                        "/api/v1/auth/send-sms-code",
                        "/api/v1/merchant/apply",
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/h2-console/**",
                        "/favicon.ico"
                );
    }

    /**
     * 配置跨域过滤器 (支持 PC 与 UniApp 本地开发跨域调用)
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
