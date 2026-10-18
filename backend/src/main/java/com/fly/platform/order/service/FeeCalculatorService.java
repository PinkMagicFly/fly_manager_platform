package com.fly.platform.order.service;

import com.fly.platform.common.exception.BusinessException;
import com.fly.platform.order.dto.FeeCalculateDTO;
import com.fly.platform.order.vo.FeeCalculateVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 运费前置试算服务
 * 实现阶梯里程、超重加价、空域通道费与加急系数（1.3倍）精确计算
 *
 * @author SoarCloud Team
 */
@Service
public class FeeCalculatorService {

    /** 地球平均半径（公里） */
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * 阶梯运费精确试算
     *
     * @param dto 试算入参传输对象
     * @return 试算明细与总额
     */
    public FeeCalculateVO calculateFee(FeeCalculateDTO dto) {
        if (dto == null) {
            throw new BusinessException(400, "试算入参不能为空");
        }
        if (dto.getGoodsWeight() == null || dto.getGoodsWeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "货物重量必须为大于0的正数");
        }
        if (dto.getSenderLng() == null || dto.getSenderLat() == null
                || dto.getReceiverLng() == null || dto.getReceiverLat() == null) {
            throw new BusinessException(400, "寄收件经纬度坐标不能为空");
        }

        // 1. 计算两点间大地球面直线航程（公里）
        double dist = calculateDistanceKm(dto.getSenderLng(), dto.getSenderLat(),
                dto.getReceiverLng(), dto.getReceiverLat());
        BigDecimal distanceKm = BigDecimal.valueOf(dist).setScale(2, RoundingMode.HALF_EVEN);

        // 2. 基础运费：首重2kg以内、首航程3km以内起步价25.00元
        // 超里程每公里加收 3.00 元；超重每千克加收 4.00 元
        BigDecimal base = new BigDecimal("25.00");
        if (distanceKm.compareTo(new BigDecimal("3.0")) > 0) {
            BigDecimal extraDist = distanceKm.subtract(new BigDecimal("3.0"));
            base = base.add(extraDist.multiply(new BigDecimal("3.00")));
        }
        if (dto.getGoodsWeight().compareTo(new BigDecimal("2.0")) > 0) {
            BigDecimal extraWeight = dto.getGoodsWeight().subtract(new BigDecimal("2.0"));
            base = base.add(extraWeight.multiply(new BigDecimal("4.00")));
        }
        BigDecimal feeBase = base.setScale(2, RoundingMode.HALF_EVEN);

        // 3. 空域通道费：根据货物类别与距离加权
        BigDecimal airspaceBase = new BigDecimal("15.00");
        if (dto.getGoodsType() != null && dto.getGoodsType() == 2) {
            // 生物医疗急救优先航道保障
            airspaceBase = new BigDecimal("20.00");
        }
        BigDecimal feeAirspace = airspaceBase.add(distanceKm.multiply(new BigDecimal("1.50")))
                .setScale(2, RoundingMode.HALF_EVEN);

        // 4. 加急优先配送附加费：若开启加急，加急费为 (基础运费 + 空域通道费) * 0.30（总系数1.3倍）
        BigDecimal feeUrgent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        if (dto.getUrgentFlag() != null && dto.getUrgentFlag() == 1) {
            feeUrgent = feeBase.add(feeAirspace).multiply(new BigDecimal("0.30"))
                    .setScale(2, RoundingMode.HALF_EVEN);
        }

        // 5. 满减优惠券抵扣：满100减15，满50减5
        BigDecimal subtotal = feeBase.add(feeAirspace).add(feeUrgent);
        BigDecimal feeDiscount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        if (subtotal.compareTo(new BigDecimal("100.00")) >= 0) {
            feeDiscount = new BigDecimal("15.00");
        } else if (subtotal.compareTo(new BigDecimal("50.00")) >= 0) {
            feeDiscount = new BigDecimal("5.00");
        }

        // 6. 应付总额 = 基础运费 + 空域费 + 加急附加费 - 优惠抵扣（保底0.01元）
        BigDecimal feeTotal = subtotal.subtract(feeDiscount);
        if (feeTotal.compareTo(new BigDecimal("0.01")) < 0) {
            feeTotal = new BigDecimal("0.01");
        }
        feeTotal = feeTotal.setScale(2, RoundingMode.HALF_EVEN);

        FeeCalculateVO vo = new FeeCalculateVO();
        vo.setDistanceKm(distanceKm);
        vo.setFeeBase(feeBase);
        vo.setFeeAirspace(feeAirspace);
        vo.setFeeUrgent(feeUrgent);
        vo.setFeeDiscount(feeDiscount);
        vo.setFeeTotal(feeTotal);
        vo.setFormulaDesc(String.format("航程%.2fkm(基础¥%s) + 空域费(¥%s) + 加急费(¥%s) - 优惠(¥%s) = 应付¥%s",
                distanceKm.doubleValue(), feeBase, feeAirspace, feeUrgent, feeDiscount, feeTotal));

        return vo;
    }

    /**
     * 大地球面距离计算 (Haversine Formula)，已抽取至 GeoUtils 公共复用
     */
    public static double calculateDistanceKm(double lon1, double lat1, double lon2, double lat2) {
        return com.fly.platform.common.util.GeoUtils.haversineKm(lon1, lat1, lon2, lat2);
    }

}
