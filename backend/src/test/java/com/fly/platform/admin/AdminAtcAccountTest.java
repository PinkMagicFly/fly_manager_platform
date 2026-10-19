package com.fly.platform.admin;

import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.stp.StpUtil;
import com.fly.platform.admin.controller.AdminAccountController;
import com.fly.platform.admin.dto.AtcAccountCreateDTO;
import com.fly.platform.admin.vo.AccountManageVO;
import com.fly.platform.common.api.PageResult;
import com.fly.platform.common.api.Result;
import com.fly.platform.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 平台管理员创建空管监察员账号与账号启停控制测试
 *
 * @author SoarCloud Team
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AdminAtcAccountTest {

    @Autowired
    private AdminAccountController accountController;

    @AfterEach
    void tearDown() {
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
    }

    @Test
    @DisplayName("管理员创建空管账号全流程: 创建 -> 列表查询 -> 启停状态变更")
    void testCreateAtcAccountWorkflow() {
        // 模拟超级管理员登录 (accountId=1)
        StpUtil.login(1L);

        AtcAccountCreateDTO dto = new AtcAccountCreateDTO();
        dto.setLoginName("atc_sz_tower01");
        dto.setMobile("13922223333");
        dto.setEmail("atc_tower@caac.gov.cn");
        dto.setOrgName("深圳空管站低空塔台");
        dto.setPassword("123456");

        Result<AccountManageVO> res = accountController.createAtcAccount(dto);
        assertThat(res.getCode()).isEqualTo(200);
        AccountManageVO vo = res.getData();
        assertThat(vo).isNotNull();
        assertThat(vo.getLoginName()).isEqualTo("atc_sz_tower01");
        assertThat(vo.getAccountKind()).isEqualTo(4); // 4-空管监察员
        assertThat(vo.getRoleCodes()).contains("ROLE_ATC");
        assertThat(vo.getStatus()).isEqualTo(1);

        // 分页查询账号
        Result<PageResult<AccountManageVO>> pageRes = accountController.pageAccounts(4, "tower", 1, 10);
        assertThat(pageRes.getData().getList()).isNotEmpty();
        assertThat(pageRes.getData().getList().get(0).getLoginName()).isEqualTo("atc_sz_tower01");

        // 冻结账号
        Result<Void> freezeRes = accountController.updateStatus(vo.getAccountId(), 2);
        assertThat(freezeRes.getCode()).isEqualTo(200);

        // 解冻账号
        Result<Void> unfreezeRes = accountController.updateStatus(vo.getAccountId(), 1);
        assertThat(unfreezeRes.getCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("安全守卫测试: 非管理员(如商户/客户)无权创建空管账号 (403/BusinessException)")
    void testNonAdminCreateAtcDenied() {
        // 模拟商户登录 (accountId=2)
        StpUtil.login(2L);

        AtcAccountCreateDTO dto = new AtcAccountCreateDTO();
        dto.setLoginName("illegal_atc");
        dto.setMobile("13900001111");

        assertThatThrownBy(() -> accountController.createAtcAccount(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅超级管理员有权创建空管监察员账号");
    }

    @Test
    @DisplayName("安全守卫测试: 管理员不可冻结自身账号")
    void testAdminCannotFreezeSelf() {
        StpUtil.login(1L);

        assertThatThrownBy(() -> accountController.updateStatus(1L, 2))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不可冻结或停用当前登录的管理员账号自身");
    }
}
