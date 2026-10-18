package com.fly.platform.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fly.platform.order.entity.CargoOrder;
import com.fly.platform.order.entity.OrderStateFlow;
import com.fly.platform.order.enums.OrderStateEnum;
import com.fly.platform.order.mapper.CargoOrderMapper;
import com.fly.platform.order.mapper.OrderStateFlowMapper;
import com.fly.platform.order.task.OrderTimeoutTask;
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
 * 订单生命周期超时取消任务单元测试
 * 覆盖待支付超时自动关闭与待接单超时自动取消
 *
 * @author SoarCloud Team
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OrderTimeoutTaskTest {

    @Autowired
    private OrderTimeoutTask orderTimeoutTask;

    @Autowired
    private CargoOrderMapper cargoOrderMapper;

    @Autowired
    private OrderStateFlowMapper orderStateFlowMapper;

    @Test
    @DisplayName("超时关单测试: 待支付超时与待接单超时订单自动跃迁为已取消70且落流水审计")
    void testTimeoutCancelFlow() {
        // 1. 造待支付超时的订单
        CargoOrder payTimeoutOrder = new CargoOrder();
        payTimeoutOrder.setOrderSn("ORD_TOPAY_" + (System.currentTimeMillis() % 1000000000L));
        payTimeoutOrder.setCustomerId(1L);
        payTimeoutOrder.setMerchantId(1L);
        payTimeoutOrder.setSenderAddr("深圳湾科技生态园");
        payTimeoutOrder.setReceiverAddr("深业上城");
        payTimeoutOrder.setSenderGeo("POINT(113.952 22.532)");
        payTimeoutOrder.setReceiverGeo("POINT(114.062 22.553)");
        payTimeoutOrder.setGoodsType(1);
        payTimeoutOrder.setGoodsWeight(new BigDecimal("2.5"));
        payTimeoutOrder.setFeeTotal(new BigDecimal("50.00"));
        payTimeoutOrder.setOrderState(OrderStateEnum.PENDING_PAY.getCode());
        payTimeoutOrder.setCreatedAt(LocalDateTime.now().minusMinutes(45)); // 45分钟前
        payTimeoutOrder.setUpdatedAt(LocalDateTime.now().minusMinutes(45));
        payTimeoutOrder.setIsDeleted(0);
        cargoOrderMapper.insert(payTimeoutOrder);

        // 2. 造待接单超时的订单
        CargoOrder acceptTimeoutOrder = new CargoOrder();
        acceptTimeoutOrder.setOrderSn("ORD_TOACC_" + (System.currentTimeMillis() % 1000000000L));
        acceptTimeoutOrder.setCustomerId(1L);
        acceptTimeoutOrder.setMerchantId(1L);
        acceptTimeoutOrder.setSenderAddr("深圳湾科技生态园");
        acceptTimeoutOrder.setReceiverAddr("深业上城");
        acceptTimeoutOrder.setSenderGeo("POINT(113.952 22.532)");
        acceptTimeoutOrder.setReceiverGeo("POINT(114.062 22.553)");
        acceptTimeoutOrder.setGoodsType(1);
        acceptTimeoutOrder.setGoodsWeight(new BigDecimal("3.0"));
        acceptTimeoutOrder.setFeeTotal(new BigDecimal("60.00"));
        acceptTimeoutOrder.setOrderState(OrderStateEnum.PENDING_ACCEPT.getCode());
        acceptTimeoutOrder.setCreatedAt(LocalDateTime.now().minusMinutes(80));
        acceptTimeoutOrder.setUpdatedAt(LocalDateTime.now().minusMinutes(75)); // 75分钟前
        acceptTimeoutOrder.setIsDeleted(0);
        cargoOrderMapper.insert(acceptTimeoutOrder);

        // 3. 执行超时任务
        int cancelled = orderTimeoutTask.checkAndCancelTimeoutOrders();
        assertThat(cancelled).isGreaterThanOrEqualTo(2);

        // 4. 断言订单状态变迁为 70 (CANCELLED)
        CargoOrder updatedPayOrder = cargoOrderMapper.selectById(payTimeoutOrder.getOrderId());
        assertThat(updatedPayOrder.getOrderState()).isEqualTo(OrderStateEnum.CANCELLED.getCode());

        CargoOrder updatedAcceptOrder = cargoOrderMapper.selectById(acceptTimeoutOrder.getOrderId());
        assertThat(updatedAcceptOrder.getOrderState()).isEqualTo(OrderStateEnum.CANCELLED.getCode());

        // 5. 断言流水记录
        List<OrderStateFlow> flows1 = orderStateFlowMapper.selectList(
                new LambdaQueryWrapper<OrderStateFlow>().eq(OrderStateFlow::getOrderId, payTimeoutOrder.getOrderId())
        );
        assertThat(flows1).isNotEmpty();
        assertThat(flows1.get(0).getToState()).isEqualTo(OrderStateEnum.CANCELLED.getCode());
        assertThat(flows1.get(0).getRemark()).contains("超时自动取消");

        List<OrderStateFlow> flows2 = orderStateFlowMapper.selectList(
                new LambdaQueryWrapper<OrderStateFlow>().eq(OrderStateFlow::getOrderId, acceptTimeoutOrder.getOrderId())
        );
        assertThat(flows2).isNotEmpty();
        assertThat(flows2.get(0).getToState()).isEqualTo(OrderStateEnum.CANCELLED.getCode());
        assertThat(flows2.get(0).getRemark()).contains("超时自动取消");
    }
}
