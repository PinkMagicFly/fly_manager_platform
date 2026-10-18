package com.fly.platform.trace.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fly.platform.airspace.entity.FlightApply;
import com.fly.platform.airspace.enums.FlightApplyStateEnum;
import com.fly.platform.airspace.mapper.FlightApplyMapper;
import com.fly.platform.asset.entity.CrewStaff;
import com.fly.platform.asset.entity.UavInfo;
import com.fly.platform.asset.mapper.CrewStaffMapper;
import com.fly.platform.asset.mapper.UavInfoMapper;
import com.fly.platform.common.exception.BusinessException;
import com.fly.platform.order.entity.CargoOrder;
import com.fly.platform.order.enums.OrderStateEnum;
import com.fly.platform.order.mapper.CargoOrderMapper;
import com.fly.platform.trace.dto.TraceBatchDTO;
import com.fly.platform.trace.dto.TracePointDTO;
import com.fly.platform.trace.entity.FlyTrace;
import com.fly.platform.trace.mapper.FlyTraceMapper;
import com.fly.platform.trace.service.FlyTraceService;
import com.fly.platform.trace.vo.OrderTraceVO;
import com.fly.platform.trace.vo.TracePointVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 飞行轨迹采集与回放业务服务实现类
 *
 * @author SoarCloud Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlyTraceServiceImpl implements FlyTraceService {

    private final FlyTraceMapper flyTraceMapper;
    private final FlightApplyMapper flightApplyMapper;
    private final CargoOrderMapper cargoOrderMapper;
    private final UavInfoMapper uavInfoMapper;
    private final CrewStaffMapper crewStaffMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchReport(TraceBatchDTO dto) {
        if (dto == null || dto.getPoints() == null || dto.getPoints().isEmpty()) {
            return 0;
        }

        // 1. 守卫校验：任务必须存在
        FlightApply apply = flightApplyMapper.selectApplyById(dto.getApplyId());
        if (apply == null) {
            throw new BusinessException(404, "飞行申报任务不存在: " + dto.getApplyId());
        }

        // 2. 守卫校验：任务必须处于 4-执行中 状态
        if (!FlightApplyStateEnum.EXECUTING.getCode().equals(apply.getApproveState())) {
            throw new BusinessException(400, String.format("飞行任务非执行中状态(当前状态: %s)，拒绝接收航迹上报",
                    FlightApplyStateEnum.getDescByCode(apply.getApproveState())));
        }

        if (apply.getUavId() == null) {
            throw new BusinessException(400, "飞行任务未指派执飞无人机资产，无法关联航迹");
        }

        Long uavId = apply.getUavId();
        List<FlyTrace> traceList = new ArrayList<>();

        for (TracePointDTO p : dto.getPoints()) {
            if (p.getLng() == null || p.getLat() == null) continue;
            FlyTrace trace = new FlyTrace();
            trace.setApplyId(apply.getApplyId());
            trace.setUavId(uavId);
            trace.setLng(p.getLng());
            trace.setLat(p.getLat());
            trace.setAlt(p.getAlt() != null ? p.getAlt() : (apply.getAltPlanned() != null ? apply.getAltPlanned().doubleValue() : 100.0));
            trace.setSpeed(p.getSpeed() != null ? p.getSpeed() : 12.0);
            trace.setBatteryRemain(p.getBatteryRemain() != null ? p.getBatteryRemain() : 95);
            trace.setReportAt(p.getReportAt() != null ? p.getReportAt() : LocalDateTime.now());
            trace.setCreatedAt(LocalDateTime.now());
            traceList.add(trace);
        }

        if (traceList.isEmpty()) {
            return 0;
        }

        // 3. 批量持久化入库
        int inserted = flyTraceMapper.batchInsert(traceList);

        // 4. 更新 Redis 最新位置缓存与在飞集合
        try {
            FlyTrace latest = traceList.stream()
                    .max(Comparator.comparing(FlyTrace::getReportAt))
                    .orElse(traceList.get(traceList.size() - 1));

            Map<String, Object> latestMap = new HashMap<>();
            latestMap.put("uavId", uavId);
            latestMap.put("applyId", apply.getApplyId());
            latestMap.put("lng", latest.getLng());
            latestMap.put("lat", latest.getLat());
            latestMap.put("alt", latest.getAlt());
            latestMap.put("speed", latest.getSpeed());
            latestMap.put("batteryRemain", latest.getBatteryRemain());
            latestMap.put("reportAt", latest.getReportAt().toString());

            String json = objectMapper.writeValueAsString(latestMap);
            stringRedisTemplate.opsForValue().set("trace:latest:" + uavId, json, 10, TimeUnit.MINUTES);
            stringRedisTemplate.opsForSet().add("trace:flying", String.valueOf(uavId));
        } catch (Exception e) {
            log.warn("写入 Redis 瞬时航迹缓存失败: {}", e.getMessage());
        }

        return inserted;
    }

    @Override
    public OrderTraceVO getOrderTrace(Long orderId) {
        CargoOrder order = cargoOrderMapper.selectOrderById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在: " + orderId);
        }

        OrderTraceVO vo = new OrderTraceVO();
        vo.setOrderId(order.getOrderId());
        vo.setOrderState(order.getOrderState());
        vo.setOrderStateDesc(OrderStateEnum.getDescByCode(order.getOrderState()));

        if (order.getApplyId() != null) {
            FlightApply apply = flightApplyMapper.selectApplyById(order.getApplyId());
            if (apply != null) {
                vo.setApplyId(apply.getApplyId());
                vo.setApplySn(apply.getApplySn());
                vo.setAltPlanned(apply.getAltPlanned());
                vo.setPlanLine(apply.parseRoutePoints());

                if (apply.getUavId() != null) {
                    UavInfo uav = uavInfoMapper.selectById(apply.getUavId());
                    if (uav != null) {
                        vo.setUavSn(uav.getUavSn());
                        vo.setUavModel(uav.getModel());
                    }
                }

                if (apply.getPilotId() != null) {
                    CrewStaff pilot = crewStaffMapper.selectById(apply.getPilotId());
                    if (pilot != null) {
                        vo.setPilotName(pilot.getRealName());
                    }
                }

                List<FlyTrace> traces = flyTraceMapper.selectByApplyId(apply.getApplyId());
                List<TracePointVO> actualPoints = traces.stream().map(t -> {
                    TracePointVO p = new TracePointVO();
                    p.setLng(t.getLng());
                    p.setLat(t.getLat());
                    p.setAlt(t.getAlt());
                    p.setSpeed(t.getSpeed());
                    p.setBatteryRemain(t.getBatteryRemain());
                    p.setReportAt(t.getReportAt());
                    return p;
                }).collect(Collectors.toList());
                vo.setActual(actualPoints);
            }
        }

        return vo;
    }
}
