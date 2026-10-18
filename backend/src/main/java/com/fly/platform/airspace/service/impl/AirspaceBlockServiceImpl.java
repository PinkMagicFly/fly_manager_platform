package com.fly.platform.airspace.service.impl;

import com.fly.platform.airspace.domain.AirspaceCollisionEngine;
import com.fly.platform.airspace.dto.AirspaceBlockDTO;
import com.fly.platform.airspace.dto.AirspaceValidateDTO;
import com.fly.platform.airspace.dto.RoutePlanDTO;
import com.fly.platform.airspace.entity.AirspaceBlock;
import com.fly.platform.airspace.enums.AirspaceKindEnum;
import com.fly.platform.airspace.mapper.AirspaceBlockMapper;
import com.fly.platform.airspace.service.AirspaceBlockService;
import com.fly.platform.airspace.vo.AirspaceBlockVO;
import com.fly.platform.airspace.vo.AirspaceValidateResultVO;
import com.fly.platform.airspace.vo.RoutePlanResultVO;
import com.fly.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 低空空域管制网格管理服务实现类
 *
 * @author SoarCloud Team
 */
@Service
@RequiredArgsConstructor
public class AirspaceBlockServiceImpl implements AirspaceBlockService {

    private final AirspaceBlockMapper airspaceBlockMapper;
    private final AirspaceCollisionEngine collisionEngine;

    @Override
    public List<AirspaceBlockVO> getAllBlocks() {
        List<AirspaceBlock> blocks = airspaceBlockMapper.selectAllActiveBlocks();
        List<AirspaceBlockVO> voList = new ArrayList<>(blocks.size());

        for (AirspaceBlock block : blocks) {
            AirspaceBlockVO vo = new AirspaceBlockVO();
            vo.setBlockId(block.getBlockId());
            vo.setBlockName(block.getBlockName());
            vo.setBlockKind(block.getBlockKind());
            vo.setBlockKindDesc(AirspaceKindEnum.getDescByCode(block.getBlockKind()));
            vo.setCoordinates(block.parsePolygonPoints());
            vo.setLimitAlt(block.getLimitAlt());
            vo.setEffectiveAt(block.getEffectiveAt());
            vo.setExpireAt(block.getExpireAt());
            vo.setSource(block.getSource());

            // 视觉着色
            if (AirspaceKindEnum.PERMANENT_FORBIDDEN.getCode().equals(block.getBlockKind())) {
                vo.setColor("#ef4444"); // 红色半透明
                vo.setFillOpacity(0.35);
            } else if (AirspaceKindEnum.DYNAMIC_LIMITED.getCode().equals(block.getBlockKind())) {
                vo.setColor("#f59e0b"); // 橙色半透明
                vo.setFillOpacity(0.30);
            } else {
                vo.setColor("#eab308"); // 黄色条纹半透明
                vo.setFillOpacity(0.35);
            }

            voList.add(vo);
        }
        return voList;
    }

    @Override
    public AirspaceValidateResultVO validateRoute(AirspaceValidateDTO dto) {
        List<AirspaceBlock> allBlocks = airspaceBlockMapper.selectAllActiveBlocks();
        return collisionEngine.validateRoute(
                dto.getRoutePoints(),
                dto.getAltPlanned(),
                dto.getStartAt(),
                dto.getEndAt(),
                allBlocks
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AirspaceBlock saveBlock(AirspaceBlockDTO dto) {
        AirspaceBlock block;
        if (dto.getBlockId() != null) {
            block = airspaceBlockMapper.selectById(dto.getBlockId());
            if (block == null) {
                throw new BusinessException(404, "目标空域区块不存在");
            }
        } else {
            block = new AirspaceBlock();
        }

        block.setBlockName(dto.getBlockName());
        block.setBlockKind(dto.getBlockKind());
        block.setLimitAlt(dto.getLimitAlt());
        block.setEffectiveAt(dto.getEffectiveAt());
        block.setExpireAt(dto.getExpireAt());
        block.setSource(dto.getSource() != null ? dto.getSource() : "空管人工划设");

        // 经纬度多边形序列转 WKT
        if (dto.getCoordinates() != null && dto.getCoordinates().size() >= 3) {
            StringBuilder sb = new StringBuilder("POLYGON((");
            for (int i = 0; i < dto.getCoordinates().size(); i++) {
                List<Double> pt = dto.getCoordinates().get(i);
                sb.append(pt.get(0)).append(" ").append(pt.get(1));
                if (i < dto.getCoordinates().size() - 1) {
                    sb.append(", ");
                }
            }
            // 确保首尾闭合
            List<Double> first = dto.getCoordinates().get(0);
            List<Double> last = dto.getCoordinates().get(dto.getCoordinates().size() - 1);
            if (!first.get(0).equals(last.get(0)) || !first.get(1).equals(last.get(1))) {
                sb.append(", ").append(first.get(0)).append(" ").append(first.get(1));
            }
            sb.append("))");
            block.setGeoPolygon(sb.toString());
        } else if (dto.getGeoPolygon() != null && !dto.getGeoPolygon().isEmpty()) {
            block.setGeoPolygon(dto.getGeoPolygon());
        }

        if (block.getBlockId() != null) {
            airspaceBlockMapper.updateBlock(block);
        } else {
            airspaceBlockMapper.insertBlock(block);
        }
        return block;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBlock(Long blockId) {
        AirspaceBlock block = airspaceBlockMapper.selectById(blockId);
        if (block == null) {
            throw new BusinessException(404, "目标空域区块不存在");
        }
        airspaceBlockMapper.deleteById(blockId);
    }

    @Override
    public RoutePlanResultVO planRoute(RoutePlanDTO dto) {
        List<AirspaceBlock> allBlocks = airspaceBlockMapper.selectAllActiveBlocks();
        List<List<Double>> directRoute = new ArrayList<>(2);
        directRoute.add(List.of(dto.getStartLng(), dto.getStartLat()));
        directRoute.add(List.of(dto.getEndLng(), dto.getEndLat()));

        LocalDateTime startAt = LocalDateTime.now();
        LocalDateTime endAt = startAt.plusHours(2);
        int alt = dto.getAltPlanned() != null ? dto.getAltPlanned() : 100;

        // 1. 初次校验直飞两点航线
        AirspaceValidateResultVO checkResult = collisionEngine.validateRoute(directRoute, alt, startAt, endAt, allBlocks);

        RoutePlanResultVO resultVO = new RoutePlanResultVO();
        if (Boolean.TRUE.equals(checkResult.getPass())) {
            // 直飞航线完全合规放行
            resultVO.setRouteLine(directRoute);
            resultVO.setPass(true);
            resultVO.setMessage("直飞航线合规，无空域冲突");
            resultVO.setViolations(new ArrayList<>());
            return resultVO;
        }

        // 2. 直飞碰撞，判断是否存在避障绕行纠偏建议航线
        List<List<Double>> suggested = checkResult.getSuggestedRoute();
        if (suggested != null && suggested.size() >= 2) {
            // 对避障航线进行闭环复核
            AirspaceValidateResultVO recheck = collisionEngine.validateRoute(suggested, alt, startAt, endAt, allBlocks);
            resultVO.setRouteLine(suggested);
            resultVO.setPass(recheck.getPass());
            if (Boolean.TRUE.equals(recheck.getPass())) {
                resultVO.setMessage(String.format("已自动完成智能避障绕行规划（成功绕开 %s）",
                        checkResult.getConflictBlockName() != null ? checkResult.getConflictBlockName() : "冲突空域"));
            } else {
                List<String> violations = new ArrayList<>();
                violations.add(recheck.getMessage());
                resultVO.setViolations(violations);
                resultVO.setMessage(recheck.getMessage());
            }
            if (checkResult.getSuggestedAlt() != null) {
                resultVO.setSuggestedAlt(checkResult.getSuggestedAlt());
            }
            return resultVO;
        }

        // 3. 无建议折线（如限高超标或临时管制）
        resultVO.setRouteLine(directRoute);
        resultVO.setPass(false);
        List<String> violations = new ArrayList<>();
        violations.add(checkResult.getMessage());
        resultVO.setViolations(violations);
        resultVO.setMessage(checkResult.getMessage());
        resultVO.setSuggestedAlt(checkResult.getSuggestedAlt());
        return resultVO;
    }
}
