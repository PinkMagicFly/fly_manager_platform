package com.fly.platform.order.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 低空货运订单核心业务展现视图对象
 *
 * @author SoarCloud Team
 */
@Data
public class CargoOrderVO {

    /** 订单唯一标识 */
    private Long orderId;

    /** 业务订单流水号 */
    private String orderSn;

    /** 下单客户档案ID */
    private Long customerId;

    /** 客户企业/个人真实名称 */
    private String customerName;

    /** 承接商户企业ID */
    private Long merchantId;

    /** 承接商户企业名称 */
    private String merchantName;

    /** 寄件详细地址 */
    private String senderAddr;

    /** 收件详细地址 */
    private String receiverAddr;

    /** 寄件经度 */
    private Double senderLng;

    /** 寄件纬度 */
    private Double senderLat;

    /** 收件经度 */
    private Double receiverLng;

    /** 收件纬度 */
    private Double receiverLat;

    /** 货物类别编码 */
    private Integer goodsType;

    /** 货物类别描述 */
    private String goodsTypeDesc;

    /** 货物重量（千克） */
    private BigDecimal goodsWeight;

    /** 加急标记：0-标准时效 1-加急优先 */
    private Integer urgentFlag;

    /** 基础运费 */
    private BigDecimal feeBase;

    /** 空域通道费 */
    private BigDecimal feeAirspace;

    /** 加急附加费 */
    private BigDecimal feeUrgent;

    /** 优惠抵扣金额 */
    private BigDecimal feeDiscount;

    /** 应付总费用 */
    private BigDecimal feeTotal;

    /** 支付途径 */
    private Integer payWay;

    /** 支付途径描述 */
    private String payWayDesc;

    /** 支付状态 */
    private Integer payState;

    /** 支付状态描述 */
    private String payStateDesc;

    /** 订单九态状态编码 */
    private Integer orderState;

    /** 订单九态状态中文描述 */
    private String orderStateDesc;

    /** 执飞无人机资产ID */
    private Long uavId;

    /** 执飞无人机编号 (SN) */
    private String uavSn;

    /** 执飞无人机机型 */
    private String uavModel;

    /** 执飞机长ID */
    private Long pilotId;

    /** 执飞机长姓名 */
    private String pilotName;

    /** 关联飞行申报单ID */
    private Long applyId;

    /** 关联飞行申报单流水号 */
    private String applySn;

    /** 申报审批状态 */
    private Integer applyApproveState;

    /** 期望最晚送达时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime expectArriveAt;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
