package com.fly.platform.order;

import com.fly.platform.common.exception.BusinessException;
import com.fly.platform.order.dto.FeeCalculateDTO;
import com.fly.platform.order.service.FeeCalculatorService;
import com.fly.platform.order.vo.FeeCalculateVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 运费前置多维阶梯试算单元测试
 *
 * @author SoarCloud Team
 */
public class FeeCalculatorServiceTest {

    private final FeeCalculatorService calculatorService = new FeeCalculatorService();

    @Test
    @DisplayName("运费试算基准测试: 基础运费 + 空域使用费 + 加急费 - 满减抵扣，精度为2位小数")
    void testFeeCalculationStandardAndUrgent() {
        FeeCalculateDTO dto = new FeeCalculateDTO();
        dto.setSenderLng(113.943);
        dto.setSenderLat(22.540);
        dto.setReceiverLng(114.062);
        dto.setReceiverLat(22.553);
        dto.setGoodsWeight(new BigDecimal("5.00")); // 5kg
        dto.setGoodsType(1);
        dto.setUrgentFlag(1); // 加急

        FeeCalculateVO vo = calculatorService.calculateFee(dto);

        assertThat(vo).isNotNull();
        assertThat(vo.getDistanceKm()).isGreaterThan(BigDecimal.ZERO);
        assertThat(vo.getFeeBase()).isGreaterThan(BigDecimal.ZERO);
        assertThat(vo.getFeeAirspace()).isGreaterThan(BigDecimal.ZERO);
        assertThat(vo.getFeeUrgent()).isGreaterThan(BigDecimal.ZERO);
        assertThat(vo.getFeeTotal()).isGreaterThan(BigDecimal.ZERO);

        // 校验2位小数精度
        assertThat(vo.getFeeBase().scale()).isEqualTo(2);
        assertThat(vo.getFeeAirspace().scale()).isEqualTo(2);
        assertThat(vo.getFeeUrgent().scale()).isEqualTo(2);
        assertThat(vo.getFeeTotal().scale()).isEqualTo(2);

        // 校验公式关系: Total = Base + Airspace + Urgent - Discount
        BigDecimal expectedTotal = vo.getFeeBase().add(vo.getFeeAirspace()).add(vo.getFeeUrgent()).subtract(vo.getFeeDiscount());
        assertThat(vo.getFeeTotal()).isEqualByComparingTo(expectedTotal);
    }

    @Test
    @DisplayName("非法负数与零重量拦截测试: 必须抛出业务异常")
    void testIllegalWeightIntercepted() {
        FeeCalculateDTO negativeDTO = new FeeCalculateDTO();
        negativeDTO.setSenderLng(113.943);
        negativeDTO.setSenderLat(22.540);
        negativeDTO.setReceiverLng(114.062);
        negativeDTO.setReceiverLat(22.553);
        negativeDTO.setGoodsWeight(new BigDecimal("-2.50"));

        assertThatThrownBy(() -> calculatorService.calculateFee(negativeDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("货物重量必须为大于0的正数");

        FeeCalculateDTO zeroDTO = new FeeCalculateDTO();
        zeroDTO.setSenderLng(113.943);
        zeroDTO.setSenderLat(22.540);
        zeroDTO.setReceiverLng(114.062);
        zeroDTO.setReceiverLat(22.553);
        zeroDTO.setGoodsWeight(BigDecimal.ZERO);

        assertThatThrownBy(() -> calculatorService.calculateFee(zeroDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("货物重量必须为大于0的正数");
    }
}
