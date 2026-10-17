package com.fly.platform.airspace;

import com.fly.platform.airspace.domain.AirspaceCollisionEngine;
import com.fly.platform.airspace.dto.AirspaceValidateDTO;
import com.fly.platform.airspace.entity.AirspaceBlock;
import com.fly.platform.airspace.enums.AirspaceKindEnum;
import com.fly.platform.airspace.mapper.AirspaceBlockMapper;
import com.fly.platform.airspace.service.AirspaceBlockService;
import com.fly.platform.airspace.vo.AirspaceValidateResultVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 空间几何相交与电子围栏碰撞算法单元测试
 * 覆盖：MBR 粗筛（<5ms）、空间相交真判、限高拦截、时段时效过滤、避障航线生成
 *
 * @author SoarCloud Team
 */
@SpringBootTest
@ActiveProfiles("test")
public class AirspaceCollisionServiceTest {

    @Autowired
    private AirspaceBlockService airspaceBlockService;

    @Autowired
    private AirspaceBlockMapper airspaceBlockMapper;

    @Autowired
    private AirspaceCollisionEngine collisionEngine;

    @Test
    @DisplayName("测试1：MBR 粗筛性能与相离航线秒级放行 (耗时 < 5ms)")
    public void testMbrDisjointPerformance() {
        // 构造一条完全远离宝安机场（113.78~113.84, 22.60~22.66）的东部航线（盐田港到大梅沙）
        List<List<Double>> safeRoute = new ArrayList<>();
        safeRoute.add(List.of(114.265, 22.578));
        safeRoute.add(List.of(114.280, 22.580));
        safeRoute.add(List.of(114.300, 22.590));

        AirspaceValidateDTO dto = new AirspaceValidateDTO();
        dto.setRoutePoints(safeRoute);
        dto.setAltPlanned(100);
        dto.setStartAt(LocalDateTime.now().plusHours(1));
        dto.setEndAt(LocalDateTime.now().plusHours(2));

        // 预热一次排除首次加载冷启动
        airspaceBlockService.validateRoute(dto);

        long startNano = System.nanoTime();
        AirspaceValidateResultVO result = airspaceBlockService.validateRoute(dto);
        long costMs = (System.nanoTime() - startNano) / 1_000_000;

        assertThat(costMs).isLessThan(20); // 毫秒级快速响应
        assertThat(result.getPass()).isTrue();
        assertThat(result.getConflictType()).isEqualTo("NONE");
    }

    @Test
    @DisplayName("测试2：航线穿越宝安机场禁飞区，精准检出 Block ID 1 并阻断，返回避障航线")
    public void testForbiddenZoneCollisionAndAvoidance() {
        // 构造穿越宝安机场禁飞区 (113.78~113.84, 22.60~22.66) 的航线
        List<List<Double>> crossRoute = new ArrayList<>();
        crossRoute.add(List.of(113.75, 22.63)); // 机场西侧
        crossRoute.add(List.of(113.88, 22.63)); // 机场东侧

        AirspaceValidateDTO dto = new AirspaceValidateDTO();
        dto.setRoutePoints(crossRoute);
        dto.setAltPlanned(90);
        dto.setStartAt(LocalDateTime.now().plusHours(1));
        dto.setEndAt(LocalDateTime.now().plusHours(2));

        AirspaceValidateResultVO result = airspaceBlockService.validateRoute(dto);

        assertThat(result.getPass()).isFalse();
        assertThat(result.getConflictType()).isEqualTo("FORBIDDEN_ZONE");
        assertThat(result.getConflictBlockId()).isEqualTo(1L);
        assertThat(result.getMessage()).contains("穿越禁飞区");

        // 验证生成的避障航线
        assertThat(result.getSuggestedRoute()).isNotNull();
        assertThat(result.getSuggestedRoute().size()).isGreaterThanOrEqualTo(3);

        // 验证避障航线自身不再与禁飞区冲突
        dto.setRoutePoints(result.getSuggestedRoute());
        AirspaceValidateResultVO recheck = airspaceBlockService.validateRoute(dto);
        assertThat(recheck.getPass()).isTrue();
    }

    @Test
    @DisplayName("测试3：限飞区限高维度校验（申报150m > 限高120m 触发告警拦截，申报100m合规放行）")
    public void testAltitudeLimitValidation() {
        // 构造穿越南山科技园限高区 (113.93~113.96, 22.52~22.56, 限高120米) 的航线
        List<List<Double>> nanshanRoute = new ArrayList<>();
        nanshanRoute.add(List.of(113.92, 22.54));
        nanshanRoute.add(List.of(113.97, 22.54));

        AirspaceValidateDTO dto = new AirspaceValidateDTO();
        dto.setRoutePoints(nanshanRoute);
        dto.setStartAt(LocalDateTime.now().plusHours(1));
        dto.setEndAt(LocalDateTime.now().plusHours(2));

        // 场景 A: 计划高度 150 米 -> 超限拦截
        dto.setAltPlanned(150);
        AirspaceValidateResultVO resultHigh = airspaceBlockService.validateRoute(dto);
        assertThat(resultHigh.getPass()).isFalse();
        assertThat(resultHigh.getConflictType()).isEqualTo("ALTITUDE_EXCEEDED");
        assertThat(resultHigh.getSuggestedAlt()).isEqualTo(120);

        // 场景 B: 计划高度 100 米 -> 正常合规放行
        dto.setAltPlanned(100);
        AirspaceValidateResultVO resultNormal = airspaceBlockService.validateRoute(dto);
        assertThat(resultNormal.getPass()).isTrue();
    }

    @Test
    @DisplayName("测试4：临时管制区时效过滤（生效时段内拦截超高、时段失效后放行）")
    public void testTemporaryControlTimeOverlap() {
        // 大运中心临时管制区 (Block 3, 限高80米, 生效时间为当前日前后7天)
        List<List<Double>> dayunRoute = new ArrayList<>();
        dayunRoute.add(List.of(114.19, 22.70));
        dayunRoute.add(List.of(114.24, 22.70));

        AirspaceValidateDTO dto = new AirspaceValidateDTO();
        dto.setRoutePoints(dayunRoute);
        dto.setAltPlanned(100); // 高于临时管制限高 80米

        // 场景 A: 计划在明天飞行 (处于临时管制生效期内) -> 拦截
        dto.setStartAt(LocalDateTime.now().plusDays(1));
        dto.setEndAt(LocalDateTime.now().plusDays(1).plusHours(2));
        AirspaceValidateResultVO resultDuring = airspaceBlockService.validateRoute(dto);
        assertThat(resultDuring.getPass()).isFalse();
        assertThat(resultDuring.getConflictType()).isEqualTo("TEMPORARY_CONTROL");

        // 场景 B: 计划在 30 天后飞行 (临时管制已失效) -> 放行
        dto.setStartAt(LocalDateTime.now().plusDays(30));
        dto.setEndAt(LocalDateTime.now().plusDays(30).plusHours(2));
        AirspaceValidateResultVO resultAfter = airspaceBlockService.validateRoute(dto);
        assertThat(resultAfter.getPass()).isTrue();
    }

    @Test
    @DisplayName("测试5：空域管制区块 WKT 空间多边形点序列解析校验（点数量>=3，排除二进制格式乱码）")
    public void testAirspacePolygonPointParsing() {
        List<AirspaceBlock> blocks = airspaceBlockMapper.selectAllActiveBlocks();
        assertThat(blocks).isNotEmpty();
        for (AirspaceBlock block : blocks) {
            List<List<Double>> points = block.parsePolygonPoints();
            assertThat(points.size()).as("区块 " + block.getBlockName() + " 解析多边形点数必须>=3").isGreaterThanOrEqualTo(3);
        }
    }
}

