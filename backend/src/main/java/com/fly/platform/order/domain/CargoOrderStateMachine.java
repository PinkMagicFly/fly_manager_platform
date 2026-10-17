package com.fly.platform.order.domain;

import com.fly.platform.order.entity.CargoOrder;
import com.fly.platform.order.entity.OrderStateFlow;
import com.fly.platform.order.enums.OrderStateEnum;
import com.fly.platform.order.mapper.OrderStateFlowMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 订单九态流转状态机驱动器
 * 严格守护订单生命周期的正向推进与异常阻断，强审计记录每一次变迁流水
 *
 * @author SoarCloud Team
 */
@Component
public class CargoOrderStateMachine {

    /** 允许的状态流转规则表：fromState -> Set<toState> */
    private static final Map<Integer, Set<Integer>> ALLOWED_TRANSITIONS = new HashMap<>();

    static {
        // 10 待支付 -> 20 待接单（支付成功）、70 已取消（主动取消或超时）
        ALLOWED_TRANSITIONS.put(OrderStateEnum.PENDING_PAY.getCode(),
                new HashSet<>(Arrays.asList(OrderStateEnum.PENDING_ACCEPT.getCode(), OrderStateEnum.CANCELLED.getCode())));

        // 20 待接单 -> 30 待调度（商户接单）、70 已取消（商户拒单或客户取消）
        ALLOWED_TRANSITIONS.put(OrderStateEnum.PENDING_ACCEPT.getCode(),
                new HashSet<>(Arrays.asList(OrderStateEnum.PENDING_DISPATCH.getCode(), OrderStateEnum.CANCELLED.getCode())));

        // 30 待调度 -> 40 待起飞（调度确认并联动飞行申报）、70 已取消（调度取消）
        ALLOWED_TRANSITIONS.put(OrderStateEnum.PENDING_DISPATCH.getCode(),
                new HashSet<>(Arrays.asList(OrderStateEnum.PENDING_TAKEOFF.getCode(), OrderStateEnum.CANCELLED.getCode())));

        // 40 待起飞 -> 50 飞行中（机长起飞执行）、80 异常中断（起飞前故障阻断）、70 已取消
        ALLOWED_TRANSITIONS.put(OrderStateEnum.PENDING_TAKEOFF.getCode(),
                new HashSet<>(Arrays.asList(OrderStateEnum.FLYING.getCode(), OrderStateEnum.ABORTED.getCode(), OrderStateEnum.CANCELLED.getCode())));

        // 50 飞行中 -> 60 已送达（客户签收并自动出账）、80 异常中断（在途迫降或突发故障）
        ALLOWED_TRANSITIONS.put(OrderStateEnum.FLYING.getCode(),
                new HashSet<>(Arrays.asList(OrderStateEnum.DELIVERED.getCode(), OrderStateEnum.ABORTED.getCode())));

        // 终态（60 已送达、70 已取消、80 异常中断）不允许任何外向跃迁
        ALLOWED_TRANSITIONS.put(OrderStateEnum.DELIVERED.getCode(), Collections.emptySet());
        ALLOWED_TRANSITIONS.put(OrderStateEnum.CANCELLED.getCode(), Collections.emptySet());
        ALLOWED_TRANSITIONS.put(OrderStateEnum.ABORTED.getCode(), Collections.emptySet());
    }

    /**
     * 校验从当前状态流转到目标状态的合法性
     *
     * @param fromState 当前状态
     * @param toState   目标状态
     * @throws OrderStateTransitionException 若非法则抛出状态机跃迁异常
     */
    public static void validateTransition(Integer fromState, Integer toState) {
        if (fromState == null || toState == null) {
            throw new OrderStateTransitionException("状态值不能为空");
        }
        if (fromState.equals(toState)) {
            return;
        }
        Set<Integer> validTargets = ALLOWED_TRANSITIONS.get(fromState);
        if (validTargets == null || !validTargets.contains(toState)) {
            throw new OrderStateTransitionException(fromState, toState);
        }
    }

    /**
     * 驱动订单状态跃迁并记录流水强审计
     *
     * @param order       运单实体
     * @param toState     目标状态
     * @param operatorId  操作人ID
     * @param remark      流转说明
     * @param flowMapper  状态流水持久化 Mapper
     * @return 产生的状态变迁流水记录
     */
    public OrderStateFlow transition(CargoOrder order, Integer toState, Long operatorId, String remark, OrderStateFlowMapper flowMapper) {
        Integer fromState = order.getOrderState();
        validateTransition(fromState, toState);

        order.setOrderState(toState);
        order.updateAudit();

        OrderStateFlow flow = new OrderStateFlow();
        flow.setOrderId(order.getOrderId());
        flow.setFromState(fromState);
        flow.setToState(toState);
        flow.setOperatorId(operatorId != null ? operatorId : 0L);
        flow.setRemark(remark != null ? remark : "状态变更");
        flow.setCreatedAt(LocalDateTime.now());

        if (flowMapper != null) {
            flowMapper.insert(flow);
        }
        return flow;
    }
}
