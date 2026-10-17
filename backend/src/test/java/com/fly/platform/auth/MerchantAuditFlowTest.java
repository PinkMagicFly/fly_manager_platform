package com.fly.platform.auth;

import com.fly.platform.auth.dto.MerchantApplyDTO;
import com.fly.platform.auth.dto.MerchantAuditDTO;
import com.fly.platform.auth.entity.MerchantArchive;
import com.fly.platform.auth.entity.UamAccount;
import com.fly.platform.auth.mapper.UamAccountMapper;
import com.fly.platform.auth.service.MerchantArchiveService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@DisplayName("集成测试: 商户资质入驻向导与审批流转")
public class MerchantAuditFlowTest {

    @Autowired
    private MerchantArchiveService merchantService;

    @Autowired
    private UamAccountMapper accountMapper;

    @Test
    @DisplayName("商户申请入驻 -> 管理员审核通过 -> 账号自动激活全闭环")
    public void testMerchantAuditPass() {
        MerchantApplyDTO applyDTO = new MerchantApplyDTO();
        applyDTO.setCorpName("大湾区低空通用航空有限公司");
        applyDTO.setLicenseNo("91440300TEST123456");
        applyDTO.setContactName("赵总");
        applyDTO.setContactPhone("13911112222");
        applyDTO.setLoginName("merchant_dwan");
        applyDTO.setPassword("123456");
        applyDTO.setAirworthinessCert("CAAC-TEST-001");

        MerchantArchive archive = merchantService.submitApply(applyDTO);
        assertThat(archive).isNotNull();
        assertThat(archive.getAuditState()).isEqualTo(0);

        UamAccount account = accountMapper.selectById(archive.getAccountId());
        assertThat(account.getStatus()).isEqualTo(0);

        MerchantAuditDTO auditDTO = new MerchantAuditDTO();
        auditDTO.setMerchantId(archive.getMerchantId());
        auditDTO.setAuditState(1);
        auditDTO.setAuditNote("资质齐全，准予接入");
        merchantService.audit(auditDTO, 1L);

        UamAccount activatedAccount = accountMapper.selectById(archive.getAccountId());
        assertThat(activatedAccount.getStatus()).isEqualTo(1);
    }
}
