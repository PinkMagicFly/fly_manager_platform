package com.fly.platform.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fly.platform.order.entity.SettleBill;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 货运结算对账单据持久层 Mapper
 *
 * @author SoarCloud Team
 */
@Mapper
public interface SettleBillMapper extends BaseMapper<SettleBill> {

    /** 根据账单ID查询详情 */
    @Select("SELECT bill_id, bill_sn, order_id, merchant_id, amount, bill_state, invoice_flag, " +
            "settle_at, created_at, updated_at, is_deleted " +
            "FROM settle_bill WHERE bill_id = #{billId} AND is_deleted = 0 LIMIT 1")
    SettleBill selectBillById(@Param("billId") Long billId);

    /** 根据订单ID查询账单详情 */
    @Select("SELECT bill_id, bill_sn, order_id, merchant_id, amount, bill_state, invoice_flag, " +
            "settle_at, created_at, updated_at, is_deleted " +
            "FROM settle_bill WHERE order_id = #{orderId} AND is_deleted = 0 LIMIT 1")
    SettleBill selectBillByOrderId(@Param("orderId") Long orderId);

    /** 条件检索账单列表 */
    @Select("<script>" +
            "SELECT bill_id, bill_sn, order_id, merchant_id, amount, bill_state, invoice_flag, " +
            "settle_at, created_at, updated_at, is_deleted " +
            "FROM settle_bill WHERE is_deleted = 0 " +
            "<if test='merchantId != null'> AND merchant_id = #{merchantId} </if>" +
            "<if test='billState != null'> AND bill_state = #{billState} </if>" +
            "<if test='billSn != null and billSn != \"\"'> AND bill_sn LIKE CONCAT('%', #{billSn}, '%') </if>" +
            "ORDER BY bill_id DESC " +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    List<SettleBill> selectBillList(@Param("merchantId") Long merchantId,
                                    @Param("billState") Integer billState,
                                    @Param("billSn") String billSn,
                                    @Param("offset") Integer offset,
                                    @Param("size") Integer size);

    /** 条件统计账单数量 */
    @Select("<script>" +
            "SELECT COUNT(1) FROM settle_bill WHERE is_deleted = 0 " +
            "<if test='merchantId != null'> AND merchant_id = #{merchantId} </if>" +
            "<if test='billState != null'> AND bill_state = #{billState} </if>" +
            "<if test='billSn != null and billSn != \"\"'> AND bill_sn LIKE CONCAT('%', #{billSn}, '%') </if>" +
            "</script>")
    int countBillList(@Param("merchantId") Long merchantId,
                      @Param("billState") Integer billState,
                      @Param("billSn") String billSn);

    /** 更新对账状态 */
    @Update("UPDATE settle_bill SET bill_state = #{billState}, updated_at = NOW() WHERE bill_id = #{billId}")
    int updateBillState(@Param("billId") Long billId, @Param("billState") Integer billState);

    /** 更新发票状态 */
    @Update("UPDATE settle_bill SET invoice_flag = #{invoiceFlag}, bill_state = #{billState}, updated_at = NOW() " +
            "WHERE bill_id = #{billId}")
    int updateInvoiceState(@Param("billId") Long billId,
                           @Param("invoiceFlag") Integer invoiceFlag,
                           @Param("billState") Integer billState);
}
