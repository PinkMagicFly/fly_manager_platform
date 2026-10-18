package com.fly.platform.alarm.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fly.platform.airspace.entity.FlightApply;
import com.fly.platform.airspace.mapper.FlightApplyMapper;
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
        Long targetApplyId = null;

        // 1. 若关联了订单，进行联动
        CargoOrder order = null;
        if (dto.getOrderId() != null && dto.getOrderId() > 0) {
            order = cargoOrderMapper.selectById(dto.getOrderId());
            if (order != null) {
                if (targetUavId == null) {
                    targetUavId = order.getUavId();
                }
                targetApplyId = order.getApplyId();

                // 若订单处于在途飞行中(50)或待起飞(40)，状态机联动跃迁至 80-异常中断
                if (OrderStateEnum.FLYING.getCode().equals(order.getOrderState())
                        || OrderStateEnum.PENDING_TAKEOFF.getCode().equals(order.getOrderState())) {
                    cargoOrderStateMachine.transition(order, OrderStateEnum.ABORTED.getCode(),
                            operatorId, "机长故障上报: " + dto.getFaultType() + " - " + dto.getDescription(),
                            orderStateFlowMapper);
                    cargoOrderMapper.updateById(order);
                    log.warn("机长故障上报联动：订单[ID={}]已中断跃迁为80-异常中断", order.getOrderId());
                }
            }
        }

        // 2. 无人机状态联动跃迁至 3-故障
        if (targetUavId != null && targetUavId > 0) {
            UavInfo uav = uavInfoMapper.selectById(targetUavId);
            if (uav != null) {
                uav.setState(UavStateEnum.FAULT.getCode());
                uav.updateAudit();
                uavInfoMapper.updateById(uav);
                log.warn("机长故障上报联动：无人机[ID={}, SN={}]状态已置为3-故障", uav.getUavId(), uav.getUavSn());

                // 移除 Redis 在飞集合 trace:flying
                if (stringRedisTemplate != null) {
                    try {
                        stringRedisTemplate.opsForSet().remove("trace:flying", String.valueOf(targetUavId));
                    } catch (Exception e) {
                        log.warn("从Redis在飞集合移除无人机失败: {}", e.getMessage());
                    }
                }
            }
        }

        // 3. 生成 2-严重 级别的告警事件记录
        String sn = "ALM" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + (int) (Math.random() * 9000 + 1000);

        AlarmEvent alarm = new AlarmEvent();
        alarm.setAlarmSn(sn);
        alarm.setLevel(AlarmLevelEnum.SERIOUS.getCode());
        alarm.setTitle("机长故障上报: " + dto.getFaultType());
        alarm.setContent(dto.getDescription());
        alarm.setApplyId(targetApplyId);
        alarm.setUavId(targetUavId);
        alarm.setState(AlarmStateEnum.PENDING.getCode());
        alarm.initAudit();

        alarmStateMachine.appendTimeline(alarm, opName, "机长现场故障上报", dto.getDescription());
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


