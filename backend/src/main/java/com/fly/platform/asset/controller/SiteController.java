package com.fly.platform.asset.controller;

import com.fly.platform.asset.dto.BookingCheckDTO;
import com.fly.platform.asset.dto.BookingCreateDTO;
import com.fly.platform.asset.entity.BaseSite;
import com.fly.platform.asset.entity.SiteBooking;
import com.fly.platform.asset.service.BaseSiteService;
import com.fly.platform.asset.service.SiteBookingConflictService;
import com.fly.platform.asset.service.SiteSnapService;
import com.fly.platform.asset.vo.BookingCheckResultVO;
import com.fly.platform.asset.vo.SiteMapVO;
import com.fly.platform.asset.vo.SiteSnapVO;
import com.fly.platform.common.api.Result;
import com.fly.platform.common.audit.Audit;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 起降场站一张图与机位防冲预约控制器
 *
 * @author SoarCloud Team
 */
@RestController
@RequestMapping("/api/v1/site")
@RequiredArgsConstructor
public class SiteController {

    private final BaseSiteService baseSiteService;
    private final SiteBookingConflictService bookingConflictService;
    private final SiteSnapService siteSnapService;

    /**
     * 经纬度就近吸附起降场站（过滤服务半径 ≤ 5000米）
     */
    @GetMapping("/snap")
    public Result<SiteSnapVO> snapSite(@RequestParam("lng") Double lng, @RequestParam("lat") Double lat) {
        return Result.ok(siteSnapService.snapSiteWithDistance(lng, lat));
    }

    @GetMapping("/map")
    public Result<List<SiteMapVO>> getSiteMap() {
        return Result.ok(baseSiteService.getSiteMapList());
    }

    @GetMapping("/list")
    public Result<List<BaseSite>> getSiteList() {
        return Result.ok(baseSiteService.getSiteList());
    }

    @GetMapping("/{id}")
    public Result<BaseSite> getSiteDetail(@PathVariable("id") Long id) {
        return Result.ok(baseSiteService.getSiteDetail(id));
    }

    @PostMapping("/booking/check")
    @Audit(module = "场站调度", action = "检测机位占用时空冲突")
    public Result<BookingCheckResultVO> checkBookingConflict(@Valid @RequestBody BookingCheckDTO dto) {
        BookingCheckResultVO result = bookingConflictService.checkConflict(dto);
        return Result.ok(result.getMessage(), result);
    }

    @PostMapping("/booking")
    @Audit(module = "场站调度", action = "创建起降机位预约")
    public Result<SiteBooking> createBooking(@Valid @RequestBody BookingCreateDTO dto) {
        SiteBooking booking = bookingConflictService.createBooking(dto);
        return Result.ok("起降场站机位时空预约成功", booking);
    }

    @GetMapping("/{id}/bookings")
    public Result<List<SiteBooking>> getRecentBookings(@PathVariable("id") Long id,
                                                      @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(bookingConflictService.getRecentBookings(id, limit));
    }
}
