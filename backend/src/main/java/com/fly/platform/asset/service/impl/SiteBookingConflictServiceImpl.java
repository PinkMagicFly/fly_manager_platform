package com.fly.platform.asset.service.impl;

import com.fly.platform.asset.dto.BookingCheckDTO;
import com.fly.platform.asset.dto.BookingCreateDTO;
import com.fly.platform.asset.entity.BaseSite;
import com.fly.platform.asset.entity.SiteBooking;
import com.fly.platform.asset.enums.BookingStateEnum;
import com.fly.platform.asset.mapper.BaseSiteMapper;
import com.fly.platform.asset.mapper.SiteBookingMapper;
import com.fly.platform.asset.service.SiteBookingConflictService;
import com.fly.platform.asset.vo.BookingCheckResultVO;
import com.fly.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 场站机位占用时空冲突检测与预约业务实现类
 *
 * @author SoarCloud Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SiteBookingConflictServiceImpl implements SiteBookingConflictService {

    private final SiteBookingMapper siteBookingMapper;
    private final BaseSiteMapper baseSiteMapper;

    @Override
    public BookingCheckResultVO checkConflict(Long siteId, LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new BusinessException(400, "预约时间区间无效：截止时间必须晚于起始时间");
        }

        BaseSite site = baseSiteMapper.selectById(siteId);
        if (site == null) {
            throw new BusinessException(404, "目标场站不存在");
        }

        // 查询时空重叠的有效预约记录
        List<SiteBooking> overlapping = siteBookingMapper.selectOverlappingBookings(siteId, startTime, endTime);

        int capacity = site.getCapacity() != null ? site.getCapacity() : 1;
        int conflictCount = overlapping.size();

        BookingCheckResultVO result = new BookingCheckResultVO();
        result.setCapacity(capacity);
        result.setConflictCount(conflictCount);
        result.setConflictingBookings(overlapping);

        if (conflictCount >= capacity) {
            result.setHasConflict(true);
            result.setMessage(String.format("目标场站【%s】在所选时段机位已满(%d/%d)，存在时空占用冲突，不可预约",
                    site.getSiteName(), conflictCount, capacity));
        } else {
            result.setHasConflict(false);
            result.setMessage(String.format("时空校验通过，该时段仍有可用机位(%d/%d)",
                    capacity - conflictCount, capacity));
        }

        return result;
    }

    @Override
    public BookingCheckResultVO checkConflict(BookingCheckDTO dto) {
        return checkConflict(dto.getSiteId(), dto.getStartTime(), dto.getEndTime());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SiteBooking createBooking(BookingCreateDTO dto) {
        // 1. 前置执行时空防重校验
        BookingCheckResultVO check = checkConflict(dto.getSiteId(), dto.getStartTime(), dto.getEndTime());
        if (Boolean.TRUE.equals(check.getHasConflict())) {
            throw new BusinessException(409, check.getMessage());
        }

        // 2. 插入预约记录
        SiteBooking booking = new SiteBooking();
        booking.setSiteId(dto.getSiteId());
        booking.setUavId(dto.getUavId());
        booking.setApplyId(dto.getApplyId());
        booking.setStartTime(dto.getStartTime());
        booking.setEndTime(dto.getEndTime());
        booking.setBookingState(BookingStateEnum.BOOKED.getCode());
        booking.initAudit();
        siteBookingMapper.insert(booking);

        log.info("场站起降机位预约成功: bookingId={}, siteId={}, uavId={}",
                booking.getBookingId(), dto.getSiteId(), dto.getUavId());
        return booking;
    }

    @Override
    public List<SiteBooking> getRecentBookings(Long siteId, int limit) {
        return siteBookingMapper.selectRecentBookings(siteId, limit > 0 ? limit : 10);
    }
}
