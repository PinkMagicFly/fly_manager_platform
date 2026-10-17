package com.fly.platform.common.interceptor;

import com.fly.platform.common.api.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 双端自适应报文裁剪器 (Client-Kind Adapter)
 * 前端请求头携带 Client-Kind: pc 或 Client-Kind: app
 * 移动端裁剪不必要的大体积数据，大幅提升弱网加载速度
 *
 * @author SoarCloud Team
 */
@RestControllerAdvice
public class ClientKindResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpServletRequest = servletRequest.getServletRequest();
            String clientKind = httpServletRequest.getHeader("Client-Kind");
            // 若为移动端请求，可在响应结果中标记适配终端
            if ("app".equalsIgnoreCase(clientKind) && body instanceof Result<?> result) {
                // 移动端轻量模式标识已生效
                return result;
            }
        }
        return body;
    }
}
