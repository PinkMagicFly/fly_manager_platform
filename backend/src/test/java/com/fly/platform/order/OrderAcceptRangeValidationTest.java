package com.fly.platform.order;

import com.fly.platform.common.exception.BusinessException;
import com.fly.platform.order.entity.CargoOrder;
import com.fly.platform.order.entity.OrderStateFlow;
import com.fly.platform.order.enums.OrderStateEnum;
import com.fly.platform.order.mapper.CargoOrderMapper;
import com.fly.platform.order.mapper.OrderStateFlowMapper;
import com.fly.platform.order.service.CargoOrderService;
import com.fly.platform.order.vo.CargoOrderVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("集成测试: 商户接单场站服务范围前置守卫与拒单机制")
public class OrderAcceptRangeValidationTest {

    @Autowired
    private CargoOrderService cargoOrderService;

    @Autowired
    private CargoOrderMapper cargoOrderMapper;

    @Autowired
    private OrderStateFlowMapper orderStateFlowMapper;

    private CargoOrder createBaseOrder(String suffix, double senderLng, double senderLat, double receiverLng, double receiverLat) {
        CargoOrder order = new CargoOrder();
        order.setOrderSn("ACC" + suffix);
        order.setCustomerId(1L);
        order.setMerchantId(1L);
        order.setSenderAddr("测试寄件");
        order.setReceiverAddr("测试收件");
        order.setSenderGeo(String.format("POINT(%.6f %.6f)", senderLng, senderLat));
        order.setReceiverGeo(String.format("POINT(%.6f %.6f)", receiverLng, receiverLat));
        order.setGoodsType(1);
        order.setGoodsWeight(new BigDecimal("5.00"));
        order.setFeeTotal(new BigDecimal("80.00"));
        order.setOrderState(OrderStateEnum.PENDING_ACCEPT.getCode()); // 20-待接单
        order.setPayState(1);
        order.initAudit();
        cargoOrderMapper.insertOrder(order);
        return order;
    }

    @Test
    @DisplayName("测试1：运单起止点超出起降场站服务覆盖半径，商户接单必须被前置拦截阻断")
    public void testAcceptOrderOutOfRangeBlocked() {
        // 远离任何场站的偏远坐标
        CargoOrder outRangeOrder = createBaseOrder("OUT01", 113.100, 23.100, 114.062, 22.553);

        assertThatThrownBy(() -> cargoOrderService.acceptOrder(outRangeOrder.getOrderId(), 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("超出任何起降场站服务覆盖范围")
                .hasMessageContaining("请执行拒单");

        // 订单依然保留在 20 待接单
        CargoOrder after = cargoOrderMapper.selectOrderById(outRangeOrder.getOrderId());
        assertThat(after.getOrderState()).isEqualTo(OrderStateEnum.PENDING_ACCEPT.getCode());
    }

    @Test
    @DisplayName("测试2：超范围订单执行商户拒单并填写原因，状态转为70已取消且流水留痕")
    public void testRejectOrderOutOfRangeSuccess() {
        CargoOrder outRangeOrder = createBaseOrder("REJ01", 113.100, 23.100, 114.062, 22.553);

        CargoOrder rejected = cargoOrderService.cancelOrder(outRangeOrder.getOrderId(), 1L, "商户拒绝接单: 寄件地址超出南山与福田场站服务覆盖半径");
        assertThat(rejected.getOrderState()).isEqualTo(OrderStateEnum.CANCELLED.getCode());

        List<OrderStateFlow> flows = orderStateFlowMapper.selectByOrderId(outRangeOrder.getOrderId());
        assertThat(flows).isNotEmpty();
        assertThat(flows.get(flows.size() - 1).getRemark()).contains("超出南山与福田场站服务覆盖半径");

        CargoOrderVO vo = cargoOrderService.getOrderDetail(outRangeOrder.getOrderId());
        assertThat(vo.getCancelReason()).contains("超出南山与福田场站服务覆盖半径");
    }

    @Test
    @DisplayName("测试3：合规场站范围内订单正常接单推进至30待调度")
    public void testAcceptCompliantOrderSuccess() {
        // 南山科技园 (113.943, 22.540) -> 福田深业上城 (114.062, 22.553)，均在 5000 米服务半径内
        CargoOrder compliantOrder = createBaseOrder("OK01", 113.943, 22.540, 114.062, 22.553);

        CargoOrder accepted = cargoOrderService.acceptOrder(compliantOrder.getOrderId(), 1L);
        assertThat(accepted.getOrderState()).isEqualTo(OrderStateEnum.PENDING_DISPATCH.getCode());
    }
}
