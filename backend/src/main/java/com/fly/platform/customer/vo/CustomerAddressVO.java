package com.fly.platform.customer.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户常用地址视图对象
 *
 * @author SoarCloud Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAddressVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long addressId;

    private Long customerId;

    private String contactName;

    private String contactPhone;

    private String province;

    private String city;

    private String district;

    private String street;

    /** 组合省市区街道文本，方便前端一键显示 */
    private String fullRegionText;

    private BigDecimal lng;

    private BigDecimal lat;

    private String detail;

    /** 组合完整地址（省市区街道 + 门牌号） */
    private String fullAddress;

    private Integer isDefault;

    private LocalDateTime createdAt;
}
