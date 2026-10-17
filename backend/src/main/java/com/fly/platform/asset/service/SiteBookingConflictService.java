package com.fly.platform.asset.service;

import com.fly.platform.asset.dto.BookingCheckDTO;
import com.fly.platform.asset.dto.BookingCreateDTO;
import com.fly.platform.asset.entity.SiteBooking;
import com.fly.platform.asset.vo.BookingCheckResultVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 场站机位占用时空冲突检测与预约业务服务
 *
 * @author SoarCloud Team
 */
public interface SiteBookingConflictService {

    /** 核心算法：检测指定场站在特定时段是否存在时空占用冲突 */
    BookingCheckResultVO checkConflict(Long siteId, LocalDateTime startTime, LocalDateTime endTime);

    /** DTO 入参便捷冲突检测接口 */
    BookingCheckResultVO checkConflict(BookingCheckDTO dto);

    /** 创建场站起降机位预约（时空防冲守卫） */
    SiteBooking createBooking(BookingCreateDTO dto);

    /** 查询指定场站的近期预约记录 */
    List<SiteBooking> getRecentBookings(Long siteId, int limit);
}
