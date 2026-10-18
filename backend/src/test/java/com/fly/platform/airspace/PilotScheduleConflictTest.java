package com.fly.platform.airspace;

import com.fly.platform.airspace.dto.FlightApproveDTO;
import com.fly.platform.airspace.entity.FlightApply;
import com.fly.platform.airspace.enums.FlightApplyStateEnum;
import com.fly.platform.airspace.mapper.FlightApplyMapper;
import com.fly.platform.airspace.service.FlightApplyService;
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
@DisplayName("集成测试: 同一机长时段排他性守卫与防重叠冲突拦截")
public class PilotScheduleConflictTest {

    @Autowired
    private CargoOrderService cargoOrderService;

    @Autowired
    private CargoOrderMapper cargoOrderMapper;

    @Autowired
    private FlightApplyService flightApplyService;

    @Autowired
    private FlightApplyMapper flightApplyMapper;

    private CargoOrder createBaseOrder(String suffix) {
        CargoOrder order = new CargoOrder();
        order.setOrderSn("PLT" + suffix);
        order.setCustomerId(1L);
        order.setMerchantId(1L);
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
    @DisplayName("守卫1：同一机长在重叠时段已被锁定，调度发起时坚决前置拦截")
    public void testDispatchPilotTimeConflictBlocked() {
        Long merchantId = 1L;
        Long pilotId = 1L;
        LocalDateTime start1 = LocalDateTime.now().plusDays(5).withHour(10).withMinute(0);
        LocalDateTime end1 = LocalDateTime.now().plusDays(5).withHour(11).withMinute(30);

        // 订单1正常调度并审批通过锁定
        CargoOrder order1 = createBaseOrder("P01");
        OrderDispatchDTO dto1 = new OrderDispatchDTO();
        dto1.setOrderId(order1.getOrderId());
        dto1.setUavId(2L);
        dto1.setPilotId(pilotId);
        dto1.setStartAt(start1);
        dto1.setEndAt(end1);
        dto1.setAltPlanned(100);
        dto1.setRouteLine("LINESTRING(113.943000 22.540000, 114.000000 22.545000, 114.062000 22.553000)");
        cargoOrderService.dispatchOrder(dto1, merchantId);

        FlightApply apply1 = flightApplyMapper.selectByOrderId(order1.getOrderId());
        FlightApproveDTO approveDTO1 = new FlightApproveDTO();
        approveDTO1.setApplyId(apply1.getApplyId());
        approveDTO1.setApproveState(FlightApplyStateEnum.APPROVED.getCode());
        approveDTO1.setApproveNote("任务1审批通过并锁定机长时段");
        flightApplyService.approveApply(approveDTO1, 3L);

        // 订单2指派同一机长在重叠时段，调度时必须直接被前置拦截
        CargoOrder order2 = createBaseOrder("P02");
        OrderDispatchDTO dto2 = new OrderDispatchDTO();
        dto2.setOrderId(order2.getOrderId());
        dto2.setUavId(3L); // 换了无人机
        dto2.setPilotId(pilotId); // 相同机长
        dto2.setStartAt(start1.plusMinutes(20)); // 重叠
        dto2.setEndAt(end1.plusMinutes(20));
        dto2.setAltPlanned(90);
        dto2.setRouteLine("LINESTRING(113.943000 22.540000, 114.000000 22.545000, 114.062000 22.553000)");

        assertThatThrownBy(() -> cargoOrderService.dispatchOrder(dto2, merchantId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("机长时段冲突拦截")
                .hasMessageContaining("不可重复调度指派");
    }

    @Test
    @DisplayName("守卫2：机长在无重叠时段正常调度并获得空管审批通过")
    public void testNonOverlappingPilotScheduleSuccess() {
        Long merchantId = 1L;
        Long pilotId = 1L;
        LocalDateTime startSafe = LocalDateTime.now().plusDays(6).withHour(14).withMinute(0);
        LocalDateTime endSafe = LocalDateTime.now().plusDays(6).withHour(15).withMinute(30);

        CargoOrder order = createBaseOrder("P03");
        OrderDispatchDTO dto = new OrderDispatchDTO();
        dto.setOrderId(order.getOrderId());
        dto.setUavId(2L);
        dto.setPilotId(pilotId);
        dto.setStartAt(startSafe);
        dto.setEndAt(endSafe);
        dto.setAltPlanned(100);
        dto.setRouteLine("LINESTRING(113.943000 22.540000, 114.000000 22.545000, 114.062000 22.553000)");

        CargoOrder dispatched = cargoOrderService.dispatchOrder(dto, merchantId);
        assertThat(dispatched.getOrderState()).isEqualTo(OrderStateEnum.PENDING_DISPATCH.getCode());

        FlightApply apply = flightApplyMapper.selectByOrderId(order.getOrderId());
        FlightApproveDTO approveDTO = new FlightApproveDTO();
        approveDTO.setApplyId(apply.getApplyId());
        approveDTO.setApproveState(FlightApplyStateEnum.APPROVED.getCode());
        approveDTO.setApproveNote("无机长时段冲突，准予批准");
        flightApplyService.approveApply(approveDTO, 3L);

        FlightApply approvedApply = flightApplyMapper.selectApplyById(apply.getApplyId());
        assertThat(approvedApply.getApproveState()).isEqualTo(FlightApplyStateEnum.APPROVED.getCode());
    }
}
