package com.fly.platform.trace;

import com.fly.platform.airspace.entity.FlightApply;
import com.fly.platform.airspace.enums.FlightApplyStateEnum;
import com.fly.platform.airspace.mapper.FlightApplyMapper;
import com.fly.platform.airspace.service.AirspaceBlockService;
import com.fly.platform.airspace.vo.AirspaceMapDataVO;
import com.fly.platform.order.entity.CargoOrder;
import com.fly.platform.order.enums.OrderStateEnum;
import com.fly.platform.order.mapper.CargoOrderMapper;
import com.fly.platform.order.service.CargoOrderService;
import com.fly.platform.trace.dto.TraceBatchDTO;
import com.fly.platform.trace.dto.TracePointDTO;
import com.fly.platform.trace.service.FlyTraceService;
import com.fly.platform.trace.vo.OrderTraceVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 阶段5B 空域态势聚合与无人机心跳生命周期单测
 *
 * @author SoarCloud Team
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class MapDataAggregateTest {

    @Autowired
    private AirspaceBlockService airspaceBlockService;
    @Autowired
    private CargoOrderService cargoOrderService;
    @Autowired
    private CargoOrderMapper cargoOrderMapper;
    @Autowired
    private FlightApplyMapper flightApplyMapper;
    @Autowired
    private FlyTraceService flyTraceService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("测试用例1：空域一张图聚合接口 map-data 返回三区网格、场站、在飞无人机与已批航线四类数据")
    void testMapDataAggregation() {
        AirspaceMapDataVO mapData = airspaceBlockService.getMapData();
        assertThat(mapData).isNotNull();
        assertThat(mapData.getBlocks()).isNotEmpty();
        assertThat(mapData.getSites()).isNotEmpty();
        assertThat(mapData.getFlyingUavs()).isNotNull();
        assertThat(mapData.getApprovedRoutes()).isNotNull();
    }

    @Test
    @DisplayName("测试用例2：在途监控全周期-起飞加心跳->在飞图层可见->送达移除心跳->降落清除闭环")
    void testFlyingUavHeartbeatLifecycle() {
        Long uavId = 1L;
        Long pilotId = 1L;

        // 1. 创建订单并置为 40-待执飞
        CargoOrder order = new CargoOrder();
        order.setOrderSn("ORDTRACE" + System.currentTimeMillis());
        order.setCustomerId(1L);
        order.setMerchantId(1L);
        order.setSenderAddr("深圳南山科技园枢纽站");
        order.setReceiverAddr("深圳福田深业上城");
        order.setSenderGeo("POINT(113.943000 22.540000)");
        order.setReceiverGeo("POINT(114.062000 22.553000)");
        order.setGoodsType(1);
        order.setGoodsWeight(new BigDecimal("3.50"));
        order.setFeeTotal(new BigDecimal("75.00"));
        order.setOrderState(OrderStateEnum.PENDING_TAKEOFF.getCode());
        order.setPayState(1);
        order.setUavId(uavId);
        order.initAudit();
        cargoOrderMapper.insertOrder(order);

        // 2. 创建关联任务并置为 1-已批准
        FlightApply apply = new FlightApply();
        apply.setApplySn("FATRACE" + System.currentTimeMillis());
        apply.setOrderId(order.getOrderId());
        apply.setMerchantId(1L);
        apply.setUavId(uavId);
        apply.setPilotId(pilotId);
        apply.setRouteLine("LINESTRING(113.943000 22.540000, 114.000000 22.545000, 114.062000 22.553000)");
        apply.setAltPlanned(110);
        apply.setStartAt(LocalDateTime.now().minusMinutes(5));
        apply.setEndAt(LocalDateTime.now().plusHours(1));
        apply.setCheckState(1);
        apply.setApproveState(FlightApplyStateEnum.APPROVED.getCode());
        apply.initAudit();
        flightApplyMapper.insertApply(apply);

        order.setApplyId(apply.getApplyId());
        cargoOrderMapper.updateOrderDispatch(order.getOrderId(), order.getOrderState(), uavId, apply.getApplyId());

        // 3. 起飞 (40 -> 50)，断言无人机加入在飞集合 trace:flying
        CargoOrder flyingOrder = cargoOrderService.takeoffOrder(order.getOrderId(), pilotId);
        assertThat(flyingOrder.getOrderState()).isEqualTo(OrderStateEnum.FLYING.getCode());
        assertThat(stringRedisTemplate.opsForSet().isMember("trace:flying", String.valueOf(uavId))).isTrue();

        // 4. 上报在途轨迹点
        TraceBatchDTO traceBatchDTO = new TraceBatchDTO();
        traceBatchDTO.setApplyId(apply.getApplyId());
        List<TracePointDTO> ptList = new ArrayList<>();
        TracePointDTO pt = new TracePointDTO();
        pt.setLng(113.970000);
        pt.setLat(22.542000);
        pt.setAlt(110.0);
        pt.setSpeed(13.2);
        pt.setBatteryRemain(92);
        pt.setReportAt(LocalDateTime.now());
        ptList.add(pt);
        traceBatchDTO.setPoints(ptList);
        flyTraceService.batchReport(traceBatchDTO);

        // 5. 态势接口包含该在飞无人机
        AirspaceMapDataVO mapData = airspaceBlockService.getMapData();
        assertThat(mapData.getFlyingUavs()).anyMatch(u -> u.getUavId().equals(uavId) && u.getLng().equals(113.970000));

        // 6. 订单轨迹回放包含 planLine 与 actual
        OrderTraceVO traceVO = flyTraceService.getOrderTrace(order.getOrderId());
        assertThat(traceVO.getPlanLine()).hasSize(3);
        assertThat(traceVO.getActual()).hasSize(1);
        assertThat(traceVO.getActual().get(0).getLng()).isEqualTo(113.970000);

        // 7. 送达签收 (50 -> 60)，心跳降落清除
        CargoOrder deliveredOrder = cargoOrderService.deliverOrder(order.getOrderId(), pilotId);
        assertThat(deliveredOrder.getOrderState()).isEqualTo(OrderStateEnum.DELIVERED.getCode());
        assertThat(stringRedisTemplate.opsForSet().isMember("trace:flying", String.valueOf(uavId))).isFalse();
    }
}
