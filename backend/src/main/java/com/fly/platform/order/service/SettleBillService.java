package com.fly.platform.order.service;

import com.fly.platform.order.dto.BillQueryDTO;
import com.fly.platform.order.dto.InvoiceApplyDTO;
import com.fly.platform.order.vo.SettleBillVO;

import java.util.List;

/**
 * 货运财务结算与对账单服务契约
 *
 * @author SoarCloud Team
 */
public interface SettleBillService {

    /** 条件检索结算账单列表 */
    List<SettleBillVO> listBills(BillQueryDTO queryDTO);

    /** 条件统计账单数量 */
    int countBills(BillQueryDTO queryDTO);

    /** 获取账单详情 */
    SettleBillVO getBillDetail(Long billId);

    /** 确认对账操作（状态流转至 2-已对账） */
    void confirmBill(Long billId, Long operatorId);

    /** 申请开具发票（状态流转至 3-已开票，标记发票为已开） */
    void applyInvoice(InvoiceApplyDTO dto, Long operatorId);
}
