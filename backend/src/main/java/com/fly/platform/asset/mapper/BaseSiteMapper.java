package com.fly.platform.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fly.platform.asset.entity.BaseSite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 起降场站持久层 Mapper
 *
 * @author SoarCloud Team
 */
@Mapper
public interface BaseSiteMapper extends BaseMapper<BaseSite> {

    /** 查询所有未删除且运行中的场站 */
    @Select("SELECT * FROM base_site WHERE is_deleted = 0 ORDER BY site_id ASC")
    List<BaseSite> selectAllActiveSites();

    /** 根据编号查询场站 */
    @Select("SELECT * FROM base_site WHERE site_code = #{siteCode} AND is_deleted = 0 LIMIT 1")
    BaseSite selectByCode(@Param("siteCode") String siteCode);
}
