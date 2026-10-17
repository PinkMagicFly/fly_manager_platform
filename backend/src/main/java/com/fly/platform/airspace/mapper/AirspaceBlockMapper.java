package com.fly.platform.airspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fly.platform.airspace.entity.AirspaceBlock;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 低空空域管制区块持久层 Mapper
 * 显式采用 ST_AsText 与 ST_GeomFromText 消除 MySQL 空间内部二进制格式乱码
 *
 * @author SoarCloud Team
 */
@Mapper
public interface AirspaceBlockMapper extends BaseMapper<AirspaceBlock> {

    /** 查询所有未逻辑删除的空域管制区块（通过 ST_AsText 转换为 WKT 字符串） */
    @Select("SELECT block_id, block_name, block_kind, ST_AsText(geo_polygon) AS geo_polygon, limit_alt, " +
            "effective_at, expire_at, source, created_at, updated_at, is_deleted " +
            "FROM airspace_block WHERE is_deleted = 0 ORDER BY block_kind ASC, block_id ASC")
    List<AirspaceBlock> selectAllActiveBlocks();

    /** 根据空域管控类别查询区块 */
    @Select("SELECT block_id, block_name, block_kind, ST_AsText(geo_polygon) AS geo_polygon, limit_alt, " +
            "effective_at, expire_at, source, created_at, updated_at, is_deleted " +
            "FROM airspace_block WHERE block_kind = #{blockKind} AND is_deleted = 0 ORDER BY block_id ASC")
    List<AirspaceBlock> selectByKind(@Param("blockKind") Integer blockKind);

    /** 插入新空域区块（空间字段采用 ST_GeomFromText 安全入库） */
    @Insert("INSERT INTO airspace_block (block_name, block_kind, geo_polygon, limit_alt, effective_at, expire_at, source, is_deleted) " +
            "VALUES (#{blockName}, #{blockKind}, ST_GeomFromText(#{geoPolygon}), #{limitAlt}, #{effectiveAt}, #{expireAt}, #{source}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "blockId")
    int insertBlock(AirspaceBlock block);

    /** 更新空域区块 */
    @Update("UPDATE airspace_block SET block_name = #{blockName}, block_kind = #{blockKind}, " +
            "geo_polygon = ST_GeomFromText(#{geoPolygon}), limit_alt = #{limitAlt}, " +
            "effective_at = #{effectiveAt}, expire_at = #{expireAt}, source = #{source} " +
            "WHERE block_id = #{blockId}")
    int updateBlock(AirspaceBlock block);
}

