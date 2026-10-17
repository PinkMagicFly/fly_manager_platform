package com.fly.platform.order;

import com.fly.platform.order.domain.CargoOrderStateMachine;
import com.fly.platform.order.domain.OrderStateTransitionException;
import com.fly.platform.order.enums.OrderStateEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 订单九态流转状态机驱动守卫单元测试
 *
 * @author SoarCloud Team
 */
public class CargoOrderStateMachineTest {

    @Test
    @DisplayName("九态正向跃迁链路测试: 10待支付 -> 20待接单 -> 30待调度 -> 40待起飞 -> 50飞行中 -> 60已送达")
    void testForwardTransitions() {
        // 10 -> 20 支付成功
        assertThatCode(() -> CargoOrderStateMachine.validateTransition(
                OrderStateEnum.PENDING_PAY.getCode(), OrderStateEnum.PENDING_ACCEPT.getCode()))
                .doesNotThrowAnyException();

        // 20 -> 30 商户接单
        assertThatCode(() -> CargoOrderStateMachine.validateTransition(
                OrderStateEnum.PENDING_ACCEPT.getCode(), OrderStateEnum.PENDING_DISPATCH.getCode()))
                .doesNotThrowAnyException();

        // 30 -> 40 调度派单
        assertThatCode(() -> CargoOrderStateMachine.validateTransition(
                OrderStateEnum.PENDING_DISPATCH.getCode(), OrderStateEnum.PENDING_TAKEOFF.getCode()))
                .doesNotThrowAnyException();

        // 40 -> 50 起飞在途
        assertThatCode(() -> CargoOrderStateMachine.validateTransition(
                OrderStateEnum.PENDING_TAKEOFF.getCode(), OrderStateEnum.FLYING.getCode()))
                .doesNotThrowAnyException();

        // 50 -> 60 送达签收
        assertThatCode(() -> CargoOrderStateMachine.validateTransition(
                OrderStateEnum.FLYING.getCode(), OrderStateEnum.DELIVERED.getCode()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("异常流转分支测试: 取消与异常中断")
    void testCancelAndAbortTransitions() {
        // 10 -> 70 客户未支付取消
        assertThatCode(() -> CargoOrderStateMachine.validateTransition(
                OrderStateEnum.PENDING_PAY.getCode(), OrderStateEnum.CANCELLED.getCode()))
                .doesNotThrowAnyException();

        // 20 -> 70 商户拒单或客户取消
        assertThatCode(() -> CargoOrderStateMachine.validateTransition(
                OrderStateEnum.PENDING_ACCEPT.getCode(), OrderStateEnum.CANCELLED.getCode()))
                .doesNotThrowAnyException();

        // 30 -> 70 调度取消
        assertThatCode(() -> CargoOrderStateMachine.validateTransition(
                OrderStateEnum.PENDING_DISPATCH.getCode(), OrderStateEnum.CANCELLED.getCode()))
                .doesNotThrowAnyException();

        // 40 -> 80 起飞前故障阻断中断
        assertThatCode(() -> CargoOrderStateMachine.validateTransition(
                OrderStateEnum.PENDING_TAKEOFF.getCode(), OrderStateEnum.ABORTED.getCode()))
                .doesNotThrowAnyException();

        // 50 -> 80 飞行中突发异常迫降中断
        assertThatCode(() -> CargoOrderStateMachine.validateTransition(
                OrderStateEnum.FLYING.getCode(), OrderStateEnum.ABORTED.getCode()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("非法跳跃与逆向流转拦截测试: 必须抛出 OrderStateTransitionException")
    void testIllegalTransitionsIntercepted() {
        // 未支付(10) 禁止直接越过接单跳到 待调度(30)
        assertThatThrownBy(() -> CargoOrderStateMachine.validateTransition(
                OrderStateEnum.PENDING_PAY.getCode(), OrderStateEnum.PENDING_DISPATCH.getCode()))
                .isInstanceOf(OrderStateTransitionException.class)
                .hasMessageContaining("不允许从");

        // 未支付(10) 禁止直接跃迁至 已送达(60)
        assertThatThrownBy(() -> CargoOrderStateMachine.validateTransition(
                OrderStateEnum.PENDING_PAY.getCode(), OrderStateEnum.DELIVERED.getCode()))
                .isInstanceOf(OrderStateTransitionException.class);

        // 已取消(70) 禁止重新调度派单(40)
        assertThatThrownBy(() -> CargoOrderStateMachine.validateTransition(
                OrderStateEnum.CANCELLED.getCode(), OrderStateEnum.PENDING_TAKEOFF.getCode()))
                .isInstanceOf(OrderStateTransitionException.class);

        // 已送达(60) 终态禁止再变更为 待支付(10)
        assertThatThrownBy(() -> CargoOrderStateMachine.validateTransition(
                OrderStateEnum.DELIVERED.getCode(), OrderStateEnum.PENDING_PAY.getCode()))
                .isInstanceOf(OrderStateTransitionException.class);

        // 异常中断(80) 终态禁止再变更为 飞行中(50)
        assertThatThrownBy(() -> CargoOrderStateMachine.validateTransition(
                OrderStateEnum.ABORTED.getCode(), OrderStateEnum.FLYING.getCode()))
                .isInstanceOf(OrderStateTransitionException.class);
    }
}
