package com.fly.platform.order.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fly.platform.order.domain.CargoOrderStateMachine;
import com.fly.platform.order.entity.CargoOrder;
import com.fly.platform.order.enums.OrderStateEnum;
import com.fly.platform.order.mapper.CargoOrderMapper;
import com.fly.platform.order.mapper.OrderStateFlowMapper;
import com.fly.platform.sysparam.service.SysParamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单生命周期超时自动取消定时调度任务
 * 监控：10-待支付超时自动关闭、20-待接单超时商户未响应自动取消
 *
 * @author SoarCloud Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final CargoOrderMapper cargoOrderMapper;
    private final OrderStateFlowMapper orderStateFlowMapper;
    private final CargoOrderStateMachine stateMachine;
    private final SysParamService sysParamService;

    /**
     * 每分钟轮询执行超时扫描与自动关单
     */
    @Scheduled(fixedDelay = 60000)
    public void scheduleCheck() {
        try {
            checkAndCancelTimeoutOrders();
        } catch (Exception e) {
            log.error("执行订单超时扫描任务异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 核心超时检测与流转执行（供定时调度及单测驱动调用）
     *
     * @return 本次成功关闭的超时订单总数
     */
    @Transactional(rollbackFor = Exception.class)
    public int checkAndCancelTimeoutOrders() {
        int cancelledCount = 0;

        // 1. 扫描 10-待支付 超时订单
        int payTimeoutMinutes = sysParamService != null
                ? sysParamService.getInt("order.timeout_pay_minutes", 30)
                : 30;
        LocalDateTime payThreshold = LocalDateTime.now().minusMinutes(payTimeoutMinutes);

        List<CargoOrder> pendingPayList = cargoOrderMapper.selectList(
                new LambdaQueryWrapper<CargoOrder>()
                        .eq(CargoOrder::getOrderState, OrderStateEnum.PENDING_PAY.getCode())
                        .le(CargoOrder::getCreatedAt, payThreshold)
        );

        for (CargoOrder order : pendingPayList) {
            try {
                stateMachine.transition(order, OrderStateEnum.CANCELLED.getCode(), 0L,
                        "超时自动取消（未在规定时间内完成支付）", orderStateFlowMapper);
                cargoOrderMapper.updateById(order);
                cancelledCount++;
                log.info("订单[ID={}, SN={}]待支付超时，系统已自动取消并落流水", order.getOrderId(), order.getOrderSn());
            } catch (Exception e) {
                log.error("取消超时待支付订单失败: orderId={}, err={}", order.getOrderId(), e.getMessage());
            }
        }

        // 2. 扫描 20-待接单 超时订单
        int acceptTimeoutMinutes = sysParamService != null
                ? sysParamService.getInt("order.timeout_accept_minutes", 60)
                : 60;
        LocalDateTime acceptThreshold = LocalDateTime.now().minusMinutes(acceptTimeoutMinutes);

        List<CargoOrder> pendingAcceptList = cargoOrderMapper.selectList(
                new LambdaQueryWrapper<CargoOrder>()
                        .eq(CargoOrder::getOrderState, OrderStateEnum.PENDING_ACCEPT.getCode())
                        .le(CargoOrder::getUpdatedAt, acceptThreshold)
        );

        for (CargoOrder order : pendingAcceptList) {
            try {
                stateMachine.transition(order, OrderStateEnum.CANCELLED.getCode(), 0L,
                        "超时自动取消（商户超时未承接该单）", orderStateFlowMapper);
                cargoOrderMapper.updateById(order);
                cancelledCount++;
                log.info("订单[ID={}, SN={}]待接单超时，系统已自动取消并落流水", order.getOrderId(), order.getOrderSn());
            } catch (Exception e) {
                log.error("取消超时待接单订单失败: orderId={}, err={}", order.getOrderId(), e.getMessage());
            }
        }

        return cancelledCount;
    }
}
