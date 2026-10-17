package com.fly.platform.auth.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.fly.platform.auth.dto.*;
import com.fly.platform.auth.entity.MerchantArchive;
import com.fly.platform.auth.entity.UamAccount;
import com.fly.platform.auth.service.MerchantArchiveService;
import com.fly.platform.common.api.Result;
import com.fly.platform.common.audit.Audit;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商户企业与子账号管理控制器
 *
 * @author SoarCloud Team
 */
@RestController
@RequestMapping("/api/v1/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantArchiveService merchantArchiveService;

    @PostMapping("/apply")
    @Audit(module = "商户入驻", action = "提交企业入驻资质")
    public Result<MerchantArchive> submitApply(@Valid @RequestBody MerchantApplyDTO dto) {
        MerchantArchive archive = merchantArchiveService.submitApply(dto);
        return Result.ok("企业资质入驻申请已提交，请等待平台管理员审批", archive);
    }

    @PostMapping("/audit")
    @SaCheckRole("ROLE_ADMIN")
    @Audit(module = "商户入驻", action = "审批企业入驻资质")
    public Result<Void> audit(@Valid @RequestBody MerchantAuditDTO dto) {
        Long auditorId = StpUtil.getLoginIdAsLong();
        merchantArchiveService.audit(dto, auditorId);
        String msg = dto.getAuditState() == 1 ? "商户入驻已审批通过，账号已激活开通" : "已驳回该企业入驻申请";
        return Result.ok(msg, null);
    }

    @GetMapping("/my-archive")
    public Result<MerchantArchive> getMyArchive() {
        Long accountId = StpUtil.getLoginIdAsLong();
        return Result.ok(merchantArchiveService.getMyArchive(accountId));
    }

    @GetMapping("/list")
    @SaCheckRole("ROLE_ADMIN")
    public Result<List<MerchantArchive>> listMerchants(
            @RequestParam(required = false) Integer auditState,
            @RequestParam(required = false) String keyword) {
        return Result.ok(merchantArchiveService.listMerchants(auditState, keyword));
    }

    @GetMapping("/sub-accounts")
    @SaCheckRole("ROLE_MERCHANT")
    public Result<List<UamAccount>> listSubAccounts() {
        Long accountId = StpUtil.getLoginIdAsLong();
        return Result.ok(merchantArchiveService.listSubAccounts(accountId));
    }

    @PostMapping("/sub-accounts")
    @SaCheckRole("ROLE_MERCHANT")
    @Audit(module = "企业组织", action = "创建企业子账号")
    public Result<UamAccount> createSubAccount(@Valid @RequestBody SubAccountCreateDTO dto) {
        Long parentAccountId = StpUtil.getLoginIdAsLong();
        return Result.ok("子账号创建成功", merchantArchiveService.createSubAccount(parentAccountId, dto));
    }

    @PutMapping("/sub-accounts/{subAccountId}/status")
    @SaCheckRole("ROLE_MERCHANT")
    @Audit(module = "企业组织", action = "变更子账号状态")
    public Result<Void> changeSubAccountStatus(
            @PathVariable Long subAccountId,
            @RequestParam Integer status) {
        Long parentAccountId = StpUtil.getLoginIdAsLong();
        merchantArchiveService.changeSubAccountStatus(parentAccountId, subAccountId, status);
        return Result.ok("子账号状态已更新", null);
    }
}

