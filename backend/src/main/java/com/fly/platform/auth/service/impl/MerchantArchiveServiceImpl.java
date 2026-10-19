package com.fly.platform.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fly.platform.auth.dto.*;
import com.fly.platform.auth.entity.*;
import com.fly.platform.auth.mapper.*;
import com.fly.platform.auth.service.MerchantArchiveService;
import com.fly.platform.common.api.ResultCode;
import com.fly.platform.common.exception.BusinessException;
import com.fly.platform.common.security.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantArchiveServiceImpl implements MerchantArchiveService {

    private final MerchantArchiveMapper merchantArchiveMapper;
    private final UamAccountMapper accountMapper;
    private final UamRoleMapper roleMapper;
    private final UamAccountRoleMapper accountRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantArchive submitApply(MerchantApplyDTO dto) {
        Long existName = accountMapper.selectCount(new LambdaQueryWrapper<UamAccount>()
                .eq(UamAccount::getLoginName, dto.getLoginName().trim()));
        if (existName > 0) throw new BusinessException(ResultCode.ACCOUNT_EXISTS.getCode(), "该登录用户名已被占用");

        Long existMobile = accountMapper.selectCount(new LambdaQueryWrapper<UamAccount>()
                .eq(UamAccount::getMobile, dto.getContactPhone().trim()));
        if (existMobile > 0) throw new BusinessException(ResultCode.ACCOUNT_EXISTS.getCode(), "该手机号已注册");

        Long existLicense = merchantArchiveMapper.selectCount(new LambdaQueryWrapper<MerchantArchive>()
                .eq(MerchantArchive::getLicenseNo, dto.getLicenseNo().trim()));
        if (existLicense > 0) throw new BusinessException(ResultCode.ACCOUNT_EXISTS.getCode(), "该统一社会信用代码已入驻");

        UamAccount account = new UamAccount();
        account.setLoginName(dto.getLoginName().trim());
        account.setMobile(dto.getContactPhone().trim());
        account.setPwdDigest(passwordEncoder.encode(dto.getPassword()));
        account.setAccountKind(2);
        account.setStatus(0);
        account.initAudit();
        accountMapper.insert(account);

        UamRole merchantRole = roleMapper.selectOne(new LambdaQueryWrapper<UamRole>()
                .eq(UamRole::getRoleCode, "ROLE_MERCHANT"));
        if (merchantRole != null) {
            UamAccountRole accountRole = new UamAccountRole();
            accountRole.setAccountId(account.getAccountId());
            accountRole.setRoleId(merchantRole.getRoleId());
            accountRole.initAudit();
            accountRoleMapper.insert(accountRole);
        }

        MerchantArchive archive = new MerchantArchive();
        archive.setAccountId(account.getAccountId());
        archive.setCorpName(dto.getCorpName().trim());
        archive.setLicenseNo(dto.getLicenseNo().trim());
        archive.setContactName(dto.getContactName().trim());
        archive.setContactPhone(dto.getContactPhone().trim());
        archive.setAirworthinessCert(dto.getAirworthinessCert());
        archive.setAuditState(0);
        archive.initAudit();
        merchantArchiveMapper.insert(archive);
        return archive;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(MerchantAuditDTO dto, Long auditorId) {
        MerchantArchive archive = merchantArchiveMapper.selectById(dto.getMerchantId());
        if (archive == null) throw new BusinessException("商户档案不存在");

        archive.setAuditState(dto.getAuditState());
        archive.setAuditNote(dto.getAuditNote());
        archive.setAuditAt(LocalDateTime.now());
        archive.setAuditorId(auditorId);
        archive.setUpdatedAt(LocalDateTime.now());
        merchantArchiveMapper.updateById(archive);

        UamAccount account = accountMapper.selectById(archive.getAccountId());
        if (account != null) {
            if (dto.getAuditState() == 1) {
                account.setStatus(1); // 激活正常
            } else if (dto.getAuditState() == 2) {
                account.setStatus(2); // 冻结/驳回
            }
            account.setUpdatedAt(LocalDateTime.now());
            accountMapper.updateById(account);
        }
    }

    @Override
    public MerchantArchive getMyArchive(Long accountId) {
        return merchantArchiveMapper.selectOne(new LambdaQueryWrapper<MerchantArchive>()
                .eq(MerchantArchive::getAccountId, accountId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UamAccount createSubAccount(Long parentAccountId, SubAccountCreateDTO dto) {
        Long exist = accountMapper.selectCount(new LambdaQueryWrapper<UamAccount>()
                .eq(UamAccount::getLoginName, dto.getLoginName().trim()));
        if (exist > 0) throw new BusinessException("子账号用户名已存在");

        UamAccount sub = new UamAccount();
        sub.setLoginName(dto.getLoginName().trim());
        sub.setMobile(dto.getMobile().trim());
        sub.setEmail(dto.getEmail());
        sub.setPwdDigest(passwordEncoder.encode(dto.getPassword()));
        sub.setAccountKind(2);
        sub.setStatus(1);
        sub.initAudit();
        accountMapper.insert(sub);

        if (dto.getRoleIds() != null) {
            for (Long roleId : dto.getRoleIds()) {
                UamAccountRole ar = new UamAccountRole();
                ar.setAccountId(sub.getAccountId());
                ar.setRoleId(roleId);
                ar.initAudit();
                accountRoleMapper.insert(ar);
            }
        }
        return sub;
    }

    @Override
    public List<UamAccount> listSubAccounts(Long parentAccountId) {
        List<UamAccount> list = accountMapper.selectList(new LambdaQueryWrapper<UamAccount>()
                .eq(UamAccount::getAccountKind, 2)
                .ne(UamAccount::getAccountId, parentAccountId));
        for (UamAccount acc : list) {
            List<UamAccountRole> uars = accountRoleMapper.selectList(new LambdaQueryWrapper<UamAccountRole>()
                    .eq(UamAccountRole::getAccountId, acc.getAccountId()));
            if (!uars.isEmpty()) {
                List<Long> rids = uars.stream().map(UamAccountRole::getRoleId).collect(Collectors.toList());
                List<UamRole> roles = roleMapper.selectBatchIds(rids);
                acc.setRoleNames(roles.stream().map(UamRole::getRoleName).collect(Collectors.toList()));
                acc.setRoleCodes(roles.stream().map(UamRole::getRoleCode).collect(Collectors.toList()));
            } else {
                acc.setRoleNames(Collections.emptyList());
                acc.setRoleCodes(Collections.emptyList());
            }
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeSubAccountStatus(Long parentAccountId, Long subAccountId, Integer status) {
        if (parentAccountId.equals(subAccountId)) {
            throw new BusinessException("不可变更主管理账号状态");
        }
        UamAccount sub = accountMapper.selectById(subAccountId);
        if (sub == null || sub.getAccountKind() != 2) {
            throw new BusinessException("子账号不存在或越权");
        }
        sub.setStatus(status);
        sub.setUpdatedAt(LocalDateTime.now());
        accountMapper.updateById(sub);
    }

    @Override
    public List<MerchantArchive> listMerchants(Integer auditState, String keyword) {
        LambdaQueryWrapper<MerchantArchive> qw = new LambdaQueryWrapper<>();
        if (auditState != null) {
            qw.eq(MerchantArchive::getAuditState, auditState);
        }
        if (keyword != null && !keyword.isBlank()) {
            qw.and(wrapper -> wrapper.like(MerchantArchive::getCorpName, keyword)
                    .or().like(MerchantArchive::getLicenseNo, keyword)
                    .or().like(MerchantArchive::getContactName, keyword));
        }
        qw.orderByDesc(MerchantArchive::getCreatedAt);
        return merchantArchiveMapper.selectList(qw);
    }
}
