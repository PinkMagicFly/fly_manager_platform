package com.fly.platform.airspace.service.impl;

import com.fly.platform.airspace.domain.AirspaceCollisionEngine;
import com.fly.platform.airspace.dto.AirspaceBlockDTO;
import com.fly.platform.airspace.dto.AirspaceValidateDTO;
import com.fly.platform.airspace.entity.AirspaceBlock;
import com.fly.platform.airspace.enums.AirspaceKindEnum;
import com.fly.platform.airspace.mapper.AirspaceBlockMapper;
import com.fly.platform.airspace.service.AirspaceBlockService;
import com.fly.platform.airspace.vo.AirspaceBlockVO;
import com.fly.platform.airspace.vo.AirspaceValidateResultVO;
import com.fly.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
