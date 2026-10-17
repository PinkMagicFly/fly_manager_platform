package com.fly.platform.order;

import com.fly.platform.order.domain.CargoOrderStateMachine;
import com.fly.platform.order.entity.CargoOrder;
import com.fly.platform.order.entity.OrderStateFlow;
import com.fly.platform.order.enums.OrderStateEnum;
import com.fly.platform.order.mapper.CargoOrderMapper;
import com.fly.platform.order.mapper.OrderStateFlowMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 订单状态变迁流水强审计强约束测试
 *
 * @author SoarCloud Team
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OrderStateFlowAuditTest {

    @Autowired
    private CargoOrderMapper cargoOrderMapper;

    @Autowired
    private OrderStateFlowMapper orderStateFlowMapper;

    @Autowired
    private CargoOrderStateMachine stateMachine;

    @Test
    @DisplayName("状态流转原子记入流水表测试: 校验原状态、目标状态、操作人与时间戳完全匹配")
    void testStateFlowAuditRecordedAtomically() {
        // 创建测试订单
        CargoOrder order = new CargoOrder();
        order.setOrderSn("ORDA" + System.currentTimeMillis());
        order.setCustomerId(1L);
        order.setMerchantId(1L);
        order.setSenderAddr("深圳湾科技生态园");
        order.setReceiverAddr("深业上城");
        order.setSenderGeo("POINT(113.952 22.532)");
        order.setReceiverGeo("POINT(114.062 22.553)");
        order.setGoodsType(1);
        order.setGoodsWeight(new BigDecimal("3.50"));
        order.setFeeTotal(new BigDecimal("68.00"));
        order.setOrderState(OrderStateEnum.PENDING_PAY.getCode());
        order.setPayState(0);
        order.initAudit();
        cargoOrderMapper.insertOrder(order);

        Long orderId = order.getOrderId();
        assertThat(orderId).isNotNull();

        // 驱动状态机 10 -> 20
        OrderStateFlow flow1 = stateMachine.transition(order, OrderStateEnum.PENDING_ACCEPT.getCode(),
                10086L, "测试客户完成支付", orderStateFlowMapper);
        cargoOrderMapper.updateOrderState(orderId, order.getOrderState(), 1);

        assertThat(flow1).isNotNull();
        assertThat(flow1.getFlowId()).isNotNull();
        assertThat(flow1.getOrderId()).isEqualTo(orderId);
        assertThat(flow1.getFromState()).isEqualTo(OrderStateEnum.PENDING_PAY.getCode());
        assertThat(flow1.getToState()).isEqualTo(OrderStateEnum.PENDING_ACCEPT.getCode());
        assertThat(flow1.getOperatorId()).isEqualTo(10086L);
        assertThat(flow1.getRemark()).contains("测试客户完成支付");
        assertThat(flow1.getCreatedAt()).isNotNull();

        // 驱动状态机 20 -> 30
        OrderStateFlow flow2 = stateMachine.transition(order, OrderStateEnum.PENDING_DISPATCH.getCode(),
                20001L, "商户接单入池", orderStateFlowMapper);
        cargoOrderMapper.updateOrderState(orderId, order.getOrderState(), null);

        // 从数据库查询该订单全量流水
        List<OrderStateFlow> list = orderStateFlowMapper.selectByOrderId(orderId);
        assertThat(list).hasSize(2);
        assertThat(list.get(0).getFromState()).isEqualTo(10);
        assertThat(list.get(0).getToState()).isEqualTo(20);
        assertThat(list.get(1).getFromState()).isEqualTo(20);
        assertThat(list.get(1).getToState()).isEqualTo(30);
    }
}
