package com.fly.platform.order.vo;

import com.fly.platform.airspace.vo.FlightApplyVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 订单全景详情视图对象（四合一一屏尽览）
 *
 * @author SoarCloud Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CargoOrderDetailVO extends CargoOrderVO {

    /** 订单全生命周期状态流转审计记录列表 */
    private List<OrderStateFlowVO> flowList;

    /** 关联结算财务账单详情 */
    private SettleBillVO settleBill;

    /** 关联飞行申报计划详情 */
    private FlightApplyVO flightApplyDetail;

    /** 航线地理折线坐标序列 [[lng, lat], ...] */
    private List<List<Double>> routePoints;
}
