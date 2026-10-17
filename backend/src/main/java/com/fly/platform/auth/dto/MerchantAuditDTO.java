package com.fly.platform.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员审批商户入驻 DTO
 *
 * @author SoarCloud Team
 */
@Data
public class MerchantAuditDTO {

    /** 商户档案ID */
    @NotNull(message = "商户ID不能为空")
    private Long merchantId;

    /** 审核状态：1-审核通过 2-审核驳回 */
    @NotNull(message = "审批结论不能为空")
    private Integer auditState;

    /** 审批意见或驳回说明 */
    private String auditNote;
}
