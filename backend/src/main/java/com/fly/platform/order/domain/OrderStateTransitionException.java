package com.fly.platform.order.domain;

import com.fly.platform.common.exception.BusinessException;
import com.fly.platform.order.enums.OrderStateEnum;

/**
 * 订单状态机非法跃迁异常
 *
 * @author SoarCloud Team
 */
public class OrderStateTransitionException extends BusinessException {

    public OrderStateTransitionException(String message) {
        super(400, message);
    }

    public OrderStateTransitionException(Integer fromState, Integer toState) {
        super(400, String.format("订单状态不允许从【%s(%d)】直接跃迁至【%s(%d)】",
                OrderStateEnum.getDescByCode(fromState), fromState,
                OrderStateEnum.getDescByCode(toState), toState));
    }
}
