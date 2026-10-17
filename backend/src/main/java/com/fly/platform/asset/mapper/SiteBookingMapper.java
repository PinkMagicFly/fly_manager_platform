package com.fly.platform.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fly.platform.asset.entity.SiteBooking;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 场站机位预约持久层 Mapper
 *
 * @author SoarCloud Team
 */
@Mapper
public interface SiteBookingMapper extends BaseMapper<SiteBooking> {

    /**
     * 查询指定场站在指定时间段内存在时空重叠的有效预约列表
     * 重叠判定条件：(start_time < #{endTime}) AND (end_time > #{startTime})
     * 并且预约状态为已预约(1)或已占用(2)
     */
    @Select("SELECT * FROM site_booking WHERE site_id = #{siteId} " +
            "AND booking_state IN (1, 2) AND is_deleted = 0 " +
            "AND start_time < #{endTime} AND end_time > #{startTime}")
    List<SiteBooking> selectOverlappingBookings(@Param("siteId") Long siteId,
                                                @Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime);

    /** 查询指定场站的近期预约记录 */
    @Select("SELECT * FROM site_booking WHERE site_id = #{siteId} AND is_deleted = 0 ORDER BY start_time DESC LIMIT #{limit}")
    List<SiteBooking> selectRecentBookings(@Param("siteId") Long siteId, @Param("limit") int limit);
}
