package com.fly.platform.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.fly.platform.common.api.Result;
import com.fly.platform.common.audit.Audit;
import com.fly.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;

/**
 * 超级管理员系统基准数据初始化与治理重置控制器
 *
 * @author SoarCloud Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/database")
@RequiredArgsConstructor
public class AdminDatabaseController {

    private final DataSource dataSource;

    /**
     * 一键重置平台初始化种子数据（全平台角色、权限、预置账号、场站、空域等恢复至基准状态）
     */
    @SaCheckRole("ROLE_ADMIN")
    @Audit(module = "系统治理", action = "一键重置平台种子数据")
    @PostMapping("/reset-init-data")
    public Result<String> resetInitData() {
        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("sql/init-data.sql"));
            populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
            populator.setContinueOnError(false);
            populator.setIgnoreFailedDrops(true);
            populator.execute(dataSource);
            log.info("超级管理员触发一键重置平台种子数据成功！");
            return Result.ok("平台初始化种子数据已成功重置！预置账号：admin/merchant_sf/dispatcher_sf/atc_officer/pilot_sf/maintain_li/customer_demo（默认密码 123456）已全部规范就绪。");
        } catch (Exception e) {
            log.error("重置平台种子数据失败", e);
            throw new BusinessException(500, "重置种子数据执行失败: " + e.getMessage());
        }
    }
}
