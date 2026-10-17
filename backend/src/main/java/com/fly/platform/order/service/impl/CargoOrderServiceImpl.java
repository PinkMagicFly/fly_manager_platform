package com.fly.platform.order.service.impl;

import com.fly.platform.airspace.domain.FlightApplyStateMachine;
import com.fly.platform.airspace.entity.FlightApply;
import com.fly.platform.airspace.enums.FlightApplyStateEnum;
import com.fly.platform.airspace.mapper.FlightApplyMapper;
import com.fly.platform.airspace.service.FlightApplyService;
import com.fly.platform.airspace.vo.FlightApplyVO;
import com.fly.platform.asset.entity.*;
import com.fly.platform.asset.enums.UavStateEnum;
import com.fly.platform.asset.mapper.*;
import com.fly.platform.asset.service.UavService;
import com.fly.platform.auth.entity.*;
import com.fly.platform.auth.mapper.*;
import com.fly.platform.common.exception.BusinessException;
import com.fly.platform.order.domain.CargoOrderStateMachine;
import com.fly.platform.order.dto.*;
import com.fly.platform.order.entity.*;
import com.fly.platform.order.enums.*;
import com.fly.platform.order.mapper.*;
import com.fly.platform.order.service.*;
import com.fly.platform.order.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 货运订单核心业务服务实现类
 *
 * @author SoarCloud Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CargoOrderServiceImpl implements CargoOrderService {

    private final CargoOrderMapper cargoOrderMapper;
    private final OrderStateFlowMapper orderStateFlowMapper;
    private final SettleBillMapper settleBillMapper;
    private final FlightApplyMapper flightApplyMapper;
    private final FlightApplyService flightApplyService;
    private final UavInfoMapper uavInfoMapper;
    private final CrewStaffMapper crewStaffMapper;
    private final MerchantArchiveMapper merchantArchiveMapper;
    private final CustomerArchiveMapper customerArchiveMapper;
    private final UavService uavService;
    private final FeeCalculatorService feeCalculatorService;
    private final SmartDispatchRecommendService smartDispatchRecommendService;
    private final CargoOrderStateMachine stateMachine;
    private final FlightApplyStateMachine flightApplyStateMachine;

    @Override
    public FeeCalculateVO calculateFee(FeeCalculateDTO dto) {
        return feeCalculatorService.calculateFee(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CargoOrder createOrder(OrderCreateDTO dto, Long customerId) {
        if (customerId == null) throw new BusinessException(400, "下单客户身份不能为空");

        FeeCalculateDTO feeDTO = new FeeCalculateDTO();
        feeDTO.setSenderLng(dto.getSenderLng());
        feeDTO.setSenderLat(dto.getSenderLat());
        feeDTO.setReceiverLng(dto.getReceiverLng());
        feeDTO.setReceiverLat(dto.getReceiverLat());
        feeDTO.setGoodsWeight(dto.getGoodsWeight());
        feeDTO.setGoodsType(dto.getGoodsType());
        feeDTO.setUrgentFlag(dto.getUrgentFlag());

        FeeCalculateVO fee = feeCalculatorService.calculateFee(feeDTO);
        String orderSn = "ORD" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + (int) (Math.random() * 9000 + 1000);

        CargoOrder order = new CargoOrder();
        order.setOrderSn(orderSn);
        order.setCustomerId(customerId);
        order.setMerchantId(dto.getMerchantId() != null ? dto.getMerchantId() : 1L);
        order.setSenderAddr(dto.getSenderAddr());
        order.setReceiverAddr(dto.getReceiverAddr());
        order.setSenderGeo(String.format("POINT(%.6f %.6f)", dto.getSenderLng(), dto.getSenderLat()));
        order.setReceiverGeo(String.format("POINT(%.6f %.6f)", dto.getReceiverLng(), dto.getReceiverLat()));
        order.setGoodsType(dto.getGoodsType());
        order.setGoodsWeight(dto.getGoodsWeight());
        order.setUrgentFlag(dto.getUrgentFlag() != null ? dto.getUrgentFlag() : 0);
        order.setFeeBase(fee.getFeeBase());
        order.setFeeAirspace(fee.getFeeAirspace());
        order.setFeeUrgent(fee.getFeeUrgent());
        order.setFeeDiscount(fee.getFeeDiscount());
        order.setFeeTotal(fee.getFeeTotal());
        order.setPayWay(dto.getPayWay() != null ? dto.getPayWay() : 1);
        order.setPayState(PayStateEnum.UNPAID.getCode());
        order.setOrderState(OrderStateEnum.PENDING_PAY.getCode());
        order.setExpectArriveAt(dto.getExpectArriveAt() != null ? dto.getExpectArriveAt() : LocalDateTime.now().plusHours(3));
        order.initAudit();

        cargoOrderMapper.insertOrder(order);

        OrderStateFlow flow = new OrderStateFlow();
        flow.setOrderId(order.getOrderId());
        flow.setFromState(0);
        flow.setToState(OrderStateEnum.PENDING_PAY.getCode());
        flow.setOperatorId(customerId);
        flow.setRemark("客户在线提交寄件运单，待支付");
        flow.setCreatedAt(LocalDateTime.now());
        orderStateFlowMapper.insert(flow);

        log.info("创建货运订单成功: orderId={}, orderSn={}", order.getOrderId(), orderSn);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CargoOrder payOrder(Long orderId, Long operatorId) {
        CargoOrder order = cargoOrderMapper.selectOrderById(orderId);
        if (order == null) throw new BusinessException(404, "订单不存在");

        stateMachine.transition(order, OrderStateEnum.PENDING_ACCEPT.getCode(), operatorId, "客户在线模拟支付成功", orderStateFlowMapper);
        cargoOrderMapper.updateOrderState(orderId, order.getOrderState(), PayStateEnum.PAID.getCode());
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CargoOrder acceptOrder(Long orderId, Long operatorId) {
        CargoOrder order = cargoOrderMapper.selectOrderById(orderId);
        if (order == null) throw new BusinessException(404, "订单不存在");

        stateMachine.transition(order, OrderStateEnum.PENDING_DISPATCH.getCode(), operatorId, "商户接单进入待调度池", orderStateFlowMapper);
        cargoOrderMapper.updateOrderState(orderId, order.getOrderState(), null);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchAcceptOrders(List<Long> orderIds, Long operatorId) {
        if (orderIds == null || orderIds.isEmpty()) return 0;
        int count = 0;
        for (Long id : orderIds) {
            try {
                acceptOrder(id, operatorId);
                count++;
            } catch (Exception e) {
                log.warn("批量接单跳过订单 orderId={}, 原因: {}", id, e.getMessage());
            }
        }
        return count;
    }

    @Override
    public SmartDispatchResultVO getRecommendDispatch(Long orderId) {
        CargoOrder order = cargoOrderMapper.selectOrderById(orderId);
        if (order == null) throw new BusinessException(404, "订单不存在");
        return smartDispatchRecommendService.recommendDispatch(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CargoOrder dispatchOrder(OrderDispatchDTO dto, Long operatorId) {
        CargoOrder order = cargoOrderMapper.selectOrderById(dto.getOrderId());
        if (order == null) throw new BusinessException(404, "订单不存在");

        if (!OrderStateEnum.PENDING_DISPATCH.getCode().equals(order.getOrderState())) {
            throw new BusinessException(400, "只有待调度状态的运单才允许提交飞行任务审批");
        }

        UavInfo uav = uavInfoMapper.selectById(dto.getUavId());
        if (uav == null) throw new BusinessException(404, "指派无人机不存在");
        // 注意：G1 语义重排，提交任务不锁资源不动订单，待空管审批通过时才时段锁定确权

        CrewStaff pilot = crewStaffMapper.selectById(dto.getPilotId());
        if (pilot == null) throw new BusinessException(404, "指派执飞机长不存在");

        // G6 防重检查：如果该订单已存在非终态飞行任务，禁止重复提交
        FlightApply existingApply = flightApplyMapper.selectByOrderId(order.getOrderId());
        if (existingApply != null && !FlightApplyStateEnum.isTerminal(existingApply.getApproveState())) {
            throw new BusinessException(400, "该订单已存在进行中的飞行任务，禁止重复提交");
        }

        LocalDateTime startAt = dto.getStartAt() != null ? dto.getStartAt() : LocalDateTime.now().plusMinutes(15);
        LocalDateTime endAt = dto.getEndAt() != null ? dto.getEndAt() : LocalDateTime.now().plusMinutes(45);
        String routeLine = String.format("LINESTRING(%.6f %.6f, %.6f %.6f)",
                order.getSenderLng(), order.getSenderLat(), order.getReceiverLng(), order.getReceiverLat());
        int altPlanned = dto.getAltPlanned() != null ? dto.getAltPlanned() : 100;

        String applySn = "FA" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + (int) (Math.random() * 9000 + 1000);

        FlightApply apply;
        if (existingApply != null) {
            // 驳回重报：复用并更新该申报单为待审批
            apply = existingApply;
            apply.setApplySn(applySn);
            apply.setUavId(dto.getUavId());
            apply.setPilotId(dto.getPilotId());
            apply.setRouteLine(routeLine);
            apply.setAltPlanned(altPlanned);
            apply.setStartAt(startAt);
            apply.setEndAt(endAt);
            apply.setCheckState(1);
            apply.setApproveState(FlightApplyStateEnum.PENDING.getCode());
            apply.setApproverId(null);
            apply.setApproveNote(null);
            apply.setApproveAt(null);
            flightApplyMapper.updateApplyContent(apply);
        } else {
            // 首次提交飞行任务申报单
            apply = new FlightApply();
            apply.setApplySn(applySn);
            apply.setOrderId(order.getOrderId());
            apply.setMerchantId(order.getMerchantId() != null ? order.getMerchantId() : 1L);
            apply.setUavId(dto.getUavId());
            apply.setPilotId(dto.getPilotId());
            apply.setRouteLine(routeLine);
            apply.setAltPlanned(altPlanned);
            apply.setStartAt(startAt);
            apply.setEndAt(endAt);
            apply.setCheckState(1);
            apply.setApproveState(FlightApplyStateEnum.PENDING.getCode());
            apply.initAudit();
            flightApplyMapper.insertApply(apply);
        }

        // G1 语义重排：提交飞行任务，不锁资源、订单停留待调度30
        cargoOrderMapper.updateOrderDispatch(order.getOrderId(), OrderStateEnum.PENDING_DISPATCH.getCode(),
                dto.getUavId(), apply.getApplyId());

        order.setUavId(dto.getUavId());
        order.setApplyId(apply.getApplyId());
        log.info("提交飞行任务成功: orderId={}, applyId={}, applySn={}, 订单停留待调度30", order.getOrderId(), apply.getApplyId(), applySn);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CargoOrder takeoffOrder(Long orderId, Long operatorId) {
        CargoOrder order = cargoOrderMapper.selectOrderById(orderId);
        if (order == null) throw new BusinessException(404, "订单不存在");

        // G4 起飞守卫：校验关联任务=1已批准
        if (order.getApplyId() == null) {
            throw new BusinessException(400, "起飞守卫拦截：订单未关联飞行任务，无法起飞");
        }
        FlightApply apply = flightApplyMapper.selectApplyById(order.getApplyId());
        if (apply == null) {
            throw new BusinessException(404, "起飞守卫拦截：关联飞行任务不存在");
        }
        if (!FlightApplyStateEnum.APPROVED.getCode().equals(apply.getApproveState())) {
            throw new BusinessException(400, String.format("起飞守卫拦截：关联飞行任务必须为【已批准】状态才允许起飞，当前任务状态为【%s】",
                    FlightApplyStateEnum.getDescByCode(apply.getApproveState())));
        }

        // 联动任务状态 -> 4 执行中
        flightApplyStateMachine.startExecute(apply);
        flightApplyMapper.updateApproveState(apply);

        // 联动无人机状态 -> 1 任务中
        if (order.getUavId() != null) {
            uavService.changeUavState(order.getUavId(), UavStateEnum.ON_TASK.getCode(), "指派运单确认起飞执飞中");
        }

        // 联动订单状态 -> 50 飞行中
        stateMachine.transition(order, OrderStateEnum.FLYING.getCode(), operatorId, "机长确认起飞执行在途货运配送，飞行任务已进入执行中", orderStateFlowMapper);
        cargoOrderMapper.updateOrderState(orderId, order.getOrderState(), null);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CargoOrder deliverOrder(Long orderId, Long operatorId) {
        CargoOrder order = cargoOrderMapper.selectOrderById(orderId);
        if (order == null) throw new BusinessException(404, "订单不存在");

        // G4 送达联动：任务 -> 5 已完成
        if (order.getApplyId() != null) {
            FlightApply apply = flightApplyMapper.selectApplyById(order.getApplyId());
            if (apply != null) {
                flightApplyStateMachine.complete(apply);
                flightApplyMapper.updateApproveState(apply);
            }
        }

        stateMachine.transition(order, OrderStateEnum.DELIVERED.getCode(), operatorId, "目的地精准降落完成签收，飞行任务已圆满完成", orderStateFlowMapper);
        cargoOrderMapper.updateOrderState(orderId, order.getOrderState(), null);

        if (order.getUavId() != null) {
            try {
                uavService.changeUavState(order.getUavId(), UavStateEnum.IDLE.getCode(), "运单送达完成签收，设备恢复闲置");
            } catch (Exception e) {
                log.warn("恢复无人机闲置异常: {}", e.getMessage());
            }
        }

        String billSn = "BILL" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + (int) (Math.random() * 9000 + 1000);

        SettleBill bill = new SettleBill();
        bill.setBillSn(billSn);
        bill.setOrderId(order.getOrderId());
        bill.setMerchantId(order.getMerchantId() != null ? order.getMerchantId() : 1L);
        bill.setAmount(order.getFeeTotal());
        bill.setBillState(BillStateEnum.PENDING_RECONCILE.getCode());
        bill.setInvoiceFlag(0);
        bill.setSettleAt(LocalDateTime.now());
        bill.initAudit();

        settleBillMapper.insert(bill);
        log.info("运单签收送达，自动生成对账单: orderId={}, billId={}, billSn={}", order.getOrderId(), bill.getBillId(), billSn);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CargoOrder cancelOrder(Long orderId, Long operatorId, String reason) {
        CargoOrder order = cargoOrderMapper.selectOrderById(orderId);
        if (order == null) throw new BusinessException(404, "订单不存在");

        stateMachine.transition(order, OrderStateEnum.CANCELLED.getCode(), operatorId, "订单已取消: " + (reason != null ? reason : "用户自主取消"), orderStateFlowMapper);
        cargoOrderMapper.updateOrderState(orderId, order.getOrderState(), null);

        if (order.getUavId() != null) {
            try {
                uavService.changeUavState(order.getUavId(), UavStateEnum.IDLE.getCode(), "运单取消，释放无人机资产");
            } catch (Exception ignored) {}
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CargoOrder abortOrder(Long orderId, Long operatorId, String reason) {
        CargoOrder order = cargoOrderMapper.selectOrderById(orderId);
        if (order == null) throw new BusinessException(404, "订单不存在");

        stateMachine.transition(order, OrderStateEnum.ABORTED.getCode(), operatorId, "配送异常中断: " + (reason != null ? reason : "突发紧急情况"), orderStateFlowMapper);
        cargoOrderMapper.updateOrderState(orderId, order.getOrderState(), null);
        return order;
    }

    @Override
    public CargoOrderDetailVO getOrderDetail(Long orderId) {
        CargoOrder order = cargoOrderMapper.selectOrderById(orderId);
        if (order == null) throw new BusinessException(404, "订单不存在");

        CargoOrderVO baseVO = convertToVO(order);
        CargoOrderDetailVO detailVO = new CargoOrderDetailVO();
        org.springframework.beans.BeanUtils.copyProperties(baseVO, detailVO);

        List<OrderStateFlow> flows = orderStateFlowMapper.selectByOrderId(orderId);
        List<OrderStateFlowVO> flowVOs = flows.stream().map(f -> {
            OrderStateFlowVO vo = new OrderStateFlowVO();
            vo.setFlowId(f.getFlowId());
            vo.setOrderId(f.getOrderId());
            vo.setFromState(f.getFromState());
            vo.setFromStateDesc(f.getFromState() == 0 ? "新建创建" : OrderStateEnum.getDescByCode(f.getFromState()));
            vo.setToState(f.getToState());
            vo.setToStateDesc(OrderStateEnum.getDescByCode(f.getToState()));
            vo.setOperatorId(f.getOperatorId());
            vo.setRemark(f.getRemark());
            vo.setCreatedAt(f.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
        detailVO.setFlowList(flowVOs);

        SettleBill bill = settleBillMapper.selectBillByOrderId(orderId);
        if (bill != null) {
            SettleBillVO billVO = new SettleBillVO();
            billVO.setBillId(bill.getBillId());
            billVO.setBillSn(bill.getBillSn());
            billVO.setOrderId(bill.getOrderId());
            billVO.setOrderSn(order.getOrderSn());
            billVO.setMerchantId(bill.getMerchantId());
            billVO.setAmount(bill.getAmount());
            billVO.setBillState(bill.getBillState());
            billVO.setBillStateDesc(BillStateEnum.getDescByCode(bill.getBillState()));
            billVO.setInvoiceFlag(bill.getInvoiceFlag());
            billVO.setSettleAt(bill.getSettleAt());
            billVO.setCreatedAt(bill.getCreatedAt());
            detailVO.setSettleBill(billVO);
        }

        if (order.getApplyId() != null) {
            try {
                FlightApplyVO applyVO = flightApplyService.getApplyDetail(order.getApplyId());
                detailVO.setFlightApplyDetail(applyVO);
                if (applyVO != null && applyVO.getRoutePoints() != null) {
                    detailVO.setRoutePoints(applyVO.getRoutePoints());
                }
            } catch (Exception ignored) {}
        }

        if (detailVO.getRoutePoints() == null || detailVO.getRoutePoints().isEmpty()) {
            List<List<Double>> points = new ArrayList<>();
            if (order.getSenderLng() != null && order.getSenderLat() != null) {
                points.add(Arrays.asList(order.getSenderLng(), order.getSenderLat()));
            }
            if (order.getReceiverLng() != null && order.getReceiverLat() != null) {
                points.add(Arrays.asList(order.getReceiverLng(), order.getReceiverLat()));
            }
            detailVO.setRoutePoints(points);
        }

        return detailVO;
    }

    @Override
    public List<CargoOrderVO> listOrders(OrderQueryDTO queryDTO) {
        int page = queryDTO.getPage() != null && queryDTO.getPage() > 0 ? queryDTO.getPage() : 1;
        int size = queryDTO.getSize() != null && queryDTO.getSize() > 0 ? queryDTO.getSize() : 10;
        int offset = (page - 1) * size;

        List<CargoOrder> list = cargoOrderMapper.selectOrderList(
                queryDTO.getMerchantId(), queryDTO.getCustomerId(),
                queryDTO.getOrderState(), queryDTO.getOrderSn(),
                queryDTO.getKeyword(), offset, size);

        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public int countOrders(OrderQueryDTO queryDTO) {
        return cargoOrderMapper.countOrderList(
                queryDTO.getMerchantId(), queryDTO.getCustomerId(),
                queryDTO.getOrderState(), queryDTO.getOrderSn(),
                queryDTO.getKeyword());
    }

    @Override
    public OrderBoardStatsVO getBoardStats(Long merchantId) {
        OrderBoardStatsVO stats = new OrderBoardStatsVO();
        stats.setTotalCount(cargoOrderMapper.countByState(merchantId, null));
        stats.setPendingPayCount(cargoOrderMapper.countByState(merchantId, OrderStateEnum.PENDING_PAY.getCode()));
        stats.setPendingAcceptCount(cargoOrderMapper.countByState(merchantId, OrderStateEnum.PENDING_ACCEPT.getCode()));
        stats.setPendingDispatchCount(cargoOrderMapper.countByState(merchantId, OrderStateEnum.PENDING_DISPATCH.getCode()));
        stats.setPendingTakeoffCount(cargoOrderMapper.countByState(merchantId, OrderStateEnum.PENDING_TAKEOFF.getCode()));
        stats.setFlyingCount(cargoOrderMapper.countByState(merchantId, OrderStateEnum.FLYING.getCode()));
        stats.setDeliveredCount(cargoOrderMapper.countByState(merchantId, OrderStateEnum.DELIVERED.getCode()));
        stats.setCancelledCount(cargoOrderMapper.countByState(merchantId, OrderStateEnum.CANCELLED.getCode()));
        stats.setAbortedCount(cargoOrderMapper.countByState(merchantId, OrderStateEnum.ABORTED.getCode()));
        return stats;
    }

    private CargoOrderVO convertToVO(CargoOrder order) {
        CargoOrderVO vo = new CargoOrderVO();
        org.springframework.beans.BeanUtils.copyProperties(order, vo);
        vo.setSenderLng(order.getSenderLng());
        vo.setSenderLat(order.getSenderLat());
        vo.setReceiverLng(order.getReceiverLng());
        vo.setReceiverLat(order.getReceiverLat());
        vo.setGoodsTypeDesc(GoodsTypeEnum.getDescByCode(order.getGoodsType()));
        vo.setOrderStateDesc(OrderStateEnum.getDescByCode(order.getOrderState()));
        vo.setPayStateDesc(order.getPayState() != null && order.getPayState() == 1 ? "已支付" : "未支付");
        vo.setPayWayDesc(order.getPayWay() != null && order.getPayWay() == 2 ? "企业月结" : "在线支付");

        if (order.getMerchantId() != null) {
            MerchantArchive m = merchantArchiveMapper.selectById(order.getMerchantId());
            if (m != null) vo.setMerchantName(m.getCorpName());
        }
        if (order.getCustomerId() != null) {
            CustomerArchive c = customerArchiveMapper.selectById(order.getCustomerId());
            if (c != null) vo.setCustomerName(c.getRealName());
        }
        if (order.getUavId() != null) {
            UavInfo uav = uavInfoMapper.selectById(order.getUavId());
            if (uav != null) {
                vo.setUavSn(uav.getUavSn());
                vo.setUavModel(uav.getModel());
            }
        }
        if (order.getApplyId() != null) {
            FlightApply apply = flightApplyMapper.selectApplyById(order.getApplyId());
            if (apply != null) {
                vo.setApplySn(apply.getApplySn());
                vo.setApplyApproveState(apply.getApproveState());
                if (apply.getPilotId() != null) {
                    CrewStaff pilot = crewStaffMapper.selectById(apply.getPilotId());
                    if (pilot != null) {
                        vo.setPilotId(pilot.getCrewId());
                        vo.setPilotName(pilot.getRealName());
                    }
                }
            }
        }
        return vo;
    }

}
