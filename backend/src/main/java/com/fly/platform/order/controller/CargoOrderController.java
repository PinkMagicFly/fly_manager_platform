package com.fly.platform.order.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fly.platform.common.api.PageResult;
import com.fly.platform.common.api.Result;
import com.fly.platform.common.audit.Audit;
import com.fly.platform.common.exception.BusinessException;

import com.fly.platform.order.dto.FeeCalculateDTO;
import com.fly.platform.order.dto.OrderCreateDTO;
import com.fly.platform.order.dto.OrderDispatchDTO;
import com.fly.platform.order.dto.OrderQueryDTO;
import com.fly.platform.order.entity.CargoOrder;
import com.fly.platform.order.service.CargoOrderService;
import com.fly.platform.order.vo.*;
import com.fly.platform.trace.service.FlyTraceService;
import com.fly.platform.trace.vo.OrderTraceVO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 货运订单运营与智能调度控制器
 *
 * @author SoarCloud Team
 */
@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
public class CargoOrderController {

    private final FlyTraceService flyTraceService;

    private final CargoOrderService cargoOrderService;

    private Long getOperatorId() {
        return StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : 1L;
    }

    /** 运费前置阶梯试算 */
    @PostMapping("/calculate-fee")
    public Result<FeeCalculateVO> calculateFee(@Valid @RequestBody FeeCalculateDTO dto) {
        return Result.ok(cargoOrderService.calculateFee(dto));
    }

    /** 客户在线下单寄件 */
    @PostMapping("/create")
    @Audit(module = "订单运营", action = "在线下单")
    public Result<CargoOrder> createOrder(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.ok("订单已成功创建，请完成支付", cargoOrderService.createOrder(dto, getOperatorId()));
    }

    /** 模拟在线支付 (10 -> 20) */
    @PostMapping("/{id}/pay")
    @Audit(module = "订单运营", action = "模拟支付")
    public Result<CargoOrder> payOrder(@PathVariable("id") Long id) {
        return Result.ok("支付成功，已进入商户待接单队列", cargoOrderService.payOrder(id, getOperatorId()));
    }

    /** 商户接单 (20 -> 30) */
    @PostMapping("/{id}/accept")
    @Audit(module = "订单运营", action = "确认接单")
    public Result<CargoOrder> acceptOrder(@PathVariable("id") Long id) {
        return Result.ok("接单成功，已进入待调度池", cargoOrderService.acceptOrder(id, getOperatorId()));
    }

    /** 商户批量接单 */
    @PostMapping("/batch-accept")
    @Audit(module = "订单运营", action = "批量接单")
    public Result<Integer> batchAcceptOrders(@RequestBody List<Long> ids) {
        int count = cargoOrderService.batchAcceptOrders(ids, getOperatorId());
        return Result.ok("成功接单 " + count + " 笔运单", count);
    }

    /** 商户拒单 (20 -> 70，原因必填) */
    @PostMapping("/{id}/reject")
    @Audit(module = "订单运营", action = "商户拒单")
    public Result<CargoOrder> rejectOrder(@PathVariable("id") Long id,
                                          @RequestParam(value = "reason") String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException(400, "拒单原因不能为空");
        }
        return Result.ok("已成功拒绝承运该运单", cargoOrderService.cancelOrder(id, getOperatorId(), "商户拒绝接单: " + reason.trim()));
    }


    /** 获取智能调度推荐候选及飞手 */
    @GetMapping("/{id}/recommend-dispatch")
    public Result<SmartDispatchResultVO> getRecommendDispatch(@PathVariable("id") Long id) {
        return Result.ok(cargoOrderService.getRecommendDispatch(id));
    }

    /** 调度派单确认 (30 -> 40，自动联动申报单) */
    @PostMapping("/dispatch")
    @Audit(module = "智能调度", action = "确认派单")
    public Result<CargoOrder> dispatchOrder(@Valid @RequestBody OrderDispatchDTO dto) {
        CargoOrder order = cargoOrderService.dispatchOrder(dto, getOperatorId());
        return Result.ok("智能调度成功，无人机已就绪并自动联动生成飞行计划申报单", order);
    }

    /** 机长确认起飞 (40 -> 50) */
    @PostMapping("/{id}/takeoff")
    @Audit(module = "飞行作业", action = "确认起飞")
    public Result<CargoOrder> takeoffOrder(@PathVariable("id") Long id) {
        return Result.ok("起飞确认成功，无人机正在执行在途配送", cargoOrderService.takeoffOrder(id, getOperatorId()));
    }

    /** 送达签收并自动出账 (50 -> 60) */
    @PostMapping("/{id}/deliver")
    @Audit(module = "飞行作业", action = "送达签收")
    public Result<CargoOrder> deliverOrder(@PathVariable("id") Long id) {
        CargoOrder order = cargoOrderService.deliverOrder(id, getOperatorId());
        return Result.ok("送达签收成功，无人机已恢复闲置，系统已瞬间自动生成财务对账单", order);
    }

    /** 取消订单 (-> 70) */
    @PostMapping("/{id}/cancel")
    @Audit(module = "订单运营", action = "取消订单")
    public Result<CargoOrder> cancelOrder(@PathVariable("id") Long id,
                                          @RequestParam(value = "reason", required = false) String reason) {
        return Result.ok("订单已取消", cargoOrderService.cancelOrder(id, getOperatorId(), reason));
    }

    /** 异常中断 (-> 80) */
    @PostMapping("/{id}/abort")
    @Audit(module = "飞行作业", action = "异常中断")
    public Result<CargoOrder> abortOrder(@PathVariable("id") Long id,
                                         @RequestParam(value = "reason", required = false) String reason) {
        return Result.ok("已记录异常中断状态", cargoOrderService.abortOrder(id, getOperatorId(), reason));
    }

    /** 查询订单全景详情（四合一视图） */
    @GetMapping("/{id}")
    public Result<CargoOrderDetailVO> getOrderDetail(@PathVariable("id") Long id) {
        return Result.ok(cargoOrderService.getOrderDetail(id));
    }


    /** 订单飞行航线与实际航迹全景回放接口 */
    @GetMapping("/{id}/trace")
    public Result<OrderTraceVO> getOrderTrace(@PathVariable("id") Long id) {
        return Result.ok(flyTraceService.getOrderTrace(id));
    }

    /** 分页条件检索订单列表 */
    @GetMapping("/page")
    public Result<PageResult<CargoOrderVO>> listOrders(OrderQueryDTO queryDTO) {
        List<CargoOrderVO> list = cargoOrderService.listOrders(queryDTO);
        int total = cargoOrderService.countOrders(queryDTO);
        return Result.ok(new PageResult<>(list, (long) total, (long) queryDTO.getPage(), (long) queryDTO.getSize()));
    }

    /** 订单看板九态统计分布 */
    @GetMapping("/board-stats")
    public Result<OrderBoardStatsVO> getBoardStats(@RequestParam(value = "merchantId", required = false) Long merchantId) {
        return Result.ok(cargoOrderService.getBoardStats(merchantId));
    }
}
