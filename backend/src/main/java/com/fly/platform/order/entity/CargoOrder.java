package com.fly.platform.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fly.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 低空货运订单核心业务实体
 *
 * @author SoarCloud Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cargo_order")
public class CargoOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 订单唯一标识（主键） */
    @TableId(type = IdType.AUTO)
    private Long orderId;

    /** 业务订单流水号 */
    private String orderSn;

    /** 下单客户档案ID */
    private Long customerId;

    /** 承接履约商户企业ID */
    private Long merchantId;

    /** 寄件地址文本 */
    private String senderAddr;

    /** 收件地址文本 */
    private String receiverAddr;

    /** 寄件地理经纬度坐标 (WKT格式: POINT(lng lat)) */
    private String senderGeo;

    /** 收件地理经纬度坐标 (WKT格式: POINT(lng lat)) */
    private String receiverGeo;

    /** 货物类别：1-普通文件 2-生物医疗急救 3-数码生鲜 4-高附加值工业件 */
    private Integer goodsType;

    /** 货物重量（千克） */
    private BigDecimal goodsWeight;

    /** 加急标记：0-标准时效 1-加急优先配送 */
    private Integer urgentFlag = 0;

    /** 基础运费（元） */
    private BigDecimal feeBase = BigDecimal.ZERO;

    /** 空域通道占用费（元） */
    private BigDecimal feeAirspace = BigDecimal.ZERO;

    /** 加急调度附加费（元） */
    private BigDecimal feeUrgent = BigDecimal.ZERO;

    /** 满减优惠券抵扣金额（元） */
    private BigDecimal feeDiscount = BigDecimal.ZERO;

    /** 应付总金额（元） */
    private BigDecimal feeTotal = BigDecimal.ZERO;

    /** 支付途径：1-在线微信/支付宝 2-企业月结扣款 */
    private Integer payWay = 1;

    /** 支付状态：0-未支付 1-已支付 2-已退款 */
    private Integer payState = 0;

    /** 订单九态：10-待支付 20-待接单 30-待调度 40-待起飞 50-飞行中 60-已送达 70-已取消 80-异常中断 */
    private Integer orderState = 10;

    /** 执飞无人机资产ID */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long uavId;

    /** 关联飞行申报单ID */
    private Long applyId;

    /** 期望最晚送达时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime expectArriveAt;

    /** 解析寄件经度 */
    public Double getSenderLng() {
        return parsePointCoord(senderGeo, 0);
    }

    /** 解析寄件纬度 */
    public Double getSenderLat() {
        return parsePointCoord(senderGeo, 1);
    }

    /** 解析收件经度 */
    public Double getReceiverLng() {
        return parsePointCoord(receiverGeo, 0);
    }

    /** 解析收件纬度 */
    public Double getReceiverLat() {
        return parsePointCoord(receiverGeo, 1);
    }

    private Double parsePointCoord(String wkt, int index) {
        if (wkt == null || wkt.trim().isEmpty()) {
            return null;
        }
        try {
            String clean = wkt.toUpperCase().replace("POINT(", "").replace("POINT (", "").replace(")", "").trim();
            String[] parts = clean.contains(",") ? clean.split(",") : clean.split("\\s+");
            if (parts.length >= 2) {
                return Double.parseDouble(parts[index].trim());
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
