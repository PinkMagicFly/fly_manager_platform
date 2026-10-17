package com.fly.platform.airspace.service;

import com.fly.platform.airspace.dto.AirspaceBlockDTO;
import com.fly.platform.airspace.dto.AirspaceValidateDTO;
import com.fly.platform.airspace.entity.AirspaceBlock;
import com.fly.platform.airspace.vo.AirspaceBlockVO;
import com.fly.platform.airspace.vo.AirspaceValidateResultVO;

import java.util.List;

/**
 * 低空空域管制网格管理服务接口
 *
 * @author SoarCloud Team
 */
public interface AirspaceBlockService {

    /** 查询全量活跃空域管制区块（含三区地图着色坐标序列） */
    List<AirspaceBlockVO> getAllBlocks();

    /** 航线合规性即时碰撞检测与避障纠偏 */
    AirspaceValidateResultVO validateRoute(AirspaceValidateDTO dto);

    /** 保存或更新空域网格区块 */
    AirspaceBlock saveBlock(AirspaceBlockDTO dto);

    /** 删除空域区块（逻辑删除） */
    void deleteBlock(Long blockId);
}
