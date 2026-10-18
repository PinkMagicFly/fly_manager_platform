package com.fly.platform.order;

import com.fly.platform.common.exception.BusinessException;
import com.fly.platform.order.dto.FeeCalculateDTO;
import com.fly.platform.order.service.FeeCalculatorService;
import com.fly.platform.order.vo.FeeCalculateVO;
import com.fly.platform.sysparam.service.SysParamService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 运费前置多维阶梯试算单元测试
 * 覆盖阶梯计算、校验守卫与系统参数动态热生效验证
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
    @DisplayName("系统参数热生效测试: 动态调整起步价与公里单价后试算金额热变化")
    void testDynamicSysParamFeeCalculation() {
        SysParamService mockParamService = Mockito.mock(SysParamService.class);
        FeeCalculatorService dynamicCalculator = new FeeCalculatorService(mockParamService);

        FeeCalculateDTO dto = new FeeCalculateDTO();
        dto.setSenderLng(113.943);
        dto.setSenderLat(22.540);
        dto.setReceiverLng(114.062);
        dto.setReceiverLat(22.553);
        dto.setGoodsWeight(new BigDecimal("2.00")); // 2kg以内部超重
        dto.setGoodsType(1);
        dto.setUrgentFlag(0);

        // 模拟基础起步价为 25.00
        when(mockParamService.getDecimal(eq("fee.base_start"), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("25.00"));
        when(mockParamService.getDecimal(eq("fee.per_km"), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("3.00"));
        when(mockParamService.getDecimal(eq("fee.urgent_ratio"), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("1.30"));

        FeeCalculateVO vo1 = dynamicCalculator.calculateFee(dto);

        // 修改参数：起步价调为 50.00，每公里调为 6.00
        when(mockParamService.getDecimal(eq("fee.base_start"), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("50.00"));
        when(mockParamService.getDecimal(eq("fee.per_km"), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("6.00"));

        FeeCalculateVO vo2 = dynamicCalculator.calculateFee(dto);

        // 断言调整参数后基础运费和总额显著提高
        assertThat(vo2.getFeeBase()).isGreaterThan(vo1.getFeeBase());
        assertThat(vo2.getFeeTotal()).isGreaterThan(vo1.getFeeTotal());
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

