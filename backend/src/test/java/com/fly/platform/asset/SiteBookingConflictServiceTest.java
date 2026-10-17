package com.fly.platform.asset;

import com.fly.platform.asset.dto.BookingCreateDTO;
import com.fly.platform.asset.entity.BaseSite;
import com.fly.platform.asset.mapper.BaseSiteMapper;
import com.fly.platform.asset.service.SiteBookingConflictService;
import com.fly.platform.asset.vo.BookingCheckResultVO;
import com.fly.platform.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 单元测试: 场站机位占用时空防重预约冲突检测算法
 *
 * @author SoarCloud Team
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("资产中心: 场站机位时空预约防重冲突检测算法测试")
public class SiteBookingConflictServiceTest {

    @Autowired
    private SiteBookingConflictService bookingService;

    @Autowired
    private BaseSiteMapper baseSiteMapper;

    @Test
    @DisplayName("测试容积为1时重叠预约拦截，容积为2时允许通过")
    public void testBookingConflictUnderDifferentCapacities() {
        // 创建一个测试专用场站，初始容量 capacity = 1
        BaseSite testSite = new BaseSite();
        testSite.setSiteCode("SITE-TEST-CAP1");
        testSite.setSiteName("时空防重测试起降点");
        testSite.setSiteGeo("113.900,22.500");
        testSite.setCapacity(1); // 容积仅为 1
        testSite.setOccupied(0);
        testSite.setSiteState(1);
        testSite.initAudit();
        baseSiteMapper.insert(testSite);
        Long siteId = testSite.getSiteId();

        LocalDateTime baseDate = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime slot1Start = baseDate;                   // 09:00
        LocalDateTime slot1End = baseDate.plusHours(1);         // 10:00

        LocalDateTime slot2Start = baseDate.plusMinutes(30);    // 09:30 (与slot1重叠)
        LocalDateTime slot2End = baseDate.plusMinutes(90);      // 10:30

        // 1. 成功提交第一笔预约 [09:00, 10:00]
        BookingCreateDTO b1 = new BookingCreateDTO();
        b1.setSiteId(siteId);
        b1.setUavId(1L);
        b1.setStartTime(slot1Start);
        b1.setEndTime(slot1End);
        bookingService.createBooking(b1);

        // 2. 模拟容量为 1 时，输入时段存在交集的预约申请 [09:30, 10:30]，断言检测到冲突并拦截
        BookingCheckResultVO check = bookingService.checkConflict(siteId, slot2Start, slot2End);
        assertThat(check.getHasConflict()).isTrue();
        assertThat(check.getConflictCount()).isEqualTo(1);
        assertThat(check.getMessage()).contains("存在时空占用冲突");

        BookingCreateDTO b2 = new BookingCreateDTO();
        b2.setSiteId(siteId);
        b2.setUavId(2L);
        b2.setStartTime(slot2Start);
        b2.setEndTime(slot2End);
        assertThatThrownBy(() -> bookingService.createBooking(b2))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("存在时空占用冲突");

        // 3. 将场站容量扩增为 2，断言并发重叠预约允许通过
        testSite.setCapacity(2);
        testSite.updateAudit();
        baseSiteMapper.updateById(testSite);

        BookingCheckResultVO checkAfterExpand = bookingService.checkConflict(siteId, slot2Start, slot2End);
        assertThat(checkAfterExpand.getHasConflict()).isFalse();
        assertThat(checkAfterExpand.getCapacity()).isEqualTo(2);

        // 此时 b2 顺利预约通过
        bookingService.createBooking(b2);
    }
}
