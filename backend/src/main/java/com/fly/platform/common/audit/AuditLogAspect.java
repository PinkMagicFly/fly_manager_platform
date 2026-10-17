package com.fly.platform.common.audit;

import cn.dev33.satoken.stp.StpUtil;
import com.fly.platform.admin.entity.AuditLog;
import com.fly.platform.admin.mapper.AuditLogMapper;
import com.fly.platform.common.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 敏感操作全局审计日志切面
 *
 * @author SoarCloud Team
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogMapper auditLogMapper;

    @Around("@annotation(audit)")
    public Object aroundAudit(ProceedingJoinPoint point, Audit audit) throws Throwable {
        long startTime = System.currentTimeMillis();
        AuditLog auditLog = new AuditLog();
        auditLog.setModuleName(audit.module());
        auditLog.setActionName(audit.action());
        auditLog.setCreatedAt(LocalDateTime.now());

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            auditLog.setRequestUri(request.getRequestURI());
            auditLog.setClientIp(IpUtils.getClientIp(request));
            String clientKind = request.getHeader("Client-Kind");
            auditLog.setClientKind(clientKind != null ? clientKind : "pc");
        } else {
            auditLog.setRequestUri("internal");
            auditLog.setClientIp("127.0.0.1");
            auditLog.setClientKind("system");
        }

        // 获取当前登录人
        if (StpUtil.isLogin()) {
            auditLog.setAccountId(StpUtil.getLoginIdAsLong());
            Object loginNameObj = StpUtil.getSession().get("loginName");
            auditLog.setLoginName(loginNameObj != null ? loginNameObj.toString() : "user_" + StpUtil.getLoginId());
        } else {
            auditLog.setAccountId(0L);
            auditLog.setLoginName("anonymous");
        }

        // 脱敏记录入参简要
        try {
            String paramsStr = Arrays.toString(point.getArgs());
            if (paramsStr.length() > 500) {
                paramsStr = paramsStr.substring(0, 500) + "...";
            }
            auditLog.setReqParams(paramsStr);
        } catch (Exception e) {
            auditLog.setReqParams("parse_error");
        }

        Object result;
        try {
            result = point.proceed();
            auditLog.setRespStatus(200);
            return result;
        } catch (Throwable t) {
            auditLog.setRespStatus(500);
            auditLog.setErrorMsg(t.getMessage());
            throw t;
        } finally {
            long cost = System.currentTimeMillis() - startTime;
            auditLog.setCostTime(cost);
            try {
                auditLogMapper.insert(auditLog);
            } catch (Exception e) {
                log.error("写入操作审计日志失败: {}", e.getMessage());
            }
        }
    }
}
