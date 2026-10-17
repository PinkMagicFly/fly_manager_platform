package com.fly.platform.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 运费前置阶梯试算请求入参传输对象
 *
 * @author SoarCloud Team
 */
@Data
public class FeeCalculateDTO {

    /** 寄件地经度 */
    @NotNull(message = "寄件地经度不能为空")
    private Double senderLng;

    /** 寄件地纬度 */
    @NotNull(message = "寄件地纬度不能为空")
    private Double senderLat;

    /** 收件地经度 */
    @NotNull(message = "收件地经度不能为空")
    private Double receiverLng;

    /** 收件地纬度 */
    @NotNull(message = "收件地纬度不能为空")
    private Double receiverLat;

    /** 货物重量（千克） */
    @NotNull(message = "货物重量不能为空")
    @DecimalMin(value = "0.01", message = "货物重量必须大于0千克")
    private BigDecimal goodsWeight;

    /** 货物类别：1-普通文件 2-生物医疗急救 3-数码生鲜 4-高附加值工业件 */
    private Integer goodsType;

    /** 加急标记：0-标准时效 1-加急优先配送 */
    private Integer urgentFlag = 0;
}
