package com.fly.platform.common.security;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fly.platform.auth.entity.UamAccountRole;
import com.fly.platform.auth.entity.UamRole;
import com.fly.platform.auth.entity.UamRoleGrant;
import com.fly.platform.auth.mapper.UamAccountRoleMapper;
import com.fly.platform.auth.mapper.UamRoleGrantMapper;
import com.fly.platform.auth.mapper.UamRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sa-Token 自定义权限加载接口实现
 * 动态根据当前登录的用户ID，拉取角色列表和权限码列表
 *
 * @author SoarCloud Team
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UamAccountRoleMapper accountRoleMapper;
    private final UamRoleMapper roleMapper;
    private final UamRoleGrantMapper roleGrantMapper;

    /**
     * 获取指定账号的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        List<String> roleCodes = getRoleList(loginId, loginType);
        if (roleCodes.isEmpty()) {
            return Collections.emptyList();
        }
        // 管理员直接拥有全局通配权限
        if (roleCodes.contains("ROLE_ADMIN")) {
            return Collections.singletonList("*");
        }
        // 查询关联的角色ID
        List<Long> roleIds = roleMapper.selectList(new LambdaQueryWrapper<UamRole>()
                        .in(UamRole::getRoleCode, roleCodes))
                .stream().map(UamRole::getRoleId).collect(Collectors.toList());

        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        return roleGrantMapper.selectList(new LambdaQueryWrapper<UamRoleGrant>()
                        .in(UamRoleGrant::getRoleId, roleIds))
                .stream().map(UamRoleGrant::getPermCode).distinct().collect(Collectors.toList());
    }

    /**
     * 获取指定账号的角色列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        long accountId;
        try {
            accountId = Long.parseLong(loginId.toString());
        } catch (NumberFormatException e) {
            return Collections.emptyList();
        }

        List<UamAccountRole> userRoles = accountRoleMapper.selectList(new LambdaQueryWrapper<UamAccountRole>()
                .eq(UamAccountRole::getAccountId, accountId));

        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> roleIds = userRoles.stream().map(UamAccountRole::getRoleId).collect(Collectors.toList());
        return roleMapper.selectBatchIds(roleIds).stream()
                .map(UamRole::getRoleCode)
                .collect(Collectors.toList());
    }
}
