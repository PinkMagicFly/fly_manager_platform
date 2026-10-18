package com.fly.platform.trace.service;

import com.fly.platform.trace.dto.TraceBatchDTO;
import com.fly.platform.trace.vo.OrderTraceVO;

/**
 * 飞行轨迹采集与回放业务服务接口
 *
 * @author SoarCloud Team
 */
public interface FlyTraceService {

    /**
     * 批量上报飞行轨迹点数据
     * 守卫：校验任务是否存在且处于执行中（approveState=4），若校验通过则落库并写入 Redis 实时最新点
     *
     * @param dto 批量上报入参
     * @return 实际落库条数
     */
    int batchReport(TraceBatchDTO dto);

    /**
     * 根据订单ID查询空间航线与实际轨迹全景回放数据
     *
     * @param orderId 订单ID
     * @return 订单航线与轨迹回放视图
     */
    OrderTraceVO getOrderTrace(Long orderId);
}
