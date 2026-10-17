package com.fly.platform.airspace;

import com.fly.platform.airspace.dto.AirspaceValidateDTO;
import com.fly.platform.airspace.dto.FlightApplyCreateDTO;
import com.fly.platform.airspace.dto.FlightApproveDTO;
import com.fly.platform.airspace.entity.FlightApply;
import com.fly.platform.airspace.enums.FlightApplyStateEnum;
import com.fly.platform.airspace.service.AirspaceBlockService;
import com.fly.platform.airspace.service.FlightApplyService;
import com.fly.platform.airspace.vo.AirspaceValidateResultVO;
import com.fly.platform.airspace.vo.FlightApplyVO;
import com.fly.platform.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 空域合规与申报审批全链路端到端功能测试链 (End-to-End Function Test Chain)
 * 严守质量门禁：在交付人工走查前，必须通过端到端全链路闭环，消灭所有功能性Bug！
 *
 * @author SoarCloud Team
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AirspaceFeatureChainTest {

    @Autowired
    private AirspaceBlockService airspaceBlockService;

    @Autowired
    private FlightApplyService flightApplyService;

    @Test
    @DisplayName("完整功能测试链：自主申报(无订单) -> 围栏检测 -> 禁区强拦截 -> 合规申报入库 -> 空管审批 -> 确权锁定")
    public void testCompleteAirspaceFunctionChain() {

        // ==================== 链路节点 1：禁飞区违规航线强拦截测试 ====================
        List<List<Double>> forbiddenRoute = new ArrayList<>();
        forbiddenRoute.add(List.of(113.75, 22.63));
        forbiddenRoute.add(List.of(113.88, 22.63));

        AirspaceValidateDTO invalidValidateDTO = new AirspaceValidateDTO();
        invalidValidateDTO.setRoutePoints(forbiddenRoute);
        invalidValidateDTO.setAltPlanned(90);
        invalidValidateDTO.setStartAt(LocalDateTime.now().plusHours(1));
        invalidValidateDTO.setEndAt(LocalDateTime.now().plusHours(2));

        AirspaceValidateResultVO validateFailResult = airspaceBlockService.validateRoute(invalidValidateDTO);
        assertThat(validateFailResult.getPass()).as("穿越宝安禁飞区必须被拦截").isFalse();
        assertThat(validateFailResult.getConflictType()).isEqualTo("FORBIDDEN_ZONE");
        assertThat(validateFailResult.getConflictBlockId()).isEqualTo(1L);

        // 验证生成的智能避障航线自身再次检测必须 100% 净空放行通过
        assertThat(validateFailResult.getSuggestedRoute()).isNotEmpty();
        AirspaceValidateDTO avoidValidateDTO = new AirspaceValidateDTO();
        avoidValidateDTO.setRoutePoints(validateFailResult.getSuggestedRoute());
        avoidValidateDTO.setAltPlanned(90);
        avoidValidateDTO.setStartAt(LocalDateTime.now().plusHours(1));
        avoidValidateDTO.setEndAt(LocalDateTime.now().plusHours(2));
        AirspaceValidateResultVO avoidPassResult = airspaceBlockService.validateRoute(avoidValidateDTO);
        assertThat(avoidPassResult.getPass()).as("采纳避障航线后必须 100% 校验合格放行").isTrue();

        FlightApplyCreateDTO forbiddenCreateDTO = new FlightApplyCreateDTO();
        forbiddenCreateDTO.setOrderId(null); // 独立申报无订单
        forbiddenCreateDTO.setMerchantId(1L);
        forbiddenCreateDTO.setUavId(1L);
        forbiddenCreateDTO.setPilotId(1L);
        forbiddenCreateDTO.setAltPlanned(90);
        forbiddenCreateDTO.setStartAt(LocalDateTime.now().plusHours(1));
        forbiddenCreateDTO.setEndAt(LocalDateTime.now().plusHours(2));
        forbiddenCreateDTO.setRoutePoints(forbiddenRoute);

        BusinessException blockEx = assertThrows(BusinessException.class, () ->
                flightApplyService.createApply(forbiddenCreateDTO, 1L)
        );
        assertThat(blockEx.getMessage()).contains("违规穿越法定禁飞区");

        // ==================== 链路节点 2：合规自主申报成功入库测试 (orderId=null 兼容) ====================
        List<List<Double>> safeRoute = new ArrayList<>();
        safeRoute.add(List.of(113.943, 22.540));
        safeRoute.add(List.of(114.000, 22.545));
        safeRoute.add(List.of(114.062, 22.553));

        AirspaceValidateDTO safeValidateDTO = new AirspaceValidateDTO();
        safeValidateDTO.setRoutePoints(safeRoute);
        safeValidateDTO.setAltPlanned(100);
        safeValidateDTO.setStartAt(LocalDateTime.now().plusHours(2));
        safeValidateDTO.setEndAt(LocalDateTime.now().plusHours(3));

        AirspaceValidateResultVO validatePassResult = airspaceBlockService.validateRoute(safeValidateDTO);
        assertThat(validatePassResult.getPass()).as("合规航线校验必须通过").isTrue();

        FlightApplyCreateDTO safeCreateDTO = new FlightApplyCreateDTO();
        safeCreateDTO.setOrderId(null); // 独立申报：无订单，测试数据库 order_id default 0 兼容性！
        safeCreateDTO.setMerchantId(1L);
        safeCreateDTO.setUavId(1L);
        safeCreateDTO.setPilotId(1L);
        safeCreateDTO.setAltPlanned(100);
        safeCreateDTO.setStartAt(LocalDateTime.now().plusHours(2));
        safeCreateDTO.setEndAt(LocalDateTime.now().plusHours(3));
        safeCreateDTO.setRoutePoints(safeRoute);

        FlightApply createdApply = flightApplyService.createApply(safeCreateDTO, 1L);
        assertThat(createdApply).isNotNull();
        assertThat(createdApply.getApplyId()).isNotNull();
        assertThat(createdApply.getApplySn()).startsWith("FA");
        assertThat(createdApply.getOrderId()).isEqualTo(0L); // 验证自动兜底为0
        assertThat(createdApply.getApproveState()).isEqualTo(FlightApplyStateEnum.PENDING.getCode());
        assertThat(createdApply.getCheckState()).isEqualTo(1);

        // ==================== 链路节点 3：单据详情检索与关联数据组装测试 ====================
        FlightApplyVO detailVO = flightApplyService.getApplyDetail(createdApply.getApplyId());
        assertThat(detailVO).isNotNull();
        assertThat(detailVO.getApplySn()).isEqualTo(createdApply.getApplySn());
        assertThat(detailVO.getRoutePoints().size()).isEqualTo(3);
        assertThat(detailVO.getMerchantName()).isNotBlank();
        assertThat(detailVO.getUavSn()).isNotBlank();
        assertThat(detailVO.getPilotName()).isNotBlank();

        // ==================== 链路节点 4：空管在线审批流转与确权锁定测试 ====================
        FlightApproveDTO approveDTO = new FlightApproveDTO();
        approveDTO.setApplyId(createdApply.getApplyId());
        approveDTO.setApproveState(FlightApplyStateEnum.APPROVED.getCode());
        approveDTO.setApproveNote("经空管时空推演核验无冲突，准予执飞");
        approveDTO.setSuggestedAlt(95);

        flightApplyService.approveApply(approveDTO, 3L);

        // 重新查询详情，验证状态更新与时段确权
        FlightApplyVO approvedVO = flightApplyService.getApplyDetail(createdApply.getApplyId());
        assertThat(approvedVO.getApproveState()).isEqualTo(FlightApplyStateEnum.APPROVED.getCode());
        assertThat(approvedVO.getApproverId()).isEqualTo(3L);
        assertThat(approvedVO.getApproveNote()).contains("核验无冲突");
        assertThat(approvedVO.getAltPlanned()).isEqualTo(95);

        // ==================== 链路节点 5：确权单据逆向流转拦截测试 ====================
        BusinessException cancelEx = assertThrows(BusinessException.class, () ->
                flightApplyService.cancelApply(createdApply.getApplyId(), 1L)
        );
        assertThat(cancelEx.getMessage()).contains("已通过审批的飞行计划单据已锁定空域时段，不允许直接撤回");

    }
}
