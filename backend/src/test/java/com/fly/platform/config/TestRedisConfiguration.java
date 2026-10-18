package com.fly.platform.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Proxy;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 单元测试环境 Redis 内存隔离配置
 * 避免单元测试对外部 Redis 物理服务的依赖，保障构建确定性与毫秒级执行
 *
 * @author SoarCloud Team
 */
@Configuration
@Profile("test")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TestRedisConfiguration {

    private final Map<String, String> kvStorage = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> setStorage = new ConcurrentHashMap<>();

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate template = Mockito.mock(StringRedisTemplate.class);

        // 使用 JDK 动态代理实现 ValueOperations，免疫泛型桥接方法
        ValueOperations<String, String> valOps = (ValueOperations<String, String>) Proxy.newProxyInstance(
                ValueOperations.class.getClassLoader(),
                new Class<?>[]{ValueOperations.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("set".equals(name) && args != null && args.length >= 2) {
                        kvStorage.put(String.valueOf(args[0]), String.valueOf(args[1]));
                        return null;
                    }
                    if ("get".equals(name) && args != null && args.length >= 1) {
                        return kvStorage.get(String.valueOf(args[0]));
                    }
                    return null;
                }
        );

        // 使用 JDK 动态代理实现 SetOperations，免疫泛型桥接方法与可变参数
        SetOperations<String, String> setOps = (SetOperations<String, String>) Proxy.newProxyInstance(
                SetOperations.class.getClassLoader(),
                new Class<?>[]{SetOperations.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("add".equals(name) && args != null && args.length >= 2) {
                        String key = String.valueOf(args[0]);
                        Set<String> set = setStorage.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
                        long added = 0;
                        for (int i = 1; i < args.length; i++) {
                            Object arg = args[i];
                            if (arg instanceof Object[]) {
                                for (Object item : (Object[]) arg) {
                                    if (item != null && set.add(String.valueOf(item))) added++;
                                }
                            } else if (arg != null) {
                                if (set.add(String.valueOf(arg))) added++;
                            }
                        }
                        return added;
                    }
                    if ("remove".equals(name) && args != null && args.length >= 2) {
                        String key = String.valueOf(args[0]);
                        Set<String> set = setStorage.get(key);
                        long removed = 0;
                        if (set != null) {
                            for (int i = 1; i < args.length; i++) {
                                Object arg = args[i];
                                if (arg instanceof Object[]) {
                                    for (Object item : (Object[]) arg) {
                                        if (item != null && set.remove(String.valueOf(item))) removed++;
                                    }
                                } else if (arg != null) {
                                    if (set.remove(String.valueOf(arg))) removed++;
                                }
                            }
                        }
                        return removed;
                    }
                    if ("members".equals(name) && args != null && args.length >= 1) {
                        String key = String.valueOf(args[0]);
                        Set<String> set = setStorage.get(key);
                        return set != null ? new HashSet<>(set) : Collections.emptySet();
                    }
                    if ("isMember".equals(name) && args != null && args.length >= 2) {
                        String key = String.valueOf(args[0]);
                        Set<String> set = setStorage.get(key);
                        return set != null && set.contains(String.valueOf(args[1]));
                    }
                    return null;
                }
        );

        Mockito.when(template.opsForValue()).thenReturn(valOps);
        Mockito.when(template.opsForSet()).thenReturn(setOps);
        return template;
    }
}
