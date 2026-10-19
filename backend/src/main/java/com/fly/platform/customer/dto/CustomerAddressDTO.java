package com.fly.platform.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 客户常用地址新增与修改传输对象
 *
 * @author SoarCloud Team
 */
@Data
public class CustomerAddressDTO {

    /** 联系人姓名 */
    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;

    /** 联系电话 */
    @NotBlank(message = "联系电话不能为空")
    private String contactPhone;

    /** 省份 */
    @NotBlank(message = "省份不能为空")
    private String province;

    /** 城市 */
    @NotBlank(message = "城市不能为空")
    private String city;

    /** 区县 */
    @NotBlank(message = "区县不能为空")
    private String district;

    /** 街道 */
    @NotBlank(message = "街道不能为空")
    private String street;

    /** 选点经度 */
    @NotNull(message = "选点经度不能为空")
    private BigDecimal lng;

    /** 选点纬度 */
    @NotNull(message = "选点纬度不能为空")
    private BigDecimal lat;

    /** 楼栋门牌详细地址 */
    @NotBlank(message = "详细地址不能为空")
    private String detail;

    /** 是否默认：0-否 1-是 */
    private Integer isDefault;
}
