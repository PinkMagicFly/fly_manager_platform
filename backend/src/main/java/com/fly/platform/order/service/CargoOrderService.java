package com.fly.platform.order.service;

import com.fly.platform.order.dto.FeeCalculateDTO;
import com.fly.platform.order.dto.OrderCreateDTO;
import com.fly.platform.order.dto.OrderDispatchDTO;
import com.fly.platform.order.dto.OrderQueryDTO;
import com.fly.platform.order.entity.CargoOrder;
import com.fly.platform.order.vo.*;

import java.util.List;

/**
 * 低空货运订单核心业务服务契约
 *
 * @author SoarCloud Team
 */
public interface CargoOrderService {

    /** 运费前置多维阶梯试算 */
    FeeCalculateVO calculateFee(FeeCalculateDTO dto);

    /** 在线下单创建 */
    CargoOrder createOrder(OrderCreateDTO dto, Long customerId);

    /** 模拟在线支付（状态从 10待支付 -> 20待接单） */
    CargoOrder payOrder(Long orderId, Long operatorId);

    /** 商户接单（状态从 20待接单 -> 30待调度） */
    CargoOrder acceptOrder(Long orderId, Long operatorId);

    /** 商户批量接单 */
    int batchAcceptOrders(List<Long> orderIds, Long operatorId);

    /** 获取智能调度推荐候选及飞手 */
    SmartDispatchResultVO getRecommendDispatch(Long orderId);

    /** 调度确认派单（锁定无人机，自动联动生成飞行申报单，状态流转至 40待起飞） */
    CargoOrder dispatchOrder(OrderDispatchDTO dto, Long operatorId);

    /** 飞手确认起飞（状态流转至 50飞行中，无人机置为任务中） */
    CargoOrder takeoffOrder(Long orderId, Long operatorId);

    /** 送达签收（状态流转至 60已送达，无人机恢复闲置，瞬间自动生成财务对账单） */
    CargoOrder deliverOrder(Long orderId, Long operatorId);

    /** 取消订单（流转至 70已取消） */
    CargoOrder cancelOrder(Long orderId, Long operatorId, String reason);

    /** 异常中断（流转至 80异常中断） */
    CargoOrder abortOrder(Long orderId, Long operatorId, String reason);

    /** 查询订单全景详情（四合一视图） */
    CargoOrderDetailVO getOrderDetail(Long orderId);

    /** 条件检索订单列表 */
    List<CargoOrderVO> listOrders(OrderQueryDTO queryDTO);

    /** 条件统计订单总数 */
    int countOrders(OrderQueryDTO queryDTO);

    /** 获取订单看板九态统计分布 */
    OrderBoardStatsVO getBoardStats(Long merchantId);
}
