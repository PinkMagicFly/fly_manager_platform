package com.fly.platform.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fly.platform.order.entity.CargoOrder;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 货运订单核心持久层 Mapper
 *
 * @author SoarCloud Team
 */
@Mapper
public interface CargoOrderMapper extends BaseMapper<CargoOrder> {

    String COLS = "order_id, order_sn, customer_id, merchant_id, sender_addr, receiver_addr, " +
            "ST_AsText(sender_geo) AS sender_geo, ST_AsText(receiver_geo) AS receiver_geo, " +
            "goods_type, goods_weight, urgent_flag, fee_base, fee_airspace, fee_urgent, fee_discount, fee_total, " +
            "pay_way, pay_state, order_state, uav_id, apply_id, expect_arrive_at, created_at, updated_at, is_deleted ";

    @Select("SELECT " + COLS + " FROM cargo_order WHERE order_id = #{orderId} AND is_deleted = 0 LIMIT 1")
    CargoOrder selectOrderById(@Param("orderId") Long orderId);

    @Select("SELECT " + COLS + " FROM cargo_order WHERE order_sn = #{orderSn} AND is_deleted = 0 LIMIT 1")
    CargoOrder selectOrderBySn(@Param("orderSn") String orderSn);

    @Insert("INSERT INTO cargo_order (order_sn, customer_id, merchant_id, sender_addr, receiver_addr, " +
            "sender_geo, receiver_geo, goods_type, goods_weight, urgent_flag, fee_base, fee_airspace, " +
            "fee_urgent, fee_discount, fee_total, pay_way, pay_state, order_state, uav_id, apply_id, expect_arrive_at) " +
            "VALUES (#{orderSn}, #{customerId}, #{merchantId}, #{senderAddr}, #{receiverAddr}, " +
            "ST_GeomFromText(#{senderGeo}), ST_GeomFromText(#{receiverGeo}), #{goodsType}, #{goodsWeight}, " +
            "#{urgentFlag}, #{feeBase}, #{feeAirspace}, #{feeUrgent}, #{feeDiscount}, #{feeTotal}, " +
            "#{payWay}, #{payState}, #{orderState}, #{uavId}, #{applyId}, #{expectArriveAt})")
    @Options(useGeneratedKeys = true, keyProperty = "orderId")
    int insertOrder(CargoOrder order);

    @Select("<script>" +
            "SELECT " + COLS + " FROM cargo_order WHERE is_deleted = 0 " +
            "<if test='merchantId != null'> AND merchant_id = #{merchantId} </if>" +
            "<if test='customerId != null'> AND customer_id = #{customerId} </if>" +
            "<if test='orderState != null'> AND order_state = #{orderState} </if>" +
            "<if test='orderSn != null and orderSn != \"\"'> AND order_sn LIKE CONCAT('%', #{orderSn}, '%') </if>" +
            "<if test='keyword != null and keyword != \"\"'> " +
            "  AND (sender_addr LIKE CONCAT('%', #{keyword}, '%') OR receiver_addr LIKE CONCAT('%', #{keyword}, '%') OR order_sn LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY order_id DESC LIMIT #{offset}, #{size}" +
            "</script>")
    List<CargoOrder> selectOrderList(@Param("merchantId") Long merchantId,
                                    @Param("customerId") Long customerId,
                                    @Param("orderState") Integer orderState,
                                    @Param("orderSn") String orderSn,
                                    @Param("keyword") String keyword,
                                    @Param("offset") Integer offset,
                                    @Param("size") Integer size);

    @Select("<script>" +
            "SELECT COUNT(1) FROM cargo_order WHERE is_deleted = 0 " +
            "<if test='merchantId != null'> AND merchant_id = #{merchantId} </if>" +
            "<if test='customerId != null'> AND customer_id = #{customerId} </if>" +
            "<if test='orderState != null'> AND order_state = #{orderState} </if>" +
            "<if test='orderSn != null and orderSn != \"\"'> AND order_sn LIKE CONCAT('%', #{orderSn}, '%') </if>" +
            "<if test='keyword != null and keyword != \"\"'> " +
            "  AND (sender_addr LIKE CONCAT('%', #{keyword}, '%') OR receiver_addr LIKE CONCAT('%', #{keyword}, '%') OR order_sn LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "</script>")
    int countOrderList(@Param("merchantId") Long merchantId,
                       @Param("customerId") Long customerId,
                       @Param("orderState") Integer orderState,
                       @Param("orderSn") String orderSn,
                       @Param("keyword") String keyword);

    @Update("UPDATE cargo_order SET order_state = #{orderState}, " +
            "pay_state = CASE WHEN #{payState} IS NOT NULL THEN #{payState} ELSE pay_state END, " +
            "updated_at = NOW() WHERE order_id = #{orderId}")
    int updateOrderState(@Param("orderId") Long orderId,
                         @Param("orderState") Integer orderState,
                         @Param("payState") Integer payState);

    @Update("UPDATE cargo_order SET order_state = #{orderState}, uav_id = #{uavId}, apply_id = #{applyId}, updated_at = NOW() " +
            "WHERE order_id = #{orderId}")
    int updateOrderDispatch(@Param("orderId") Long orderId,
                            @Param("orderState") Integer orderState,
                            @Param("uavId") Long uavId,
                            @Param("applyId") Long applyId);

    @Select("<script>" +
            "SELECT COUNT(1) FROM cargo_order WHERE is_deleted = 0 " +
            "<if test='merchantId != null'> AND merchant_id = #{merchantId} </if>" +
            "<if test='orderState != null'> AND order_state = #{orderState} </if>" +
            "</script>")
    int countByState(@Param("merchantId") Long merchantId, @Param("orderState") Integer orderState);
}
