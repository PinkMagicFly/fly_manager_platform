package com.fly.platform.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fly.platform.order.entity.OrderStateFlow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单状态变迁流水持久层 Mapper
 *
 * @author SoarCloud Team
 */
@Mapper
public interface OrderStateFlowMapper extends BaseMapper<OrderStateFlow> {

    /** 根据订单ID按时间正序查询全量流转流水 */
    @Select("SELECT flow_id, order_id, from_state, to_state, operator_id, remark, created_at " +
            "FROM order_state_flow WHERE order_id = #{orderId} ORDER BY flow_id ASC")
    List<OrderStateFlow> selectByOrderId(@Param("orderId") Long orderId);
}
