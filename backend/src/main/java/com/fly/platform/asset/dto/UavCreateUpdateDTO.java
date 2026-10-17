package com.fly.platform.asset.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 无人机资产创建与修订参数
 *
 * @author SoarCloud Team
 */
@Data
public class UavCreateUpdateDTO {

    /** 机身设备唯一编号 */
    @NotBlank(message = "机身设备编号不可为空")
    private String uavSn;

    /** 无人机机型名称 */
    @NotBlank(message = "无人机机型不可为空")
    private String model;

    /** 所属物流商户企业ID */
    @NotNull(message = "所属商户不可为空")
    private Long merchantId;

    /** 最大额定载重（千克） */
    @NotNull(message = "额定载重不可为空")
    @DecimalMin(value = "0.1", message = "额定载重必须大于0")
    private BigDecimal maxLoad;

    /** 最大标称航程（千米） */
    @NotNull(message = "标称航程不可为空")
    @DecimalMin(value = "1.0", message = "标称航程必须大于0")
    private BigDecimal maxRangeKm;

    /** 当前电池剩余电量百分比 */
    private Integer batteryPct;

    /** 常驻基地场站ID */
    private Long homeSiteId;
}
