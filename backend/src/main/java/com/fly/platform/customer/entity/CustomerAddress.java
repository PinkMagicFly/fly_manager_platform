package com.fly.platform.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fly.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 客户常用地址簿实体
 *
 * @author SoarCloud Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_address")
public class CustomerAddress extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 地址ID主键自增 */
    @TableId(value = "address_id", type = IdType.AUTO)
    private Long addressId;

    /** 归属客户账户ID (uam_account.account_id) */
    @TableField("customer_id")
    private Long customerId;

    /** 联系人姓名 */
    @TableField("contact_name")
    private String contactName;

    /** 联系人手机号码 */
    @TableField("contact_phone")
    private String contactPhone;

    /** 省份 */
    @TableField("province")
    private String province;

    /** 城市 */
    @TableField("city")
    private String city;

    /** 区县 */
    @TableField("district")
    private String district;

    /** 街道/乡镇 */
    @TableField("street")
    private String street;

    /** 地图选点经度 */
    @TableField("lng")
    private BigDecimal lng;

    /** 地图选点纬度 */
    @TableField("lat")
    private BigDecimal lat;

    /** 详细门牌/楼栋地址 */
    @TableField("detail")
    private String detail;

    /** 是否默认地址：0-否 1-是 */
    @TableField("is_default")
    private Integer isDefault;
}
