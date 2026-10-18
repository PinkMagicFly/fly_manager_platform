package com.fly.platform.trace;

import com.fly.platform.airspace.entity.FlightApply;
import com.fly.platform.airspace.enums.FlightApplyStateEnum;
import com.fly.platform.airspace.mapper.FlightApplyMapper;
import com.fly.platform.common.exception.BusinessException;
import com.fly.platform.trace.dto.TraceBatchDTO;
import com.fly.platform.trace.dto.TracePointDTO;
import com.fly.platform.trace.entity.FlyTrace;
import com.fly.platform.trace.mapper.FlyTraceMapper;
import com.fly.platform.trace.service.FlyTraceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 阶段5B 航迹高频批量上报与在途监控单测
 *
 * @author SoarCloud Team
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TraceBatchReportTest {

    @Autowired
    private FlyTraceService flyTraceService;

    @Autowired
    private FlyTraceMapper flyTraceMapper;

    @Autowired
    private FlightApplyMapper flightApplyMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private FlightApply createMockFlightApply(Integer approveState) {
        FlightApply apply = new FlightApply();
        apply.setApplySn("FA" + System.currentTimeMillis() + (int) (Math.random() * 900));
        apply.setMerchantId(1L);
        apply.setUavId(1L);
        apply.setPilotId(1L);
        apply.setRouteLine("LINESTRING(113.943000 22.540000, 114.020000 22.550000, 114.062000 22.553000)");
        apply.setAltPlanned(110);
        apply.setStartAt(LocalDateTime.now().minusMinutes(10));
        apply.setEndAt(LocalDateTime.now().plusHours(1));
        apply.setCheckState(1);
        apply.setApproveState(approveState);
        apply.initAudit();
        flightApplyMapper.insertApply(apply);
        return apply;
    }

    @Test
    @DisplayName("测试用例1：执行中任务批量上报航迹成功，验证落库条数、升序排序及Redis最新位置缓存")
    void testBatchReportSuccess() {
        // 构造处于 4-执行中 状态的任务
        FlightApply apply = createMockFlightApply(FlightApplyStateEnum.EXECUTING.getCode());
        Long applyId = apply.getApplyId();
        Long uavId = apply.getUavId();

        TraceBatchDTO dto = new TraceBatchDTO();
        dto.setApplyId(applyId);
        dto.setBatchUuid(UUID.randomUUID().toString());

        List<TracePointDTO> points = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusMinutes(5);

        for (int i = 0; i < 5; i++) {
            TracePointDTO pt = new TracePointDTO();
            pt.setLng(113.943000 + i * 0.01);
            pt.setLat(22.540000 + i * 0.005);
            pt.setAlt(110.0 + i);
            pt.setSpeed(12.5);
            pt.setBatteryRemain(98 - i);
            pt.setReportAt(baseTime.plusSeconds(i * 10));
            points.add(pt);
        }
        dto.setPoints(points);

        // 执行批量上报
        int inserted = flyTraceService.batchReport(dto);
        assertThat(inserted).isEqualTo(5);

        // 验证数据库持久化航迹
        List<FlyTrace> traces = flyTraceMapper.selectByApplyId(applyId);
        assertThat(traces).hasSize(5);
        assertThat(traces.get(0).getLng()).isEqualTo(113.943000);
        assertThat(traces.get(4).getLng()).isEqualTo(113.983000);

        // 验证采样时间升序
        assertThat(traces.get(0).getReportAt()).isBefore(traces.get(4).getReportAt());

        // 验证 Redis 最新位置缓存与在飞集合
        String latestJson = stringRedisTemplate.opsForValue().get("trace:latest:" + uavId);
        assertThat(latestJson).isNotNull();
        assertThat(latestJson).contains("113.983");

        Boolean isFlying = stringRedisTemplate.opsForSet().isMember("trace:flying", String.valueOf(uavId));
        assertThat(isFlying).isTrue();
    }

    @Test
    @DisplayName("测试用例2：守卫测试-非执行中任务上报航迹被坚决拦截并抛出400")
    void testBatchReportNonExecutingTaskBlocked() {
        // 构造处于 1-已批准 (但尚未确认起飞，非执行中) 状态的任务
        FlightApply apply = createMockFlightApply(FlightApplyStateEnum.APPROVED.getCode());

        TraceBatchDTO dto = new TraceBatchDTO();
        dto.setApplyId(apply.getApplyId());
        List<TracePointDTO> points = new ArrayList<>();
        TracePointDTO pt = new TracePointDTO();
        pt.setLng(113.943);
        pt.setLat(22.540);
        points.add(pt);
        dto.setPoints(points);

        assertThatThrownBy(() -> flyTraceService.batchReport(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("飞行任务非执行中状态");
    }

    @Test
    @DisplayName("测试用例3：守卫测试-任务不存在上报航迹拦截抛出404")
    void testBatchReportTaskNotFound() {
        TraceBatchDTO dto = new TraceBatchDTO();
        dto.setApplyId(9999999L);
        List<TracePointDTO> points = new ArrayList<>();
        TracePointDTO pt = new TracePointDTO();
        pt.setLng(113.943);
        pt.setLat(22.540);
        points.add(pt);
        dto.setPoints(points);

        assertThatThrownBy(() -> flyTraceService.batchReport(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("飞行申报任务不存在");
    }
}
