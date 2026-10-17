package com.fly.platform.order.service.impl;

import com.fly.platform.auth.entity.MerchantArchive;
import com.fly.platform.auth.mapper.MerchantArchiveMapper;
import com.fly.platform.common.exception.BusinessException;
import com.fly.platform.order.dto.BillQueryDTO;
import com.fly.platform.order.dto.InvoiceApplyDTO;
import com.fly.platform.order.entity.CargoOrder;
import com.fly.platform.order.entity.SettleBill;
import com.fly.platform.order.enums.BillStateEnum;
import com.fly.platform.order.mapper.CargoOrderMapper;
import com.fly.platform.order.mapper.SettleBillMapper;
import com.fly.platform.order.service.SettleBillService;
import com.fly.platform.order.vo.SettleBillVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 货运财务结算对账单服务实现类
 *
 * @author SoarCloud Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettleBillServiceImpl implements SettleBillService {

    private final SettleBillMapper settleBillMapper;
    private final CargoOrderMapper cargoOrderMapper;
    private final MerchantArchiveMapper merchantArchiveMapper;

    @Override
    public List<SettleBillVO> listBills(BillQueryDTO queryDTO) {
        int page = queryDTO.getPage() != null && queryDTO.getPage() > 0 ? queryDTO.getPage() : 1;
        int size = queryDTO.getSize() != null && queryDTO.getSize() > 0 ? queryDTO.getSize() : 10;
        int offset = (page - 1) * size;

        List<SettleBill> list = settleBillMapper.selectBillList(
                queryDTO.getMerchantId(), queryDTO.getBillState(),
                queryDTO.getBillSn(), offset, size);

        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public int countBills(BillQueryDTO queryDTO) {
        return settleBillMapper.countBillList(
                queryDTO.getMerchantId(), queryDTO.getBillState(), queryDTO.getBillSn());
    }

    @Override
    public SettleBillVO getBillDetail(Long billId) {
        SettleBill bill = settleBillMapper.selectBillById(billId);
        if (bill == null) throw new BusinessException(404, "账单不存在");
        return convertToVO(bill);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmBill(Long billId, Long operatorId) {
        SettleBill bill = settleBillMapper.selectBillById(billId);
        if (bill == null) throw new BusinessException(404, "账单不存在");

        if (bill.getBillState() != null && bill.getBillState() >= BillStateEnum.RECONCILED.getCode()) {
            throw new BusinessException(400, "该账单已完成对账，无需重复操作");
        }

        settleBillMapper.updateBillState(billId, BillStateEnum.RECONCILED.getCode());
        log.info("账单确认对账成功: billId={}, billSn={}, 操作人={}", billId, bill.getBillSn(), operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyInvoice(InvoiceApplyDTO dto, Long operatorId) {
        SettleBill bill = settleBillMapper.selectBillById(dto.getBillId());
        if (bill == null) throw new BusinessException(404, "账单不存在");

        if (bill.getInvoiceFlag() != null && bill.getInvoiceFlag() == 1) {
            throw new BusinessException(400, "该对账单已开具增值税电子发票，不可重复申请");
        }

        settleBillMapper.updateInvoiceState(dto.getBillId(), 1, BillStateEnum.INVOICED.getCode());
        log.info("申请开具发票成功: billId={}, 抬头={}, 税号={}, 操作人={}",
                dto.getBillId(), dto.getInvoiceTitle(), dto.getTaxNumber(), operatorId);
    }

    private SettleBillVO convertToVO(SettleBill bill) {
        SettleBillVO vo = new SettleBillVO();
        vo.setBillId(bill.getBillId());
        vo.setBillSn(bill.getBillSn());
        vo.setOrderId(bill.getOrderId());
        vo.setMerchantId(bill.getMerchantId());
        vo.setAmount(bill.getAmount());
        vo.setBillState(bill.getBillState());
        vo.setBillStateDesc(BillStateEnum.getDescByCode(bill.getBillState()));
        vo.setInvoiceFlag(bill.getInvoiceFlag());
        vo.setSettleAt(bill.getSettleAt());
        vo.setCreatedAt(bill.getCreatedAt());

        if (bill.getOrderId() != null) {
            CargoOrder order = cargoOrderMapper.selectOrderById(bill.getOrderId());
            if (order != null) vo.setOrderSn(order.getOrderSn());
        }
        if (bill.getMerchantId() != null) {
            MerchantArchive m = merchantArchiveMapper.selectById(bill.getMerchantId());
            if (m != null) vo.setMerchantName(m.getCorpName());
        }
        return vo;
    }
}
