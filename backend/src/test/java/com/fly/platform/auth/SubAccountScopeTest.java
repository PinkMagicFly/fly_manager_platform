package com.fly.platform.auth;

import com.fly.platform.auth.dto.SubAccountCreateDTO;
import com.fly.platform.auth.entity.UamAccount;
import com.fly.platform.auth.service.MerchantArchiveService;
import com.fly.platform.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@DisplayName("集成测试: 商户企业子账号管理与越权边界守卫")
public class SubAccountScopeTest {

    @Autowired
    private MerchantArchiveService merchantService;

    @Test
    @DisplayName("商户创建子账号并查询列表")
    public void testSubAccountLifecycle() {
        SubAccountCreateDTO dto = new SubAccountCreateDTO();
        dto.setLoginName("sf_pilot_dispatcher");
        dto.setPassword("123456");
        dto.setMobile("13988887777");
        dto.setEmail("pilot_dispatch@sf.com");
        dto.setRoleIds(Collections.singletonList(2L));

        UamAccount sub = merchantService.createSubAccount(2L, dto);
        assertThat(sub).isNotNull();
        assertThat(sub.getLoginName()).isEqualTo("sf_pilot_dispatcher");

        List<UamAccount> subs = merchantService.listSubAccounts(2L);
        assertThat(subs).isNotEmpty();

        assertThatThrownBy(() -> merchantService.changeSubAccountStatus(2L, 2L, 2))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不可变更主管理账号");
    }
}
