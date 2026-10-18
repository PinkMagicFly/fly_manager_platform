package com.fly.platform.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fly.platform.trace.entity.FlyTrace;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 飞行轨迹明细数据持久层 Mapper
 *
 * @author SoarCloud Team
 */
@Mapper
public interface FlyTraceMapper extends BaseMapper<FlyTrace> {

    /**
     * 批量持久化航迹点数据
     *
     * @param list 航迹点集合
     * @return 插入条数
     */
    int batchInsert(@Param("list") List<FlyTrace> list);

    /**
     * 根据飞行任务ID查询全部航迹点（按采样时间升序排列）
     *
     * @param applyId 飞行申报任务ID
     * @return 航迹点列表
     */
    List<FlyTrace> selectByApplyId(@Param("applyId") Long applyId);

    /**
     * 查询指定无人机最新的一条航迹记录
     *
     * @param uavId 无人机资产ID
     * @return 最新航迹点
     */
    FlyTrace selectLatestByUavId(@Param("uavId") Long uavId);
}
