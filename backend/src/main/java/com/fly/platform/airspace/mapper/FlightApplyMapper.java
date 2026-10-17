package com.fly.platform.airspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fly.platform.airspace.entity.FlightApply;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 飞行计划申报与合规审批持久层 Mapper
 * 显式采用 ST_AsText 与 ST_GeomFromText 消除 MySQL 空间内部二进制格式乱码
 *
 * @author SoarCloud Team
 */
@Mapper
public interface FlightApplyMapper extends BaseMapper<FlightApply> {

    /** 根据申报单流水编号查询 */
    @Select("SELECT apply_id, apply_sn, order_id, merchant_id, uav_id, pilot_id, " +
            "ST_AsText(route_line) AS route_line, alt_planned, start_at, end_at, check_state, " +
            "approve_state, approver_id, approve_note, approve_at, created_at, updated_at, is_deleted " +
            "FROM flight_apply WHERE apply_sn = #{applySn} AND is_deleted = 0 LIMIT 1")
    FlightApply selectBySn(@Param("applySn") String applySn);

    /** 根据主键ID查询申报单详情 */
    @Select("SELECT apply_id, apply_sn, order_id, merchant_id, uav_id, pilot_id, " +
            "ST_AsText(route_line) AS route_line, alt_planned, start_at, end_at, check_state, " +
            "approve_state, approver_id, approve_note, approve_at, created_at, updated_at, is_deleted " +
            "FROM flight_apply WHERE apply_id = #{applyId} AND is_deleted = 0 LIMIT 1")
    FlightApply selectApplyById(@Param("applyId") Long applyId);

    /** 条件检索飞行申报单列表 */
    @Select("<script>" +
            "SELECT apply_id, apply_sn, order_id, merchant_id, uav_id, pilot_id, " +
            "ST_AsText(route_line) AS route_line, alt_planned, start_at, end_at, check_state, " +
            "approve_state, approver_id, approve_note, approve_at, created_at, updated_at, is_deleted " +
            "FROM flight_apply WHERE is_deleted = 0 " +
            "<if test='merchantId != null'> AND merchant_id = #{merchantId} </if>" +
            "<if test='approveState != null'> AND approve_state = #{approveState} </if>" +
            "ORDER BY apply_id DESC" +
            "</script>")
    List<FlightApply> selectApplyList(@Param("merchantId") Long merchantId, @Param("approveState") Integer approveState);

    /** 插入新申报单（航线空间折线字段采用 ST_GeomFromText 安全入库） */
    @Insert("INSERT INTO flight_apply (apply_sn, order_id, merchant_id, uav_id, pilot_id, " +
            "route_line, alt_planned, start_at, end_at, check_state, approve_state) " +
            "VALUES (#{applySn}, #{orderId}, #{merchantId}, #{uavId}, #{pilotId}, " +
            "ST_GeomFromText(#{routeLine}), #{altPlanned}, #{startAt}, #{endAt}, #{checkState}, #{approveState})")
    @Options(useGeneratedKeys = true, keyProperty = "applyId")
    int insertApply(FlightApply apply);
}

