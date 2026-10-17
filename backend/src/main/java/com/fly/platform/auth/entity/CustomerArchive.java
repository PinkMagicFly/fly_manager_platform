package com.fly.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fly.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 客户实名档案实体
 *
 * @author SoarCloud Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_archive")
public class CustomerArchive extends BaseEntity {

    /** 客户ID */
    @TableId(type = IdType.AUTO)
    private Long customerId;

    /** 关联账户ID */
    private Long accountId;

    /** 客户真实姓名/企业名称 */
    private String realName;

    /** 身份证号/统一信用代码 */
    private String idCard;

    /** 月结信用额度 */
    private BigDecimal creditLimit;

    /** 实名认证状态：0-未认证 1-已认证 2-认证失败 */
    private Integer authState;
}
