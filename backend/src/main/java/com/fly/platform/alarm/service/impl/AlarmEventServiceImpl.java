package com.fly.platform.alarm.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fly.platform.airspace.entity.FlightApply;
import com.fly.platform.airspace.mapper.FlightApplyMapper;
import com.fly.platform.airspace.enums.FlightApplyStateEnum;
import java.util.Arrays;

import com.fly.platform.alarm.domain.AlarmStateMachine;
import com.fly.platform.alarm.dto.AlarmCreateDTO;
import com.fly.platform.alarm.dto.AlarmProcessDTO;
import com.fly.platform.alarm.dto.AlarmReportDTO;
import com.fly.platform.alarm.entity.AlarmEvent;
import com.fly.platform.alarm.enums.AlarmLevelEnum;
import com.fly.platform.alarm.enums.AlarmSourceEnum;
import com.fly.platform.alarm.enums.AlarmStateEnum;
import com.fly.platform.alarm.mapper.AlarmEventMapper;
import com.fly.platform.alarm.service.AlarmEventService;
import com.fly.platform.alarm.vo.AlarmVO;
import com.fly.platform.asset.entity.UavInfo;
import com.fly.platform.asset.enums.UavStateEnum;
import com.fly.platform.asset.mapper.UavInfoMapper;
import com.fly.platform.common.api.PageResult;
import com.fly.platform.common.exception.BusinessException;
import com.fly.platform.order.domain.CargoOrderStateMachine;
import com.fly.platform.order.entity.CargoOrder;
import com.fly.platform.order.enums.OrderStateEnum;
import com.fly.platform.order.mapper.CargoOrderMapper;
import com.fly.platform.order.mapper.OrderStateFlowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 实时告警事件业务服务实现
 *
 * @author SoarCloud Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmEventServiceImpl implements AlarmEventService {

    private final AlarmEventMapper alarmEventMapper;
    private final AlarmStateMachine alarmStateMachine;
    private final CargoOrderMapper cargoOrderMapper;
    private final OrderStateFlowMapper orderStateFlowMapper;
    private final CargoOrderStateMachine cargoOrderStateMachine;
    private final UavInfoMapper uavInfoMapper;
    private final FlightApplyMapper flightApplyMapper;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public PageResult<AlarmVO> page(Integer level, Integer state, String keyword, int pageNo, int pageSize) {
        LambdaQueryWrapper<AlarmEvent> qw = new LambdaQueryWrapper<>();
        if (level != null && level > 0) {
            qw.eq(AlarmEvent::getLevel, level);
        }
        if (state != null && state >= 0) {
            qw.eq(AlarmEvent::getState, state);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            qw.and(w -> w.like(AlarmEvent::getAlarmSn, keyword.trim())
                    .or().like(AlarmEvent::getTitle, keyword.trim())
                    .or().like(AlarmEvent::getContent, keyword.trim()));
        }
        qw.orderByDesc(AlarmEvent::getAlarmId);

        Page<AlarmEvent> page = alarmEventMapper.selectPage(new Page<>(pageNo, pageSize), qw);
        List<AlarmVO> voList = page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList());
        return new PageResult<>(voList, page.getTotal(), (long) pageNo, (long) pageSize);
    }

    @Override
    public AlarmVO getDetail(Long id) {
        AlarmEvent alarm = alarmEventMapper.selectById(id);
        if (alarm == null) {
            throw new BusinessException(404, "告警事件不存在: " + id);
        }
        return convertToVO(alarm);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createManual(AlarmCreateDTO dto) {
        String sn = "ALM" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + (int) (Math.random() * 9000 + 1000);

        AlarmEvent alarm = new AlarmEvent();
        alarm.setAlarmSn(sn);
        alarm.setLevel(dto.getLevel());
        alarm.setTitle(dto.getTitle());
        alarm.setContent(dto.getContent());
        alarm.setUavId(dto.getUavId());
        alarm.setApplyId(dto.getApplyId());
        alarm.setState(AlarmStateEnum.PENDING.getCode());
        alarm.initAudit();

        String opName = getCurrentLoginName();
        alarmStateMachine.appendTimeline(alarm, opName, "登记入库", "值班人员手工登记异常告警");

        alarmEventMapper.insert(alarm);
        log.info("手工登记告警入库: alarmId={}, alarmSn={}", alarm.getAlarmId(), alarm.getAlarmSn());
        return alarm.getAlarmId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void process(Long id, AlarmProcessDTO dto) {
        AlarmEvent alarm = alarmEventMapper.selectById(id);
        if (alarm == null) {
            throw new BusinessException(404, "告警事件不存在: " + id);
        }
        String opName = getCurrentLoginName();
        alarmStateMachine.process(alarm, opName, dto.getNote());
        alarmEventMapper.updateById(alarm);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void close(Long id, AlarmProcessDTO dto) {
        AlarmEvent alarm = alarmEventMapper.selectById(id);
        if (alarm == null) {
            throw new BusinessException(404, "告警事件不存在: " + id);
        }
        String opName = getCurrentLoginName();
        alarmStateMachine.close(alarm, opName, dto.getNote());
        alarmEventMapper.updateById(alarm);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reportFault(AlarmReportDTO dto) {
        Long operatorId = 0L;
        String opName = getCurrentLoginName();
        if (StpUtil.isLogin()) {
            try {
                operatorId = StpUtil.getLoginIdAsLong();
            } catch (Exception ignored) {}
        }

        Long targetUavId = dto.getUavId();
        FlightApply apply = null;
        CargoOrder order = null;

        // 1. 若指定了无人机，优先寻找当前正在由该无人机执飞或已核准的任务
        if (targetUavId != null && targetUavId > 0) {
            apply = flightApplyMapper.selectOne(new LambdaQueryWrapper<FlightApply>()
                    .eq(FlightApply::getUavId, targetUavId)
                    .in(FlightApply::getApproveState, Arrays.asList(FlightApplyStateEnum.EXECUTING.getCode(), FlightApplyStateEnum.APPROVED.getCode()))
                    .last("LIMIT 1"));
        }

        // 2. 若指定了订单，或由 apply 反查到订单
        if (dto.getOrderId() != null && dto.getOrderId() > 0) {
            order = cargoOrderMapper.selectById(dto.getOrderId());
            if (order != null && apply == null && order.getApplyId() != null) {
                apply = flightApplyMapper.selectById(order.getApplyId());
            }
        }
        if (apply != null && apply.getOrderId() != null && apply.getOrderId() > 0 && order == null) {
            order = cargoOrderMapper.selectById(apply.getOrderId());
        }

        if (targetUavId == null && apply != null) {
            targetUavId = apply.getUavId();
        }
        if (targetUavId == null && order != null) {
            targetUavId = order.getUavId();
        }
        Long targetApplyId = apply != null ? apply.getApplyId() : (order != null ? order.getApplyId() : null);

        // 3. 飞行任务联动取消：审批台状态置为 3-已取消
        if (apply != null) {
            apply.setApproveState(FlightApplyStateEnum.CANCELED.getCode());
            apply.setApproveNote("机长现场故障上报，任务已取消并释放空域及场站资源: " + dto.getFaultType());
            apply.updateAudit();
            flightApplyMapper.updateById(apply);
            log.warn("机长故障上报联动：飞行计划[ID={}, SN={}]状态已置为3-已取消", apply.getApplyId(), apply.getApplySn());
        }

        // 4. 运单联动：重新变更为 30-待调度，并解绑故障无人机以允许调度台重新指派
        if (order != null) {
            Integer currentState = order.getOrderState();
            if (OrderStateEnum.FLYING.getCode().equals(currentState)
                    || OrderStateEnum.PENDING_TAKEOFF.getCode().equals(currentState)) {
                cargoOrderStateMachine.transition(order, OrderStateEnum.PENDING_DISPATCH.getCode(),
                        operatorId, "机长故障上报: " + dto.getFaultType() + "，飞行任务已取消，订单重新变更为待调度",
                        orderStateFlowMapper);
                cargoOrderMapper.update(null, new LambdaUpdateWrapper<CargoOrder>()
                        .eq(CargoOrder::getOrderId, order.getOrderId())
                        .set(CargoOrder::getUavId, null)
                        .set(CargoOrder::getOrderState, OrderStateEnum.PENDING_DISPATCH.getCode())
                        .set(CargoOrder::getUpdatedAt, LocalDateTime.now()));
                order.setUavId(null);
                log.warn("机长故障上报联动：订单[ID={}]状态已变更为30-待调度并解绑故障机", order.getOrderId());
            }
        }

        // 5. 无人机状态联动置为 3-故障
        if (targetUavId != null && targetUavId > 0) {
            UavInfo uav = uavInfoMapper.selectById(targetUavId);
            if (uav != null) {
                uav.setState(UavStateEnum.FAULT.getCode());
                uav.updateAudit();
                uavInfoMapper.updateById(uav);
                log.warn("机长故障上报联动：无人机[ID={}, SN={}]状态已置为3-故障", uav.getUavId(), uav.getUavSn());
            }

            // 移除 Redis 在飞集合 trace:flying
            if (stringRedisTemplate != null) {
                try {
                    stringRedisTemplate.opsForSet().remove("trace:flying", String.valueOf(targetUavId));
                } catch (Exception e) {
                    log.warn("从Redis在飞集合移除无人机失败: {}", e.getMessage());
                }
            }
        }

        // 6. 生成 2-严重 级别的告警事件记录（严格限制字段长度，防止 SQL 截断异常）
        String sn = "ALM" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + (int) (Math.random() * 9000 + 1000);

        AlarmEvent alarm = new AlarmEvent();
        alarm.setAlarmSn(sn);
        alarm.setLevel(AlarmLevelEnum.SERIOUS.getCode());

        String safeTitle = "机长故障上报: " + dto.getFaultType();
        if (safeTitle.length() > 30) safeTitle = safeTitle.substring(0, 30);
        alarm.setTitle(safeTitle);

        String safeContent = dto.getDescription() != null ? dto.getDescription().trim() : "现场故障上报";
        if (safeContent.length() > 240) safeContent = safeContent.substring(0, 240);
        alarm.setContent(safeContent);

        alarm.setApplyId(targetApplyId);
        alarm.setUavId(targetUavId);
        alarm.setState(AlarmStateEnum.PENDING.getCode());
        alarm.initAudit();

        alarmStateMachine.appendTimeline(alarm, opName, "机长现场故障上报", safeContent);
        alarmEventMapper.insert(alarm);

        Map<String, Object> result = new HashMap<>();
        result.put("alarmId", alarm.getAlarmId());
        result.put("alarmSn", alarm.getAlarmSn());
        result.put("level", AlarmLevelEnum.SERIOUS.getCode());
        result.put("levelLabel", AlarmLevelEnum.SERIOUS.getDesc());
        if (order != null) {
            result.put("orderId", order.getOrderId());
            result.put("orderState", order.getOrderState());
        }
        if (apply != null) {
            result.put("applyId", apply.getApplyId());
            result.put("applyState", apply.getApproveState());
        }
        return result;
    }

    private AlarmVO convertToVO(AlarmEvent alarm) {
        AlarmVO vo = new AlarmVO();
        vo.setAlarmId(alarm.getAlarmId());
        vo.setAlarmSn(alarm.getAlarmSn());
        vo.setApplyId(alarm.getApplyId());
        vo.setUavId(alarm.getUavId());
        vo.setLevel(alarm.getLevel());
        vo.setLevelLabel(AlarmLevelEnum.getDescByCode(alarm.getLevel()));
        vo.setTitle(alarm.getTitle());
        vo.setContent(alarm.getContent());
        vo.setState(alarm.getState());
        vo.setStateLabel(AlarmStateEnum.getDescByCode(alarm.getState()));
        vo.setTimeline(AlarmStateMachine.parseTimeline(alarm.getTimeline()));
        vo.setHandledAt(alarm.getHandledAt());
        vo.setCreatedAt(alarm.getCreatedAt());

        if (alarm.getTitle() != null && alarm.getTitle().contains("机长故障上报")) {
            vo.setSource(AlarmSourceEnum.FAULT_REPORT.getCode());
            vo.setSourceLabel(AlarmSourceEnum.FAULT_REPORT.getDesc());
        } else {
            vo.setSource(AlarmSourceEnum.MANUAL_RECORD.getCode());
            vo.setSourceLabel(AlarmSourceEnum.MANUAL_RECORD.getDesc());
        }

        if (alarm.getUavId() != null) {
            UavInfo uav = uavInfoMapper.selectById(alarm.getUavId());
            if (uav != null) {
                vo.setUavSn(uav.getUavSn());
            }
        }
        if (alarm.getApplyId() != null) {
            FlightApply apply = flightApplyMapper.selectById(alarm.getApplyId());
            if (apply != null && apply.getOrderId() != null && apply.getOrderId() > 0) {
                vo.setOrderId(apply.getOrderId());
                CargoOrder order = cargoOrderMapper.selectById(apply.getOrderId());
                if (order != null) {
                    vo.setOrderSn(order.getOrderSn());
                }
            }
        }
        return vo;
    }

    private String getCurrentLoginName() {
        if (StpUtil.isLogin()) {
            try {
                Object name = StpUtil.getSession().get("loginName");
                if (name != null) return name.toString();
                return "user_" + StpUtil.getLoginId();
            } catch (Exception ignored) {}
        }
        return "机长/现场运维";
    }
}


