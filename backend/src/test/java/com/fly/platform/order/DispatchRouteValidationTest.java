package com.fly.platform.order;

import com.fly.platform.common.exception.BusinessException;
import com.fly.platform.order.dto.OrderDispatchDTO;
import com.fly.platform.order.entity.CargoOrder;
import com.fly.platform.order.enums.OrderStateEnum;
import com.fly.platform.order.mapper.CargoOrderMapper;
import com.fly.platform.order.service.CargoOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("集成测试: 调度航线模型与空间几何边界校验")
public class DispatchRouteValidationTest {

    @Autowired
    private CargoOrderService cargoOrderService;

    @Autowired
    private CargoOrderMapper cargoOrderMapper;

    private CargoOrder createBaseOrder(String suffix) {
        CargoOrder order = new CargoOrder();
        order.setOrderSn("VALTEST" + suffix);
        order.setCustomerId(1L);
        order.setMerchantId(1L);
        // 南山科技园 (113.943, 22.540) -> 福田深业上城 (114.062, 22.553)
        order.setSenderAddr("深圳南山科技园枢纽站");
        order.setReceiverAddr("深圳福田深业上城");
        order.setSenderGeo("POINT(113.943000 22.540000)");
        order.setReceiverGeo("POINT(114.062000 22.553000)");
        order.setGoodsType(1);
        order.setGoodsWeight(new BigDecimal("5.00"));
        order.setFeeTotal(new BigDecimal("80.00"));
        order.setOrderState(OrderStateEnum.PENDING_DISPATCH.getCode());
        order.setPayState(1);
        order.initAudit();
        cargoOrderMapper.insertOrder(order);
        return order;
    }

    @Test
    @DisplayName("校验拦截1：航线起点偏离起飞场站超过50米，断言 400 业务拦截")
    public void testStartPointDeviatesFromSite() {
        CargoOrder order = createBaseOrder("DEV01");

        OrderDispatchDTO dto = new OrderDispatchDTO();
        dto.setOrderId(order.getOrderId());
        dto.setUavId(2L);
        dto.setPilotId(1L);
        dto.setAltPlanned(100);
        dto.setStartAt(LocalDateTime.now().plusDays(1));
        dto.setEndAt(LocalDateTime.now().plusDays(1).plusHours(1));
        // 起点偏离南山场站约 1.5km
        dto.setRouteLine("LINESTRING(113.930000 22.540000, 114.000000 22.545000, 114.062000 22.553000)");

        assertThatThrownBy(() -> cargoOrderService.dispatchOrder(dto, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("航线起点偏离起飞场站")
                .hasMessageContaining("超过50米");
    }

    @Test
    @DisplayName("校验拦截2：航线终点偏离降落场站超过50米，断言 400 业务拦截")
    public void testEndPointDeviatesFromSite() {
        CargoOrder order = createBaseOrder("DEV02");

        OrderDispatchDTO dto = new OrderDispatchDTO();
        dto.setOrderId(order.getOrderId());
        dto.setUavId(2L);
        dto.setPilotId(1L);
        dto.setAltPlanned(100);
        dto.setStartAt(LocalDateTime.now().plusDays(1));
        dto.setEndAt(LocalDateTime.now().plusDays(1).plusHours(1));
        // 终点偏离福田场站约 2km
        dto.setRouteLine("LINESTRING(113.943000 22.540000, 114.000000 22.545000, 114.080000 22.553000)");

        assertThatThrownBy(() -> cargoOrderService.dispatchOrder(dto, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("航线终点偏离降落场站")
                .hasMessageContaining("超过50米");
    }

    @Test
    @DisplayName("校验拦截3：航线穿越宝安机场禁飞区，断言 400 禁飞区合规拦截")
    public void testRoutePassesThroughForbiddenZone() {
        CargoOrder order = createBaseOrder("DEV03");

        OrderDispatchDTO dto = new OrderDispatchDTO();
        dto.setOrderId(order.getOrderId());
        dto.setUavId(2L);
        dto.setPilotId(1L);
        dto.setAltPlanned(100);
        dto.setStartAt(LocalDateTime.now().plusDays(1));
        dto.setEndAt(LocalDateTime.now().plusDays(1).plusHours(1));
        // 中间途经点强行穿过宝安机场净空禁飞区核心网格
        dto.setRouteLine("LINESTRING(113.943000 22.540000, 113.810000 22.630000, 114.062000 22.553000)");

        assertThatThrownBy(() -> cargoOrderService.dispatchOrder(dto, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("航线空域合规校验未通过")
                .hasMessageContaining("禁飞区");
    }

    @Test
    @DisplayName("校验放行4：首末场站精准锚定且折线合规，断言调度成功并保留待调度30")
    public void testCompliantRouteSuccess() {
        CargoOrder order = createBaseOrder("DEV04");

        OrderDispatchDTO dto = new OrderDispatchDTO();
        dto.setOrderId(order.getOrderId());
        dto.setUavId(2L);
        dto.setPilotId(1L);
        dto.setAltPlanned(100);
        dto.setStartAt(LocalDateTime.now().plusDays(1));
        dto.setEndAt(LocalDateTime.now().plusDays(1).plusHours(1));
        // 合规多点折线（3个航路点）
        dto.setRouteLine("LINESTRING(113.943000 22.540000, 114.000000 22.545000, 114.062000 22.553000)");

        CargoOrder dispatched = cargoOrderService.dispatchOrder(dto, 1L);
        assertThat(dispatched).isNotNull();
        assertThat(dispatched.getOrderState()).isEqualTo(OrderStateEnum.PENDING_DISPATCH.getCode());
        assertThat(dispatched.getApplyId()).isNotNull();
    }
}
