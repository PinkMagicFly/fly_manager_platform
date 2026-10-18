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
import org.springframework.data.redis.core.StringRedisTemplate;


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
    private final com.fly.platform.asset.service.SiteSnapService siteSnapService;
    private final com.fly.platform.airspace.mapper.AirspaceBlockMapper airspaceBlockMapper;
    private final com.fly.platform.airspace.domain.AirspaceCollisionEngine collisionEngine;
    private final StringRedisTemplate stringRedisTemplate;



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

        // 要求2：接单前置守卫校验（运单起止地址必须落入起降场站服务覆盖范围内，否则禁止接单并引导拒单）
        try {
            siteSnapService.snapSite(order.getSenderLng(), order.getSenderLat());
            siteSnapService.snapSite(order.getReceiverLng(), order.getReceiverLat());
        } catch (BusinessException e) {
            throw new BusinessException(400, "接单失败：该运单起止地址超出任何起降场站服务覆盖范围，无法承运，请执行拒单");
        }

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
        int altPlanned = dto.getAltPlanned() != null ? dto.getAltPlanned() : 100;

        // 问题3前置守卫：发起调度时检测机长在所选时段是否已被其他已批准(1)或执行中(4)任务排他锁定
        List<FlightApply> pilotConflicts = flightApplyMapper.selectOverlappingPilotTasks(
                dto.getPilotId(), startAt, endAt, existingApply != null ? existingApply.getApplyId() : null);
        if (pilotConflicts != null && !pilotConflicts.isEmpty()) {
            throw new BusinessException(400, String.format("机长时段冲突拦截：责任机长[%s]在所选时段已被任务【%s】排他占用，不可重复调度指派，请更换机长或调整起降时段！",
                    pilot.getRealName(), pilotConflicts.get(0).getApplySn()));
        }

        String routeLine = dto.getRouteLine();

        if (routeLine == null || routeLine.trim().isEmpty()) {
            throw new BusinessException(400, "规划航线折线坐标序列不能为空");
        }

        // 校验链 1：寄件/收件坐标吸附起飞场站A与降落场站B
        com.fly.platform.asset.entity.BaseSite siteA = siteSnapService.snapSite(order.getSenderLng(), order.getSenderLat());
        com.fly.platform.asset.entity.BaseSite siteB = siteSnapService.snapSite(order.getReceiverLng(), order.getReceiverLat());

        // 校验链 2：解析 routeLine 首末点，与两场站坐标距离 ≤ 50 米（防端点脱离场站）
        List<List<Double>> routePoints = com.fly.platform.common.util.GeoUtils.parseWktLineString(routeLine);
        if (routePoints == null || routePoints.size() < 2) {
            throw new BusinessException(400, "规划航线折线坐标点数不足，至少包含起降2个场站点");
        }
        List<Double> startPoint = routePoints.get(0);
        List<Double> endPoint = routePoints.get(routePoints.size() - 1);
        double distStart = com.fly.platform.common.util.GeoUtils.haversineMeters(startPoint.get(0), startPoint.get(1), siteA.getLongitude(), siteA.getLatitude());
        if (distStart > 50.0) {
            throw new BusinessException(400, String.format("航线起点偏离起飞场站[%s]超过50米 (当前偏离: %.1f米)", siteA.getSiteName(), distStart));
        }
        double distEnd = com.fly.platform.common.util.GeoUtils.haversineMeters(endPoint.get(0), endPoint.get(1), siteB.getLongitude(), siteB.getLatitude());
        if (distEnd > 50.0) {
            throw new BusinessException(400, String.format("航线终点偏离降落场站[%s]超过50米 (当前偏离: %.1f米)", siteB.getSiteName(), distEnd));
        }

        // 校验链 3：调 AirspaceCollisionEngine 对整条折线做禁飞/限高/时段校验
        List<com.fly.platform.airspace.entity.AirspaceBlock> allBlocks = airspaceBlockMapper.selectAllActiveBlocks();
        com.fly.platform.airspace.vo.AirspaceValidateResultVO validateResult = collisionEngine.validateRoute(routePoints, altPlanned, startAt, endAt, allBlocks);
        if (!Boolean.TRUE.equals(validateResult.getPass())) {
            throw new BusinessException(400, "航线空域合规校验未通过: " + validateResult.getMessage());
        }

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
        // 联动在飞无人机心跳维护 -> 加入 trace:flying
        if (order.getUavId() != null) {
            try {
                stringRedisTemplate.opsForSet().add("trace:flying", String.valueOf(order.getUavId()));
            } catch (Exception e) {
                log.warn("维护在飞无人机Redis心跳集合失败: {}", e.getMessage());
            }
        }

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
            try {
                stringRedisTemplate.opsForSet().remove("trace:flying", String.valueOf(order.getUavId()));
            } catch (Exception e) {
                log.warn("心跳维护: 从Redis移除在飞无人机失败: {}", e.getMessage());
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
            try {
                stringRedisTemplate.opsForSet().remove("trace:flying", String.valueOf(order.getUavId()));
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

        if (order.getUavId() != null) {
            try {
                stringRedisTemplate.opsForSet().remove("trace:flying", String.valueOf(order.getUavId()));
            } catch (Exception e) {
                log.warn("异常中断从Redis移除在飞无人机失败: {}", e.getMessage());
            }
        }
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
        // 预检起止坐标是否落入服务场站覆盖范围（≤5000米）
        try {
            if (order.getSenderLng() != null && order.getSenderLat() != null
                    && order.getReceiverLng() != null && order.getReceiverLat() != null) {
                siteSnapService.snapSite(order.getSenderLng(), order.getSenderLat());
                siteSnapService.snapSite(order.getReceiverLng(), order.getReceiverLat());
                vo.setOutOfServiceRange(false);
            } else {
                vo.setOutOfServiceRange(true);
            }
        } catch (Exception e) {
            vo.setOutOfServiceRange(true);
        }

        if (order.getApplyId() != null) {
            FlightApply apply = flightApplyMapper.selectApplyById(order.getApplyId());
            if (apply != null) {
                vo.setApplySn(apply.getApplySn());
                vo.setApplyApproveState(apply.getApproveState());
                vo.setRoutePoints(apply.parseRoutePoints());
                vo.setApproveNote(apply.getApproveNote());
                if (apply.getPilotId() != null) {
                    CrewStaff pilot = crewStaffMapper.selectById(apply.getPilotId());
                    if (pilot != null) {
                        vo.setPilotId(pilot.getCrewId());
                        vo.setPilotName(pilot.getRealName());
                    }
                }
            }
        }

        // 若订单已取消(70)或异常中断(80)，尝试从最新流水提取取消/拒单/中断原因
        if (OrderStateEnum.CANCELLED.getCode().equals(order.getOrderState())
                || OrderStateEnum.ABORTED.getCode().equals(order.getOrderState())) {
            List<OrderStateFlow> flows = orderStateFlowMapper.selectByOrderId(order.getOrderId());
            if (flows != null && !flows.isEmpty()) {
                vo.setCancelReason(flows.get(flows.size() - 1).getRemark());
            }
        }
        return vo;
    }

}
