package com.fly.platform.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fly.platform.admin.dto.AtcAccountCreateDTO;
import com.fly.platform.admin.vo.AccountManageVO;
import com.fly.platform.auth.entity.UamAccount;
import com.fly.platform.auth.entity.UamAccountRole;
import com.fly.platform.auth.entity.UamRole;
import com.fly.platform.auth.mapper.UamAccountMapper;
import com.fly.platform.auth.mapper.UamAccountRoleMapper;
import com.fly.platform.auth.mapper.UamRoleMapper;
import com.fly.platform.common.api.PageResult;
import com.fly.platform.common.api.Result;
import com.fly.platform.common.audit.Audit;
import com.fly.platform.common.desensitize.DesensitizeUtils;
import com.fly.platform.common.exception.BusinessException;
import com.fly.platform.common.security.PasswordEncoder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 平台统一账户管理与空管账号创建控制器（仅超管权限）
 *
 * @author SoarCloud Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/accounts")
@RequiredArgsConstructor
public class AdminAccountController {

    private final UamAccountMapper accountMapper;
    private final UamRoleMapper roleMapper;
    private final UamAccountRoleMapper accountRoleMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 分页查询平台账号列表
     */
    @GetMapping("/page")
    @SaCheckRole("ROLE_ADMIN")
    public Result<PageResult<AccountManageVO>> pageAccounts(
            @RequestParam(required = false) Integer accountKind,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "1") int pageNo,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {

        LambdaQueryWrapper<UamAccount> qw = new LambdaQueryWrapper<UamAccount>()
                .eq(accountKind != null, UamAccount::getAccountKind, accountKind);

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            qw.and(w -> w.like(UamAccount::getLoginName, kw)
                    .or().like(UamAccount::getMobile, kw));
        }
        qw.orderByAsc(UamAccount::getAccountId);

        Page<UamAccount> page = accountMapper.selectPage(new Page<UamAccount>(pageNo, pageSize), qw);
        List<AccountManageVO> voList = new ArrayList<>();

        for (UamAccount acc : page.getRecords()) {
            List<UamAccountRole> uars = accountRoleMapper.selectList(new LambdaQueryWrapper<UamAccountRole>()
                    .eq(UamAccountRole::getAccountId, acc.getAccountId()));
            List<String> roleCodes = Collections.emptyList();
            List<String> roleNames = Collections.emptyList();
            if (!uars.isEmpty()) {
                List<Long> rids = uars.stream().map(UamAccountRole::getRoleId).collect(Collectors.toList());
                List<UamRole> roles = roleMapper.selectBatchIds(rids);
                roleCodes = roles.stream().map(UamRole::getRoleCode).collect(Collectors.toList());
                roleNames = roles.stream().map(UamRole::getRoleName).collect(Collectors.toList());
            }

            voList.add(AccountManageVO.builder()
                    .accountId(acc.getAccountId())
                    .loginName(acc.getLoginName())
                    .mobile(DesensitizeUtils.maskMobile(acc.getMobile()))
                    .email(acc.getEmail())
                    .accountKind(acc.getAccountKind())
                    .accountKindLabel(getKindLabel(acc.getAccountKind()))
                    .status(acc.getStatus())
                    .statusLabel(acc.getStatus() == 1 ? "正常使用" : "已冻结")
                    .roleCodes(roleCodes)
                    .roleNames(roleNames)
                    .createdAt(acc.getCreatedAt())
                    .lastLoginAt(acc.getLastLoginAt())
                    .lastLoginIp(acc.getLastLoginIp())
                    .build());
        }

        PageResult<AccountManageVO> result = new PageResult<>(
                voList,
                page.getTotal(),
                (long) pageNo,
                (long) pageSize
        );
        return Result.ok(result);
    }
    /**
     * 新建空管监察员账号（无需强制改密，密码开箱直接登录可用）
     */
    @PostMapping("/atc")
    @SaCheckRole("ROLE_ADMIN")
    @Audit(module = "系统治理", action = "新建空管监察员账号")
    @Transactional(rollbackFor = Exception.class)
    public Result<AccountManageVO> createAtcAccount(@Valid @RequestBody AtcAccountCreateDTO dto) {
        if (!StpUtil.hasRole("ROLE_ADMIN")) {
            throw new BusinessException(403, "仅超级管理员有权创建空管监察员账号");
        }

        String loginName = dto.getLoginName().trim();
        Long existName = accountMapper.selectCount(new LambdaQueryWrapper<UamAccount>()
                .eq(UamAccount::getLoginName, loginName));
        if (existName > 0) {
            throw new BusinessException(400, "该空管账号名称已被占用，请使用其他名称");
        }

        String mobile = dto.getMobile().trim();
        Long existMobile = accountMapper.selectCount(new LambdaQueryWrapper<UamAccount>()
                .eq(UamAccount::getMobile, mobile));
        if (existMobile > 0) {
            throw new BusinessException(400, "该手机号码已被登记占用");
        }

        // 默认密码处理（取消首登强制改密，设置的密码即直接生效使用）
        String rawPwd = (dto.getPassword() != null && !dto.getPassword().trim().isEmpty())
                ? dto.getPassword().trim() : "123456";

        UamAccount atc = new UamAccount();
        atc.setLoginName(loginName);
        atc.setMobile(mobile);
        atc.setEmail(dto.getEmail());
        atc.setPwdDigest(passwordEncoder.encode(rawPwd));
        atc.setAccountKind(4); // 4-空管监察员
        atc.setStatus(1);      // 1-正常启用
        atc.initAudit();
        accountMapper.insert(atc);

        // 绑定空管角色 ROLE_ATC (role_id = 3)
        UamRole atcRole = roleMapper.selectOne(new LambdaQueryWrapper<UamRole>()
                .eq(UamRole::getRoleCode, "ROLE_ATC"));
        Long atcRoleId = (atcRole != null) ? atcRole.getRoleId() : 3L;

        UamAccountRole uar = new UamAccountRole();
        uar.setAccountId(atc.getAccountId());
        uar.setRoleId(atcRoleId);
        uar.initAudit();
        accountRoleMapper.insert(uar);

        log.info("管理员成功创建空管监察员账号: accountId={}, loginName={}", atc.getAccountId(), atc.getLoginName());

        AccountManageVO vo = AccountManageVO.builder()
                .accountId(atc.getAccountId())
                .loginName(atc.getLoginName())
                .mobile(DesensitizeUtils.maskMobile(atc.getMobile()))
                .email(atc.getEmail())
                .accountKind(4)
                .accountKindLabel("空管监察员")
                .status(1)
                .statusLabel("正常使用")
                .roleCodes(Collections.singletonList("ROLE_ATC"))
                .roleNames(Collections.singletonList("空管监察员"))
                .createdAt(atc.getCreatedAt())
                .build();

        return Result.ok("空管监察员账号创建成功！初始密码为：" + rawPwd + "，已赋予审批权限，可直接登录使用", vo);
    }

    /**
     * 变更账号启停状态
     */
    @PutMapping("/{id}/status")
    @SaCheckRole("ROLE_ADMIN")
    @Audit(module = "系统治理", action = "变更平台账号状态")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        Long currentAdminId = StpUtil.getLoginIdAsLong();
        if (currentAdminId.equals(id)) {
            throw new BusinessException(400, "不可冻结或停用当前登录的管理员账号自身");
        }

        UamAccount account = accountMapper.selectById(id);
        if (account == null) {
            throw new BusinessException(404, "账号不存在: " + id);
        }

        account.setStatus(status);
        account.setUpdatedAt(LocalDateTime.now());
        accountMapper.updateById(account);

        return Result.ok(status == 1 ? "账号已解冻启用" : "账号已停用冻结", null);
    }

    private String getKindLabel(Integer kind) {
        if (kind == null) return "未知类型";
        return switch (kind) {
            case 1 -> "货运客户";
            case 2 -> "物流商户";
            case 3 -> "超级管理员";
            case 4 -> "空管监察员";
            case 5 -> "商用机长";
            case 6 -> "运维专员";
            default -> "平台用户";
        };
    }

}
