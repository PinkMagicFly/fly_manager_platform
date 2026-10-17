package com.fly.platform.auth.service;

import com.fly.platform.auth.dto.MerchantApplyDTO;
import com.fly.platform.auth.dto.MerchantAuditDTO;
import com.fly.platform.auth.dto.SubAccountCreateDTO;
import com.fly.platform.auth.entity.MerchantArchive;
import com.fly.platform.auth.entity.UamAccount;

import java.util.List;

/**
 * 商户企业资质与组织服务接口
 *
 * @author SoarCloud Team
 */
public interface MerchantArchiveService {

    /**
     * 提交商户企业资质入驻申请
     *
     * @param dto 入驻表单
     * @return 创建的商户档案
     */
    MerchantArchive submitApply(MerchantApplyDTO dto);

    /**
     * 管理员审批商户资质
     *
     * @param dto       审批结果
     * @param auditorId 审批人ID
     */
    void audit(MerchantAuditDTO dto, Long auditorId);

    /**
     * 查询指定账号的企业资质档案
     *
     * @param accountId 登录账户ID
     * @return 商户档案
     */
    MerchantArchive getMyArchive(Long accountId);

    /**
     * 商户为主企业开设子账号
     *
     * @param parentAccountId 主账号ID
     * @param dto             子账号信息
     * @return 新增的子账号
     */
    UamAccount createSubAccount(Long parentAccountId, SubAccountCreateDTO dto);

    /**
     * 查询企业子账号列表
     *
     * @param parentAccountId 主账号ID
     * @return 子账号列表
     */
    List<UamAccount> listSubAccounts(Long parentAccountId);

    /**
     * 启停子账号
     *
     * @param parentAccountId 主账号ID
     * @param subAccountId    子账号ID
     * @param status          目标状态：1正常 2冻结
     */
    void changeSubAccountStatus(Long parentAccountId, Long subAccountId, Integer status);

    /**
     * 管理员条件查询商户企业列表
     *
     * @param auditState 审核状态
     * @param keyword    搜索关键词
     * @return 商户档案列表
     */
    List<MerchantArchive> listMerchants(Integer auditState, String keyword);
}
