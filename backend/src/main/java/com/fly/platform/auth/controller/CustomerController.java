package com.fly.platform.auth.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.fly.platform.auth.dto.CustomerVerifyDTO;
import com.fly.platform.auth.entity.CustomerArchive;
import com.fly.platform.auth.service.CustomerArchiveService;
import com.fly.platform.common.api.Result;
import com.fly.platform.common.audit.Audit;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客户实名与信用授信控制器
 *
 * @author SoarCloud Team
 */
@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerArchiveService customerArchiveService;

    @PostMapping("/verify")
    @Audit(module = "客户认证", action = "提交实名认证")
    public Result<CustomerArchive> verifyCustomer(@Valid @RequestBody CustomerVerifyDTO dto) {
        Long accountId = StpUtil.getLoginIdAsLong();
        return Result.ok("实名认证已提交成功", customerArchiveService.verifyCustomer(accountId, dto));
    }

    @GetMapping("/my-archive")
    public Result<CustomerArchive> getMyArchive() {
        Long accountId = StpUtil.getLoginIdAsLong();
        return Result.ok(customerArchiveService.getMyArchive(accountId));
    }

    @GetMapping("/list")
    @SaCheckRole("ROLE_ADMIN")
    public Result<List<CustomerArchive>> listCustomers(
            @RequestParam(required = false) Integer authState,
            @RequestParam(required = false) String keyword) {
        return Result.ok(customerArchiveService.listCustomers(authState, keyword));
    }

    @PutMapping("/{customerId}/audit")
    @SaCheckRole("ROLE_ADMIN")
    @Audit(module = "客户认证", action = "审批客户实名与调整授信")
    public Result<Void> auditCustomer(
            @PathVariable Long customerId,
            @RequestParam(required = false) Integer authState,
            @RequestParam(required = false) java.math.BigDecimal creditLimit) {
        customerArchiveService.auditCustomer(customerId, authState, creditLimit);
        return Result.ok("客户认证状态与授信额度已更新", null);
    }
}

