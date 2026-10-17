package com.fly.platform.auth;

import com.fly.platform.auth.dto.CustomerVerifyDTO;
import com.fly.platform.auth.entity.CustomerArchive;
import com.fly.platform.auth.mapper.CustomerArchiveMapper;
import com.fly.platform.auth.service.CustomerArchiveService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("集成测试: 客户实名与信用授信额度调增闭环")
public class CustomerAuditFlowTest {

    @Autowired
    private CustomerArchiveService customerService;

    @Autowired
    private CustomerArchiveMapper customerMapper;

    @Test
    @DisplayName("客户提交实名 -> 管理员调增授信额度 -> 断言额度与状态双更新")
    public void testCustomerAuditAndCreditAdjust() {
        CustomerVerifyDTO verifyDTO = new CustomerVerifyDTO();
        verifyDTO.setRealName("腾讯低空物流测试主体");
        verifyDTO.setIdCard("440301199001018888");
        verifyDTO.setCreditLimit(new BigDecimal("5000.00"));

        CustomerArchive archive = customerService.verifyCustomer(6L, verifyDTO);
        assertThat(archive).isNotNull();
        assertThat(archive.getCreditLimit()).isEqualByComparingTo("5000.00");

        // 管理员进行调额复核，调增额度至 50000.00
        BigDecimal newLimit = new BigDecimal("50000.00");
        customerService.auditCustomer(archive.getCustomerId(), 1, newLimit);

        CustomerArchive updated = customerMapper.selectById(archive.getCustomerId());
        assertThat(updated).isNotNull();
        assertThat(updated.getAuthState()).isEqualTo(1);
        assertThat(updated.getCreditLimit()).isEqualByComparingTo("50000.00");
    }
}
