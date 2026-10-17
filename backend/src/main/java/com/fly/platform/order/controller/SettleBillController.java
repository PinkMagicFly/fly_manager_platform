package com.fly.platform.order.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fly.platform.common.api.PageResult;
import com.fly.platform.common.api.Result;
import com.fly.platform.common.audit.Audit;
import com.fly.platform.order.dto.BillQueryDTO;
import com.fly.platform.order.dto.InvoiceApplyDTO;
import com.fly.platform.order.service.SettleBillService;
import com.fly.platform.order.vo.SettleBillVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 货运财务结算与对账单控制器
 *
 * @author SoarCloud Team
 */
@RestController
@RequestMapping("/api/v1/order/bill")
@RequiredArgsConstructor
public class SettleBillController {

    private final SettleBillService settleBillService;

    private Long getOperatorId() {
        return StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : 1L;
    }

    /** 分页检索对账单列表 */
    @GetMapping("/page")
    public Result<PageResult<SettleBillVO>> listBills(BillQueryDTO queryDTO) {
        List<SettleBillVO> list = settleBillService.listBills(queryDTO);
        int total = settleBillService.countBills(queryDTO);
        return Result.ok(new PageResult<>(list, (long) total, (long) queryDTO.getPage(), (long) queryDTO.getSize()));
    }

    /** 查询对账单详情 */
    @GetMapping("/{id}")
    public Result<SettleBillVO> getBillDetail(@PathVariable("id") Long id) {
        return Result.ok(settleBillService.getBillDetail(id));
    }

    /** 商户确认对账 */
    @PostMapping("/{id}/confirm")
    @Audit(module = "财务结算", action = "确认对账")
    public Result<Void> confirmBill(@PathVariable("id") Long id) {
        settleBillService.confirmBill(id, getOperatorId());
        return Result.ok("对账确认成功，单据状态已更新为已对账", null);
    }

    /** 申请开具发票 */
    @PostMapping("/invoice")
    @Audit(module = "财务结算", action = "开具发票")
    public Result<Void> applyInvoice(@Valid @RequestBody InvoiceApplyDTO dto) {
        settleBillService.applyInvoice(dto, getOperatorId());
        return Result.ok("增值税电子发票已提交开具，开票凭证将发送至指定邮箱", null);
    }
}
