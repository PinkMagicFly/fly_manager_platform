package com.fly.platform.order.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 货运订单在线创建请求传输对象
 *
 * @author SoarCloud Team
 */
@Data
public class OrderCreateDTO {

    /** 承接商户企业ID（可选，默认指派平台认证商户） */
    private Long merchantId;

    /** 寄件详细地址文本 */
    @NotBlank(message = "寄件地址不能为空")
    private String senderAddr;

    /** 收件详细地址文本 */
    @NotBlank(message = "收件地址不能为空")
    private String receiverAddr;

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

    /** 货物类别：1-普通文件 2-生物医疗急救 3-数码生鲜 4-高附加值工业件 */
    @NotNull(message = "货物类别不能为空")
    private Integer goodsType;

    /** 货物重量（千克） */
    @NotNull(message = "货物重量不能为空")
    @DecimalMin(value = "0.01", message = "货物重量必须大于0千克")
    private BigDecimal goodsWeight;

    /** 加急标记：0-标准时效 1-加急优先配送 */
    private Integer urgentFlag = 0;

    /** 支付途径：1-在线微信/支付宝 2-企业月结扣款 */
    private Integer payWay = 1;

    /** 期望送达时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime expectArriveAt;
}
