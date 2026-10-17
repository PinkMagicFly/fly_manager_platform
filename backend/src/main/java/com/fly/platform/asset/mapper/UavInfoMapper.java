package com.fly.platform.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fly.platform.asset.entity.UavInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 无人机资产台账持久层 Mapper
 *
 * @author SoarCloud Team
 */
@Mapper
public interface UavInfoMapper extends BaseMapper<UavInfo> {

    /** 根据设备SN编号查询 */
    @Select("SELECT * FROM uav_info WHERE uav_sn = #{sn} AND is_deleted = 0 LIMIT 1")
    UavInfo selectBySn(@Param("sn") String sn);

    /** 条件检索无人机列表 */
    List<UavInfo> selectUavList(@Param("merchantId") Long merchantId,
                                @Param("model") String model,
                                @Param("state") Integer state);

    /** 驱动五态流转更新 */
    @Update("UPDATE uav_info SET state = #{state}, updated_at = NOW() WHERE uav_id = #{uavId} AND is_deleted = 0")
    int updateUavState(@Param("uavId") Long uavId, @Param("state") Integer state);
}
