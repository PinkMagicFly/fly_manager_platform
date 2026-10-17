package com.fly.platform.airspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fly.platform.airspace.entity.ViolationCase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 飞行违规案件记录持久层 Mapper
 *
 * @author SoarCloud Team
 */
@Mapper
public interface ViolationCaseMapper extends BaseMapper<ViolationCase> {

    /** 根据案件流水编号查询 */
    @Select("SELECT * FROM violation_case WHERE case_sn = #{caseSn} AND is_deleted = 0 LIMIT 1")
    ViolationCase selectBySn(@Param("caseSn") String caseSn);

    /** 条件检索违规案件列表 */
    @Select("<script>" +
            "SELECT * FROM violation_case WHERE is_deleted = 0 " +
            "<if test='violateKind != null'> AND violate_kind = #{violateKind} </if>" +
            "<if test='punishState != null'> AND punish_state = #{punishState} </if>" +
            "ORDER BY case_id DESC" +
            "</script>")
    List<ViolationCase> selectCaseList(@Param("violateKind") Integer violateKind, @Param("punishState") Integer punishState);
}
