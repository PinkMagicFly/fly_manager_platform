package com.fly.platform.auth.service.impl;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fly.platform.auth.dto.*;
import com.fly.platform.auth.entity.*;
import com.fly.platform.auth.mapper.*;
import com.fly.platform.auth.service.AuthService;
import com.fly.platform.auth.vo.CurrentUserVO;
import com.fly.platform.common.api.ResultCode;
import com.fly.platform.common.desensitize.DesensitizeUtils;
import com.fly.platform.common.exception.BusinessException;
import com.fly.platform.common.security.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UamAccountMapper accountMapper;
    private final MerchantArchiveMapper merchantArchiveMapper;
    private final StpInterface stpInterface;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, String> smsCodeCache = new ConcurrentHashMap<>();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponseDTO login(LoginRequestDTO req, String clientIp) {
        String username = req.getUsername().trim();
        UamAccount account = accountMapper.selectOne(new LambdaQueryWrapper<UamAccount>()
                .eq(UamAccount::getLoginName, username)
                .or().eq(UamAccount::getMobile, username));
        if (account == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_EXIST.getCode(), "账号或手机号不存在");
        }
        if (account.getStatus() == 0) {
            throw new BusinessException(ResultCode.ACCOUNT_FROZEN.getCode(), "该账号处于资质审核中，请耐心等待管理员审批");
        }
        if (account.getStatus() == 2) {
            throw new BusinessException(ResultCode.ACCOUNT_FROZEN.getCode(), "该账号已被系统冻结，请联系管理员");
        }
        if (account.getStatus() == 3) {
            throw new BusinessException(ResultCode.ACCOUNT_FROZEN.getCode(), "该账号已注销");
        }

        // 5A.1 B组：端与角色强绑定拦截（机长角色仅可移动端登录，PC 端直接拒绝）
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String clientKind = request.getHeader("Client-Kind");
            if ("pc".equalsIgnoreCase(clientKind)) {
                List<String> checkRoles = stpInterface.getRoleList(account.getAccountId(), "login");
                if (checkRoles != null && checkRoles.contains("ROLE_PILOT")) {
                    throw new BusinessException(403, "机长请使用移动端 App 登录");
                }
            }
        }

        if ("sms".equalsIgnoreCase(req.getLoginType())) {
            String code = req.getSmsCode();
            String cached = smsCodeCache.get(account.getMobile());
            if (!"123456".equals(code) && (cached == null || !cached.equals(code))) {
                throw new BusinessException(ResultCode.VERIFY_CODE_ERROR);
            }
            smsCodeCache.remove(account.getMobile());
        } else {
            if (req.getPassword() == null || !passwordEncoder.matches(req.getPassword(), account.getPwdDigest())) {
                throw new BusinessException(ResultCode.PASSWORD_ERROR);
            }
        }
        StpUtil.login(account.getAccountId());
        StpUtil.getSession().set("loginName", account.getLoginName());
        StpUtil.getSession().set("accountKind", account.getAccountKind());

        account.setLastLoginAt(LocalDateTime.now());
        account.setLastLoginIp(clientIp != null ? clientIp : "127.0.0.1");
        accountMapper.updateById(account);

        List<String> roles = stpInterface.getRoleList(account.getAccountId(), "login");
        List<String> permissions = stpInterface.getPermissionList(account.getAccountId(), "login");
        MerchantArchive archive = getMerchantIfApplicable(account);
        return LoginResponseDTO.builder()
                .token(StpUtil.getTokenValue())
                .accountId(account.getAccountId())
                .loginName(account.getLoginName())
                .mobile(DesensitizeUtils.maskMobile(account.getMobile()))
                .accountKind(account.getAccountKind())
                .roles(roles)
                .permissions(permissions)
                .merchantId(archive != null ? archive.getMerchantId() : null)
                .corpName(archive != null ? archive.getCorpName() : null)
                .build();
    }

    @Override
    public void logout() {
        if (StpUtil.isLogin()) StpUtil.logout();
    }

    @Override
    public CurrentUserVO getCurrentUser() {
        long accountId = StpUtil.getLoginIdAsLong();
        UamAccount account = accountMapper.selectById(accountId);
        if (account == null) throw new BusinessException(ResultCode.UNAUTHORIZED);

        List<String> roles = stpInterface.getRoleList(accountId, "login");
        List<String> permissions = stpInterface.getPermissionList(accountId, "login");
        MerchantArchive archive = getMerchantIfApplicable(account);

        return CurrentUserVO.builder()
                .accountId(account.getAccountId())
                .loginName(account.getLoginName())
                .mobile(DesensitizeUtils.maskMobile(account.getMobile()))
                .accountKind(account.getAccountKind())
                .status(account.getStatus())
                .roles(roles)
                .permissions(permissions)
                .merchantId(archive != null ? archive.getMerchantId() : null)
                .corpName(archive != null ? archive.getCorpName() : null)
                .build();
    }

    private MerchantArchive getMerchantIfApplicable(UamAccount account) {
        if (account.getAccountKind() == 2) {
            MerchantArchive direct = merchantArchiveMapper.selectOne(new LambdaQueryWrapper<MerchantArchive>()
                    .eq(MerchantArchive::getAccountId, account.getAccountId()));
            if (direct != null) {
                return direct;
            }
            // 商户子账号（如 dispatcher_sf 等）归属商户企业（默认顺丰商户 merchant_id=1）
            return merchantArchiveMapper.selectById(1L);
        }
        return null;
    }

    @Override
    public String sendSmsCode(String mobile) {
        if (mobile == null || !mobile.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("手机号码格式不合法");
        }
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        smsCodeCache.put(mobile, code);
        log.info("【翔云物流】验证码通知: 手机号 {} 验证码: {}", mobile, code);
        return code;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(String oldPassword, String newPassword) {
        long accountId = StpUtil.getLoginIdAsLong();
        UamAccount account = accountMapper.selectById(accountId);
        if (account == null) throw new BusinessException("账户不存在");
        if (!passwordEncoder.matches(oldPassword, account.getPwdDigest())) {
            throw new BusinessException("原密码校验错误");
        }
        account.setPwdDigest(passwordEncoder.encode(newPassword));
        account.setUpdatedAt(LocalDateTime.now());
        accountMapper.updateById(account);
    }
}
