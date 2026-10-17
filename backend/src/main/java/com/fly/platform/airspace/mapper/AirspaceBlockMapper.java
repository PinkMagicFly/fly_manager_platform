package com.fly.platform.airspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fly.platform.airspace.entity.AirspaceBlock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 低空空域管制区块持久层 Mapper
 *
 * @author SoarCloud Team
 */
@Mapper
public interface AirspaceBlockMapper extends BaseMapper<AirspaceBlock> {

    /** 查询所有未逻辑删除的空域管制区块 */
    @Select("SELECT * FROM airspace_block WHERE is_deleted = 0 ORDER BY block_kind ASC, block_id ASC")
    List<AirspaceBlock> selectAllActiveBlocks();

    /** 根据空域管控类别查询区块 */
    @Select("SELECT * FROM airspace_block WHERE block_kind = #{blockKind} AND is_deleted = 0 ORDER BY block_id ASC")
    List<AirspaceBlock> selectByKind(@Param("blockKind") Integer blockKind);
}
