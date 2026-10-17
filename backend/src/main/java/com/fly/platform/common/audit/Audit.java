package com.fly.platform.common.audit;

import java.lang.annotation.*;

/**
 * 敏感操作全局审计留痕注解
 *
 * @author SoarCloud Team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audit {

    /** 业务所属模块 (如: 账户认证, 商户审核, 权限设置) */
    String module() default "";

    /** 核心业务动作 (如: 账号登录, 审核通过, 冻结账号) */
    String action() default "";
}
