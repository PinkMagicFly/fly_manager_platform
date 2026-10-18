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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 跨领域事务强一致性与回滚守卫测试
 *
 * @author SoarCloud Team
 */
@SpringBootTest
@ActiveProfiles("test")
public class OrderScheduleTransactionalTest {

    @Autowired
    private CargoOrderMapper cargoOrderMapper;

    @Autowired
    private CargoOrderService cargoOrderService;

    @Test
    @DisplayName("跨领域事务一致性测试: 调度指派不存在的机组/无人机时异常阻断并强回滚，订单状态不发生篡改")
    void testScheduleTransactionalRollbackOnFailure() {
        // 创建待调度初始订单
        CargoOrder order = new CargoOrder();
        order.setOrderSn("ORDTX" + System.currentTimeMillis());
        order.setCustomerId(1L);
        order.setMerchantId(1L);
        order.setSenderAddr("测试起点");
        order.setReceiverAddr("测试终点");
        order.setSenderGeo("POINT(113.943 22.540)");
        order.setReceiverGeo("POINT(114.062 22.553)");
        order.setGoodsType(1);
        order.setGoodsWeight(new BigDecimal("2.00"));
        order.setFeeTotal(new BigDecimal("50.00"));
        order.setOrderState(OrderStateEnum.PENDING_DISPATCH.getCode()); // 30-待调度
        order.setPayState(1);
        order.initAudit();
        cargoOrderMapper.insertOrder(order);

        Long orderId = order.getOrderId();

        // 构造非法调度入参：指定不存在的机长ID (999999L)，触发业务异常
        OrderDispatchDTO invalidDTO = new OrderDispatchDTO();
        invalidDTO.setOrderId(orderId);
        invalidDTO.setUavId(1L);
        invalidDTO.setPilotId(999999L); // 不存在
        invalidDTO.setRouteLine("LINESTRING(113.943000 22.540000, 114.000000 22.545000, 114.062000 22.553000)");


        assertThatThrownBy(() -> cargoOrderService.dispatchOrder(invalidDTO, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");

        // 事务强回滚断言：订单状态依然是 30-待调度，未被脏更新为 40
        CargoOrder afterRollback = cargoOrderMapper.selectOrderById(orderId);
        assertThat(afterRollback.getOrderState()).isEqualTo(OrderStateEnum.PENDING_DISPATCH.getCode());
        assertThat(afterRollback.getUavId()).isNull();
        assertThat(afterRollback.getApplyId()).isNull();
    }
}
