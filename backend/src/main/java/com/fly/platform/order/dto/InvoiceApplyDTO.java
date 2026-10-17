package com.fly.platform.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 货运账单发票开具申请入参对象
 *
 * @author SoarCloud Team
 */
@Data
public class InvoiceApplyDTO {

    /** 目标对账单ID */
    @NotNull(message = "对账单ID不能为空")
    private Long billId;

    /** 发票抬头单位全称 */
    @NotBlank(message = "发票抬头单位全称不能为空")
    private String invoiceTitle;

    /** 纳税人识别号 / 统一社会信用代码 */
    @NotBlank(message = "纳税人识别号不能为空")
    private String taxNumber;

    /** 接收电子发票邮箱 */
    private String email;

    /** 开票备注 */
    private String remarks;
}
