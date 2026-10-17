package com.fly.platform.order;

import com.fly.platform.airspace.dto.FlightApproveDTO;
import com.fly.platform.airspace.enums.FlightApplyStateEnum;
import com.fly.platform.airspace.mapper.FlightApplyMapper;
import com.fly.platform.airspace.service.FlightApplyService;
import com.fly.platform.asset.entity.UavInfo;
import com.fly.platform.asset.enums.UavStateEnum;
import com.fly.platform.asset.mapper.UavInfoMapper;
import com.fly.platform.order.domain.OrderStateTransitionException;
import com.fly.platform.order.dto.*;
import com.fly.platform.order.entity.CargoOrder;
import com.fly.platform.order.entity.SettleBill;
import com.fly.platform.order.enums.BillStateEnum;
import com.fly.platform.order.enums.OrderStateEnum;
import com.fly.platform.order.mapper.CargoOrderMapper;
import com.fly.platform.order.mapper.SettleBillMapper;
import com.fly.platform.order.service.CargoOrderService;
import com.fly.platform.order.service.SettleBillService;
import com.fly.platform.order.vo.*;
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
 * 阶段4端到端“功能测试链”全生命周期测试门禁 (E2E Function Test Chain)
 *
 * @author SoarCloud Team
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OrderFeatureChainTest {

    @Autowired
    private CargoOrderService cargoOrderService;

    @Autowired
    private SettleBillService settleBillService;

    @Autowired
    private CargoOrderMapper cargoOrderMapper;

    @Autowired
    private SettleBillMapper settleBillMapper;

    @Autowired
    private FlightApplyMapper flightApplyMapper;

    @Autowired
    private FlightApplyService flightApplyService;

    @Autowired
    private UavInfoMapper uavInfoMapper;

    @Test
    @DisplayName("端到端闭环测试: 下单-支付-接单-调度联动申报-起飞-送达自动出账-财务对账-开票")
    void testEndToEndOrderFeatureChain() {
        Long customerId = 6L;
        Long merchantId = 1L;
        Long pilotId = 1L;

        // 节点 1：前置阶梯运费试算
        FeeCalculateDTO feeDTO = new FeeCalculateDTO();
        feeDTO.setSenderLng(113.943);
        feeDTO.setSenderLat(22.540);
        feeDTO.setReceiverLng(114.062);
        feeDTO.setReceiverLat(22.553);
        feeDTO.setGoodsWeight(new BigDecimal("12.00"));
        feeDTO.setGoodsType(3);
        feeDTO.setUrgentFlag(1);

        FeeCalculateVO feeVO = cargoOrderService.calculateFee(feeDTO);
        assertThat(feeVO).isNotNull();
        assertThat(feeVO.getFeeTotal()).isGreaterThan(new BigDecimal("50.00"));

        // 节点 2：在线下单创建 (10 待支付)
        OrderCreateDTO createDTO = new OrderCreateDTO();
        createDTO.setSenderAddr("南山科技园枢纽站A区");
        createDTO.setReceiverAddr("福田深业上城楼顶起降坪");
        createDTO.setSenderLng(113.943);
        createDTO.setSenderLat(22.540);
        createDTO.setReceiverLng(114.062);
        createDTO.setReceiverLat(22.553);
        createDTO.setGoodsType(3);
        createDTO.setGoodsWeight(new BigDecimal("12.00"));
        createDTO.setUrgentFlag(1);
        createDTO.setPayWay(1);
        createDTO.setMerchantId(merchantId);

        CargoOrder createdOrder = cargoOrderService.createOrder(createDTO, customerId);
        assertThat(createdOrder).isNotNull();
        Long orderId = createdOrder.getOrderId();
        assertThat(createdOrder.getOrderState()).isEqualTo(OrderStateEnum.PENDING_PAY.getCode());

        // 节点 3：模拟在线支付 (10 -> 20 待接单)
        CargoOrder paidOrder = cargoOrderService.payOrder(orderId, customerId);
        assertThat(paidOrder.getOrderState()).isEqualTo(OrderStateEnum.PENDING_ACCEPT.getCode());

        // 节点 4：商户确认接单 (20 -> 30 待调度)
        CargoOrder acceptedOrder = cargoOrderService.acceptOrder(orderId, merchantId);
        assertThat(acceptedOrder.getOrderState()).isEqualTo(OrderStateEnum.PENDING_DISPATCH.getCode());

        // 节点 5：智能推荐计算与调度派单 (30 -> 40 待起飞，联动申报)
        SmartDispatchResultVO recommendResult = cargoOrderService.getRecommendDispatch(orderId);
        assertThat(recommendResult).isNotNull();

        UavInfo candidateUav = uavInfoMapper.selectList(null).stream()
                .filter(u -> u.getState() == UavStateEnum.IDLE.getCode() && u.getMaxLoad().compareTo(new BigDecimal("12.00")) >= 0)
                .findFirst().orElseThrow();

        OrderDispatchDTO dispatchDTO = new OrderDispatchDTO();
        dispatchDTO.setOrderId(orderId);
        dispatchDTO.setUavId(candidateUav.getUavId());
        dispatchDTO.setPilotId(pilotId);
        dispatchDTO.setAltPlanned(110);

        // 阶段5A新时序：提交飞行任务审批，不锁资源，订单停留 30 待调度
        CargoOrder dispatchedOrder = cargoOrderService.dispatchOrder(dispatchDTO, merchantId);
        assertThat(dispatchedOrder.getOrderState()).isEqualTo(OrderStateEnum.PENDING_DISPATCH.getCode());
        assertThat(dispatchedOrder.getApplyId()).isNotNull();
        assertThat(flightApplyMapper.selectApplyById(dispatchedOrder.getApplyId())).isNotNull();

        // 空管审批通过：空域时段确权 + 锁定资源，推进订单状态至 40 待起飞
        FlightApproveDTO approveDTO = new FlightApproveDTO();
        approveDTO.setApplyId(dispatchedOrder.getApplyId());
        approveDTO.setApproveState(FlightApplyStateEnum.APPROVED.getCode());
        approveDTO.setApproveNote("空管合规审核通过，准予起飞执飞");
        approveDTO.setSuggestedAlt(110);
        flightApplyService.approveApply(approveDTO, 3L);

        CargoOrder approvedOrder = cargoOrderMapper.selectOrderById(orderId);
        assertThat(approvedOrder.getOrderState()).isEqualTo(OrderStateEnum.PENDING_TAKEOFF.getCode());

        // 节点 6：机长确认起飞 (40 -> 50 飞行中，校验关联任务=已批准)
        CargoOrder flyingOrder = cargoOrderService.takeoffOrder(orderId, pilotId);
        assertThat(flyingOrder.getOrderState()).isEqualTo(OrderStateEnum.FLYING.getCode());
        // 节点 7：送达签收与自动出账 (50 -> 60 已送达)
        CargoOrder deliveredOrder = cargoOrderService.deliverOrder(orderId, pilotId);
        assertThat(deliveredOrder.getOrderState()).isEqualTo(OrderStateEnum.DELIVERED.getCode());

        SettleBill autoBill = settleBillMapper.selectBillByOrderId(orderId);
        assertThat(autoBill).isNotNull();
        assertThat(autoBill.getAmount()).isEqualByComparingTo(deliveredOrder.getFeeTotal());
        assertThat(autoBill.getBillState()).isEqualTo(BillStateEnum.PENDING_RECONCILE.getCode());

        // 节点 8：全景详情四合一回显验证
        CargoOrderDetailVO detailVO = cargoOrderService.getOrderDetail(orderId);
        assertThat(detailVO).isNotNull();
        assertThat(detailVO.getFlowList()).hasSizeGreaterThanOrEqualTo(5);
        assertThat(detailVO.getSettleBill()).isNotNull();
        assertThat(detailVO.getFlightApplyDetail()).isNotNull();
        assertThat(detailVO.getRoutePoints()).isNotEmpty();

        // 节点 9：商户确认对账 (1 -> 2 已对账)
        settleBillService.confirmBill(autoBill.getBillId(), merchantId);
        SettleBill reconciledBill = settleBillMapper.selectBillById(autoBill.getBillId());
        assertThat(reconciledBill.getBillState()).isEqualTo(BillStateEnum.RECONCILED.getCode());

        // 节点 10：开具增值税电子发票 (2 -> 3 已开票)
        InvoiceApplyDTO invoiceDTO = new InvoiceApplyDTO();
        invoiceDTO.setBillId(autoBill.getBillId());
        invoiceDTO.setInvoiceTitle("腾讯科技（深圳）有限公司");
        invoiceDTO.setTaxNumber("91440300708461136T");
        invoiceDTO.setEmail("finance@tencent.com");

        settleBillService.applyInvoice(invoiceDTO, merchantId);
        SettleBill invoicedBill = settleBillMapper.selectBillById(autoBill.getBillId());
        assertThat(invoicedBill.getBillState()).isEqualTo(BillStateEnum.INVOICED.getCode());
        assertThat(invoicedBill.getInvoiceFlag()).isEqualTo(1);

        // 节点 11：终态逆向跃迁拦截测试
        assertThatThrownBy(() -> cargoOrderService.payOrder(orderId, customerId))
                .isInstanceOf(OrderStateTransitionException.class)
                .hasMessageContaining("不允许从");

    }
}
