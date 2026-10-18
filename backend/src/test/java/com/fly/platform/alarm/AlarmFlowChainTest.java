package com.fly.platform.alarm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fly.platform.alarm.dto.AlarmProcessDTO;
import com.fly.platform.alarm.dto.AlarmReportDTO;
import com.fly.platform.alarm.enums.AlarmLevelEnum;
import com.fly.platform.alarm.enums.AlarmStateEnum;
import com.fly.platform.alarm.service.AlarmEventService;
import com.fly.platform.alarm.vo.AlarmVO;
import com.fly.platform.asset.entity.UavInfo;
import com.fly.platform.asset.enums.UavStateEnum;
import com.fly.platform.asset.mapper.UavInfoMapper;
import com.fly.platform.common.exception.BusinessException;
import com.fly.platform.order.entity.CargoOrder;
import com.fly.platform.order.entity.OrderStateFlow;
import com.fly.platform.order.enums.OrderStateEnum;
import com.fly.platform.order.mapper.CargoOrderMapper;
import com.fly.platform.order.mapper.OrderStateFlowMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 应急告警处置全生命周期业务流转测试
 * 覆盖：机长突发故障上报 -> 严重告警生成 -> 订单80异常中断 -> 无人机3故障 -> 处置中 -> 办结关闭及单向守卫
 *
 * @author SoarCloud Team
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AlarmFlowChainTest {

    @Autowired
    private AlarmEventService alarmEventService;

    @Autowired
    private CargoOrderMapper cargoOrderMapper;

    @Autowired
    private OrderStateFlowMapper orderStateFlowMapper;

    @Autowired
    private UavInfoMapper uavInfoMapper;

    @Test
    @DisplayName("告警全闭环测试: 故障上报联动严重告警、订单80异常中断、无人机3故障、处置与办结全流程")
    void testAlarmFullLifecycleChain() {
        // 1. 造一单处于 50-飞行中 的测试运单
        CargoOrder flyingOrder = new CargoOrder();
        flyingOrder.setOrderSn("ORD_FLY_" + (System.currentTimeMillis() % 1000000000L));
        flyingOrder.setCustomerId(1L);
        flyingOrder.setMerchantId(1L);
        flyingOrder.setSenderAddr("深圳湾科技生态园A座");
        flyingOrder.setReceiverAddr("深业上城商圈楼顶");
        flyingOrder.setSenderGeo("POINT(113.952 22.532)");
        flyingOrder.setReceiverGeo("POINT(114.062 22.553)");
        flyingOrder.setGoodsType(1);
        flyingOrder.setGoodsWeight(new BigDecimal("3.5"));
        flyingOrder.setFeeTotal(new BigDecimal("75.00"));
        flyingOrder.setOrderState(OrderStateEnum.FLYING.getCode());
        flyingOrder.setUavId(1L);
        flyingOrder.setApplyId(1L);
        flyingOrder.setIsDeleted(0);
        cargoOrderMapper.insert(flyingOrder);

        // 确保无人机初始为非故障态
        UavInfo uav = uavInfoMapper.selectById(1L);
        assertThat(uav).isNotNull();

        // 2. 机长现场故障上报
        AlarmReportDTO reportDTO = new AlarmReportDTO();
        reportDTO.setOrderId(flyingOrder.getOrderId());
        reportDTO.setUavId(1L);
        reportDTO.setFaultType("旋翼三号电机高温异常");
        reportDTO.setDescription("飞行高度100米时电机温度瞬时超过95度，已触发迫降避险");

        Map<String, Object> reportResult = alarmEventService.reportFault(reportDTO);
        assertThat(reportResult).isNotNull();
        Long alarmId = (Long) reportResult.get("alarmId");
        assertThat(alarmId).isNotNull();
        assertThat(reportResult.get("level")).isEqualTo(AlarmLevelEnum.SERIOUS.getCode());

        // 3. 断言订单状态联动跃迁至 80-异常中断
        CargoOrder abortedOrder = cargoOrderMapper.selectById(flyingOrder.getOrderId());
        assertThat(abortedOrder.getOrderState()).isEqualTo(OrderStateEnum.ABORTED.getCode());

        // 断言订单状态流水记录
        List<OrderStateFlow> flows = orderStateFlowMapper.selectList(
                new LambdaQueryWrapper<OrderStateFlow>()
                        .eq(OrderStateFlow::getOrderId, flyingOrder.getOrderId())
                        .eq(OrderStateFlow::getToState, OrderStateEnum.ABORTED.getCode())
        );
        assertThat(flows).isNotEmpty();
        assertThat(flows.get(0).getRemark()).contains("机长故障上报");

        // 断言无人机状态联动置为 3-故障
        UavInfo faultUav = uavInfoMapper.selectById(1L);
        assertThat(faultUav.getState()).isEqualTo(UavStateEnum.FAULT.getCode());

        // 4. 断言告警详情初始状态为 0-未处置
        AlarmVO initialAlarm = alarmEventService.getDetail(alarmId);
        assertThat(initialAlarm.getState()).isEqualTo(AlarmStateEnum.PENDING.getCode());
        assertThat(initialAlarm.getLevel()).isEqualTo(AlarmLevelEnum.SERIOUS.getCode());
        assertThat(initialAlarm.getTimeline()).isNotEmpty();
        assertThat(initialAlarm.getTimeline().get(0).getAction()).contains("机长现场故障上报");

        // 5. 调度中心开始处置告警 (0 -> 1)
        AlarmProcessDTO processDTO = new AlarmProcessDTO();
        processDTO.setNote("调度员已联系备降坪并派出巡检车接管设备");
        alarmEventService.process(alarmId, processDTO);

        AlarmVO processingAlarm = alarmEventService.getDetail(alarmId);
        assertThat(processingAlarm.getState()).isEqualTo(AlarmStateEnum.PROCESSING.getCode());
        assertThat(processingAlarm.getTimeline()).hasSizeGreaterThanOrEqualTo(2);

        // 6. 办结关闭告警 (1 -> 2)
        AlarmProcessDTO closeDTO = new AlarmProcessDTO();
        closeDTO.setNote("无人机已安全转运回厂维修，险情解除");
        alarmEventService.close(alarmId, closeDTO);

        AlarmVO closedAlarm = alarmEventService.getDetail(alarmId);
        assertThat(closedAlarm.getState()).isEqualTo(AlarmStateEnum.CLOSED.getCode());
        assertThat(closedAlarm.getHandledAt()).isNotNull();

        // 7. 守卫拦截验证：对已关闭的告警再次重复关闭必须抛出业务异常
        assertThatThrownBy(() -> alarmEventService.close(alarmId, closeDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已");
    }
}
