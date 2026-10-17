package com.fly.platform.asset;

import com.fly.platform.asset.domain.UavStateMachine;
import com.fly.platform.asset.dto.UavCreateUpdateDTO;
import com.fly.platform.asset.entity.UavInfo;
import com.fly.platform.asset.enums.UavStateEnum;
import com.fly.platform.asset.mapper.UavInfoMapper;
import com.fly.platform.asset.service.UavService;
import com.fly.platform.common.exception.BusinessException;
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

/**
 * 单元测试: 无人机五态模型流转状态机守卫
 *
 * @author SoarCloud Team
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("资产中心: 无人机五态流转与状态机跃迁守卫测试")
public class UavStateMachineTest {

    @Autowired
    private UavService uavService;

    @Autowired
    private UavInfoMapper uavInfoMapper;

    @Test
    @DisplayName("测试正向合法跃迁: 闲置 -> 任务中 -> 闲置")
    public void testValidStateTransition() {
        UavCreateUpdateDTO dto = new UavCreateUpdateDTO();
        dto.setUavSn("UAV-TEST-001");
        dto.setModel("顺丰丰舟-90 (测试机)");
        dto.setMerchantId(1L);
        dto.setMaxLoad(new BigDecimal("20.00"));
        dto.setMaxRangeKm(new BigDecimal("60.0"));
        dto.setBatteryPct(95);

        UavInfo created = uavService.createUav(dto);
        assertThat(created.getState()).isEqualTo(UavStateEnum.IDLE.getCode());

        // 跃迁至 1-任务中
        uavService.changeUavState(created.getUavId(), UavStateEnum.ON_TASK.getCode(), "指派调度任务起飞");
        UavInfo step1 = uavInfoMapper.selectById(created.getUavId());
        assertThat(step1.getState()).isEqualTo(UavStateEnum.ON_TASK.getCode());

        // 跃迁恢复为 0-闲置
        uavService.changeUavState(created.getUavId(), UavStateEnum.IDLE.getCode(), "任务执行完毕送达签收");
        UavInfo step2 = uavInfoMapper.selectById(created.getUavId());
        assertThat(step2.getState()).isEqualTo(UavStateEnum.IDLE.getCode());
    }

    @Test
    @DisplayName("测试核心安全拦截: 维保中或故障直接跃迁至任务中抛出业务异常")
    public void testIllegalTransitionFromFaultOrMaintainToOnTask() {
        // 1. 静态状态机守卫校验：维保中(2) -> 任务中(1)
        assertThatThrownBy(() ->
                UavStateMachine.validateTransition(UavStateEnum.MAINTAINING.getCode(), UavStateEnum.ON_TASK.getCode())
        ).isInstanceOf(BusinessException.class)
         .hasMessageContaining("维保中或故障设备不可直接跃迁至任务中");

        // 2. 静态状态机守卫校验：故障(3) -> 任务中(1)
        assertThatThrownBy(() ->
                UavStateMachine.validateTransition(UavStateEnum.FAULT.getCode(), UavStateEnum.ON_TASK.getCode())
        ).isInstanceOf(BusinessException.class)
         .hasMessageContaining("维保中或故障设备不可直接跃迁至任务中");

        // 3. 业务 Service 拦截：将故障状态的无人机直接流转到任务中
        assertThatThrownBy(() ->
                uavService.changeUavState(4L, UavStateEnum.ON_TASK.getCode(), "非法任务跃迁测试")
        ).isInstanceOf(BusinessException.class)
         .hasMessageContaining("不可直接跃迁至任务中");
    }

    @Test
    @DisplayName("测试状态跃迁审计时间 updated_at 自动更新留痕")
    public void testAuditTimestampUpdateOnStateChange() {
        LocalDateTime beforeTime = LocalDateTime.now().minusSeconds(2);
        uavService.changeUavState(1L, UavStateEnum.ON_TASK.getCode(), "审计测试跃迁");

        UavInfo updated = uavInfoMapper.selectById(1L);
        assertThat(updated.getUpdatedAt()).isNotNull();
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(beforeTime);
    }
}

