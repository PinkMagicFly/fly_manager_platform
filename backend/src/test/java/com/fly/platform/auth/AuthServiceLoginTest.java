package com.fly.platform.auth;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fly.platform.auth.dto.LoginRequestDTO;
import com.fly.platform.auth.dto.LoginResponseDTO;
import com.fly.platform.auth.entity.UamAccount;
import com.fly.platform.auth.mapper.UamAccountMapper;
import com.fly.platform.auth.service.AuthService;
import com.fly.platform.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("集成测试: 用户多端登录鉴权服务")
public class AuthServiceLoginTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UamAccountMapper accountMapper;

    @Test
    @DisplayName("管理员账号正确密码登录，断言返回有效Token与角色")
    public void testAdminLoginSuccess() {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setUsername("admin");
        req.setPassword("123456");
        req.setLoginType("password");

        LoginResponseDTO res = authService.login(req, "127.0.0.1");
        assertThat(res).isNotNull();
        assertThat(res.getToken()).isNotBlank();
        assertThat(res.getLoginName()).isEqualTo("admin");
        assertThat(res.getRoles()).contains("ROLE_ADMIN");
        assertThat(res.getMobile()).isEqualTo("138****0001");
    }

    @Test
    @DisplayName("输入错误密码，断言抛出密码错误异常")
    public void testLoginWithWrongPassword() {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setUsername("admin");
        req.setPassword("wrong_pass");
        req.setLoginType("password");

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码错误");
    }

    @Test
    @DisplayName("待审核状态账号尝试登录，断言直接拦截")
    public void testPendingAccountLogin() {
        accountMapper.update(null, new LambdaUpdateWrapper<UamAccount>()
                .eq(UamAccount::getLoginName, "merchant_pending")
                .set(UamAccount::getStatus, 0));

        LoginRequestDTO req = new LoginRequestDTO();
        req.setUsername("merchant_pending");
        req.setPassword("123456");
        req.setLoginType("password");

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("审核中");
    }
}
