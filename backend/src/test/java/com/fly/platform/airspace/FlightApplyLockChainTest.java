package com.fly.platform.airspace;

import com.fly.platform.airspace.dto.FlightApproveDTO;
import com.fly.platform.airspace.entity.FlightApply;
import com.fly.platform.airspace.enums.FlightApplyStateEnum;
import com.fly.platform.airspace.mapper.FlightApplyMapper;
import com.fly.platform.airspace.service.FlightApplyService;
import com.fly.platform.asset.entity.SiteBooking;
import com.fly.platform.asset.entity.UavInfo;
import com.fly.platform.asset.enums.UavStateEnum;
import com.fly.platform.asset.mapper.SiteBookingMapper;
import com.fly.platform.asset.mapper.UavInfoMapper;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 阶段5A核心测试门禁：飞行任务资源锁定链与守卫测试
 * 链路：提交零占用 -> 审批锁定 -> 时段冲突拦截 -> 驳回重报 -> 起飞守卫 -> 重复提交拦截
 *
 * @author SoarCloud Team
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class FlightApplyLockChainTest {

    @Autowired
    private CargoOrderMapper cargoOrderMapper;

    @Autowired
    private CargoOrderService cargoOrderService;

    @Autowired
    private FlightApplyMapper flightApplyMapper;

    @Autowired
    private FlightApplyService flightApplyService;

    @Autowired
    private UavInfoMapper uavInfoMapper;

    @Autowired
    private SiteBookingMapper siteBookingMapper;

    /**
     * 辅助方法：快速在数据库中构造一个处于“30-待调度”状态的基准订单
     */
    private CargoOrder createPendingDispatchOrder(String snSuffix) {
        CargoOrder order = new CargoOrder();
        order.setOrderSn("ORDTEST" + snSuffix);
        order.setCustomerId(1L);
        order.setMerchantId(1L);
        order.setSenderAddr("深圳南山科技园枢纽站");
        order.setReceiverAddr("深圳福田深业上城");
        order.setSenderGeo("POINT(113.943000 22.540000)");
        order.setReceiverGeo("POINT(114.062000 22.553000)");
        order.setGoodsType(1);
        order.setGoodsWeight(new BigDecimal("5.00"));
        order.setFeeTotal(new BigDecimal("80.00"));
        order.setOrderState(OrderStateEnum.PENDING_DISPATCH.getCode()); // 30-待调度
        order.setPayState(1);
        order.initAudit();
        cargoOrderMapper.insertOrder(order);
        return order;
    }

    @Test
    @DisplayName("阶段5A全链路守卫测试：提交零占用->审批锁定->时段冲突拦截->驳回重报->起飞守卫->重复提交拦截")
    void testFlightApplyLockChainFullFlow() {
        Long merchantId = 1L;
        Long pilotId = 1L;
        Long uavId = 1L;

        // -------------------------------------------------------------
        // 节点 1：提交零占用（G1）
        // -------------------------------------------------------------
        CargoOrder order1 = createPendingDispatchOrder("5A01");
        Long order1Id = order1.getOrderId();

        LocalDateTime start1 = LocalDateTime.now().plusHours(1);
        LocalDateTime end1 = LocalDateTime.now().plusHours(2);

        OrderDispatchDTO dispatchDTO1 = new OrderDispatchDTO();
        dispatchDTO1.setOrderId(order1Id);
        dispatchDTO1.setUavId(uavId);
        dispatchDTO1.setPilotId(pilotId);
        dispatchDTO1.setStartAt(start1);
        dispatchDTO1.setEndAt(end1);
        dispatchDTO1.setAltPlanned(100);
        // 5A.1：提交多点折线（3个航路点）
        dispatchDTO1.setRouteLine("LINESTRING(113.943000 22.540000, 114.000000 22.545000, 114.062000 22.553000)");

        CargoOrder dispatchedOrder1 = cargoOrderService.dispatchOrder(dispatchDTO1, merchantId);

        // 断言：订单严格停留待调度 30 状态，未被篡改推进
        assertThat(dispatchedOrder1.getOrderState()).isEqualTo(OrderStateEnum.PENDING_DISPATCH.getCode());
        assertThat(dispatchedOrder1.getApplyId()).isNotNull();

        // 断言：任务已生成但处于 0-待审批，且入库折线航路点数 ≥ 3
        FlightApply apply1 = flightApplyMapper.selectApplyById(dispatchedOrder1.getApplyId());
        assertThat(apply1).isNotNull();
        assertThat(apply1.getApproveState()).isEqualTo(FlightApplyStateEnum.PENDING.getCode());
        assertThat(apply1.parseRoutePoints()).hasSizeGreaterThanOrEqualTo(3);

        // 断言：无人机资产未被锁定（仍为闲置 0）
        UavInfo uav1 = uavInfoMapper.selectById(uavId);
        assertThat(uav1.getState()).isEqualTo(UavStateEnum.IDLE.getCode());

        // 断言：场站无有效预约记录（零占用）
        List<SiteBooking> bookingsBefore = siteBookingMapper.selectOverlappingBookings(1L, start1, end1);
        assertThat(bookingsBefore).isEmpty();

        // -------------------------------------------------------------
        // 节点 2：审批锁定（G1）
        // -------------------------------------------------------------
        FlightApproveDTO approveDTO1 = new FlightApproveDTO();
        approveDTO1.setApplyId(apply1.getApplyId());
        approveDTO1.setApproveState(FlightApplyStateEnum.APPROVED.getCode());
        approveDTO1.setApproveNote("空管审批通过，确权时段并锁定资源");
        flightApplyService.approveApply(approveDTO1, 3L);

        // 断言：任务跃迁至 1-已批准
        FlightApply approvedApply1 = flightApplyMapper.selectApplyById(apply1.getApplyId());
        assertThat(approvedApply1.getApproveState()).isEqualTo(FlightApplyStateEnum.APPROVED.getCode());

        // 断言：订单推进至 40-待起飞
        CargoOrder afterApprovedOrder1 = cargoOrderMapper.selectOrderById(order1Id);
        assertThat(afterApprovedOrder1.getOrderState()).isEqualTo(OrderStateEnum.PENDING_TAKEOFF.getCode());

        // 断言：场站起降机位已成功预约锁定
        List<SiteBooking> bookingsAfter = siteBookingMapper.selectOverlappingBookings(1L, start1, end1);
        assertThat(bookingsAfter).hasSizeGreaterThanOrEqualTo(1);

        // -------------------------------------------------------------
        // 节点 3：时段冲突拦截（同一无人机同一时段排他性守卫）
        // -------------------------------------------------------------
        CargoOrder order2 = createPendingDispatchOrder("5A02");
        Long order2Id = order2.getOrderId();

        // 构造与订单1在同一时段(重叠30分钟)的派单请求，使用同一台无人机 uavId=1
        OrderDispatchDTO dispatchDTO2 = new OrderDispatchDTO();
        dispatchDTO2.setOrderId(order2Id);
        dispatchDTO2.setUavId(uavId);
        dispatchDTO2.setPilotId(pilotId);
        dispatchDTO2.setStartAt(start1.plusMinutes(30)); // 时段重叠
        dispatchDTO2.setEndAt(end1.plusMinutes(30));
        dispatchDTO2.setAltPlanned(100);
        dispatchDTO2.setRouteLine("LINESTRING(113.943000 22.540000, 114.000000 22.545000, 114.062000 22.553000)");

        CargoOrder dispatchedOrder2 = cargoOrderService.dispatchOrder(dispatchDTO2, merchantId);
        assertThat(dispatchedOrder2.getOrderState()).isEqualTo(OrderStateEnum.PENDING_DISPATCH.getCode());
        Long apply2Id = dispatchedOrder2.getApplyId();

        // 空管对存在时段冲突的任务2进行审批，断言被强行阻断并抛出 BusinessException
        FlightApproveDTO conflictApproveDTO = new FlightApproveDTO();
        conflictApproveDTO.setApplyId(apply2Id);
        conflictApproveDTO.setApproveState(FlightApplyStateEnum.APPROVED.getCode());
        conflictApproveDTO.setApproveNote("试图强行批准冲突任务");

        assertThatThrownBy(() -> flightApplyService.approveApply(conflictApproveDTO, 3L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无人机时段冲突拦截");

        // 断言：订单2仍然保留在 30 待调度
        CargoOrder checkOrder2 = cargoOrderMapper.selectOrderById(order2Id);
        assertThat(checkOrder2.getOrderState()).isEqualTo(OrderStateEnum.PENDING_DISPATCH.getCode());

        // -------------------------------------------------------------
        // 节点 4：驳回重报（G3原因必填 + 驳回后订单停留在30 + 重报复用更新）
        // -------------------------------------------------------------
        // G3 测试：空驳回原因拦截
        FlightApproveDTO blankRejectDTO = new FlightApproveDTO();
        blankRejectDTO.setApplyId(apply2Id);
        blankRejectDTO.setApproveState(FlightApplyStateEnum.REJECTED.getCode());
        blankRejectDTO.setApproveNote("   ");
        assertThatThrownBy(() -> flightApplyService.approveApply(blankRejectDTO, 3L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("驳回原因必填");

        // 正常驳回
        FlightApproveDTO rejectDTO = new FlightApproveDTO();
        rejectDTO.setApplyId(apply2Id);
        rejectDTO.setApproveState(FlightApplyStateEnum.REJECTED.getCode());
        rejectDTO.setApproveNote("该时段与任务1时段冲突，请调整后重新申报");
        flightApplyService.approveApply(rejectDTO, 3L);

        FlightApply rejectedApply = flightApplyMapper.selectApplyById(apply2Id);
        assertThat(rejectedApply.getApproveState()).isEqualTo(FlightApplyStateEnum.REJECTED.getCode());

        // 驳回重报：修改起降时间至不冲突的未来时段，并调整更新航线，再次提交
        OrderDispatchDTO redispatchDTO = new OrderDispatchDTO();
        redispatchDTO.setOrderId(order2Id);
        redispatchDTO.setUavId(uavId);
        redispatchDTO.setPilotId(pilotId);
        redispatchDTO.setStartAt(end1.plusHours(1)); // 不再冲突
        redispatchDTO.setEndAt(end1.plusHours(2));
        redispatchDTO.setAltPlanned(90);
        String updatedRoute = "LINESTRING(113.943000 22.540000, 114.020000 22.550000, 114.062000 22.553000)";
        redispatchDTO.setRouteLine(updatedRoute);

        CargoOrder redispatchedOrder2 = cargoOrderService.dispatchOrder(redispatchDTO, merchantId);
        assertThat(redispatchedOrder2.getOrderState()).isEqualTo(OrderStateEnum.PENDING_DISPATCH.getCode());

        // 断言驳回重报后折线更新
        FlightApply recheckApply = flightApplyMapper.selectApplyById(redispatchedOrder2.getApplyId());
        assertThat(recheckApply.getRouteLine()).contains("114.020000");

        // 重新批准任务2成功
        FlightApproveDTO approveDTO2 = new FlightApproveDTO();
        approveDTO2.setApplyId(redispatchedOrder2.getApplyId());
        approveDTO2.setApproveState(FlightApplyStateEnum.APPROVED.getCode());
        approveDTO2.setApproveNote("调整后时段核验无误，准予通过");
        flightApplyService.approveApply(approveDTO2, 3L);

        CargoOrder finalOrder2 = cargoOrderMapper.selectOrderById(order2Id);
        assertThat(finalOrder2.getOrderState()).isEqualTo(OrderStateEnum.PENDING_TAKEOFF.getCode());

        // -------------------------------------------------------------
        // 节点 5：起飞守卫（G4关联任务必须为已批准1，起飞联动任务4执行中+设备1任务中）
        // -------------------------------------------------------------
        CargoOrder unapprovedOrder = createPendingDispatchOrder("5A03");
        // 构造一个未审批(状态0)的任务并关联订单
        OrderDispatchDTO pendingDTO = new OrderDispatchDTO();
        pendingDTO.setOrderId(unapprovedOrder.getOrderId());
        pendingDTO.setUavId(3L);
        pendingDTO.setPilotId(pilotId);
        pendingDTO.setRouteLine("LINESTRING(113.943000 22.540000, 114.000000 22.545000, 114.062000 22.553000)");
        cargoOrderService.dispatchOrder(pendingDTO, merchantId);

        // 未经审批直接强行起飞，断言被起飞守卫阻断
        assertThatThrownBy(() -> cargoOrderService.takeoffOrder(unapprovedOrder.getOrderId(), pilotId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("起飞守卫拦截");

        // 对已批准的订单1执行正向起飞
        CargoOrder flyingOrder1 = cargoOrderService.takeoffOrder(order1Id, pilotId);
        assertThat(flyingOrder1.getOrderState()).isEqualTo(OrderStateEnum.FLYING.getCode());

        FlightApply executingApply1 = flightApplyMapper.selectApplyById(approvedApply1.getApplyId());
        assertThat(executingApply1.getApproveState()).isEqualTo(FlightApplyStateEnum.EXECUTING.getCode());

        UavInfo runningUav = uavInfoMapper.selectById(uavId);
        assertThat(runningUav.getState()).isEqualTo(UavStateEnum.ON_TASK.getCode());

        // -------------------------------------------------------------
        // 节点 6：重复提交拦截（G6存在非终态任务禁止重复提交）
        // -------------------------------------------------------------
        OrderDispatchDTO duplicateDTO = new OrderDispatchDTO();
        duplicateDTO.setOrderId(order1Id);
        duplicateDTO.setUavId(uavId);
        duplicateDTO.setPilotId(pilotId);
        duplicateDTO.setRouteLine("LINESTRING(113.943000 22.540000, 114.000000 22.545000, 114.062000 22.553000)");

        assertThatThrownBy(() -> cargoOrderService.dispatchOrder(duplicateDTO, merchantId))
                .isInstanceOf(BusinessException.class);
    }
}
