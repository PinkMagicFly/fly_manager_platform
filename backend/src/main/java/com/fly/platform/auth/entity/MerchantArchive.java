package com.fly.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fly.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 商户企业资质档案实体
 *
 * @author SoarCloud Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_archive")
public class MerchantArchive extends BaseEntity {

    /** 商户ID (自增主键) */
    @TableId(type = IdType.AUTO)
    private Long merchantId;

    /** 关联登录账户ID */
    private Long accountId;

    /** 企业全称 */
    private String corpName;

    /** 统一社会信用代码/营业执照号 */
    private String licenseNo;

    /** 联系人真实姓名 */
    private String contactName;

    /** 联系人手机号 */
    private String contactPhone;

    /** 适航许可证书编号 */
    private String airworthinessCert;

    /** 审批状态：0-待审核 1-已通过 2-已驳回 */
    private Integer auditState;

    /** 审核批注/驳回原因 */
    private String auditNote;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime auditAt;

    /** 审核人管理员ID */
    private Long auditorId;
}
