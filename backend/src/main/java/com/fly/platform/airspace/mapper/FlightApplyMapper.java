package com.fly.platform.airspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fly.platform.airspace.entity.FlightApply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 飞行计划申报与合规审批持久层 Mapper
 *
 * @author SoarCloud Team
 */
@Mapper
public interface FlightApplyMapper extends BaseMapper<FlightApply> {

    /** 根据申报单流水编号查询 */
    @Select("SELECT * FROM flight_apply WHERE apply_sn = #{applySn} AND is_deleted = 0 LIMIT 1")
    FlightApply selectBySn(@Param("applySn") String applySn);

    /** 条件检索飞行申报单列表 */
    @Select("<script>" +
            "SELECT * FROM flight_apply WHERE is_deleted = 0 " +
            "<if test='merchantId != null'> AND merchant_id = #{merchantId} </if>" +
            "<if test='approveState != null'> AND approve_state = #{approveState} </if>" +
            "ORDER BY apply_id DESC" +
            "</script>")
    List<FlightApply> selectApplyList(@Param("merchantId") Long merchantId, @Param("approveState") Integer approveState);
}
