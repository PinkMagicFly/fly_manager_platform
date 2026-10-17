package com.fly.platform.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fly.platform.asset.entity.UavMaintain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 无人机维保与故障记录持久层 Mapper
 *
 * @author SoarCloud Team
 */
@Mapper
public interface UavMaintainMapper extends BaseMapper<UavMaintain> {

    /** 查询指定无人机的维保记录列表 */
    @Select("SELECT * FROM uav_maintain WHERE uav_id = #{uavId} AND is_deleted = 0 ORDER BY maintain_id DESC")
    List<UavMaintain> selectByUavId(@Param("uavId") Long uavId);

    /** 统计某台无人机的历史故障发生次数 */
    @Select("SELECT COUNT(*) FROM uav_maintain WHERE uav_id = #{uavId} AND maintain_type = 2 AND is_deleted = 0")
    int countFaultsByUavId(@Param("uavId") Long uavId);
}
