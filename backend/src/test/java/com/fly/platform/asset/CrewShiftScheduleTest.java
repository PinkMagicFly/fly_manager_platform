package com.fly.platform.asset;

import com.fly.platform.asset.dto.CrewShiftDTO;
import com.fly.platform.asset.dto.CrewStaffDTO;
import com.fly.platform.asset.entity.CrewStaff;
import com.fly.platform.asset.service.CrewShiftService;
import com.fly.platform.asset.service.CrewStaffService;
import com.fly.platform.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 单元测试: 机组周排班与执照守卫冲突检测
 *
 * @author SoarCloud Team
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("资产中心: 机组排班重叠冲突防冲与资质守卫测试")
public class CrewShiftScheduleTest {

    @Autowired
    private CrewShiftService crewShiftService;

    @Autowired
    private CrewStaffService crewStaffService;

    @Test
    @DisplayName("测试同一机长在重叠时段排班时精准拦截并报警")
    public void testOverlappingShiftConflict() {
        // 使用种子中的机长 张志强 (crewId = 1)
        LocalDate targetDate = LocalDate.now().plusDays(2);
        LocalTime slot1Start = LocalTime.of(9, 0);
        LocalTime slot1End = LocalTime.of(12, 0);

        // 第一次成功排班：09:00 - 12:00
        CrewShiftDTO shift1 = new CrewShiftDTO();
        shift1.setCrewId(1L);
        shift1.setShiftDate(targetDate);
        shift1.setStartTime(slot1Start);
        shift1.setEndTime(slot1End);
        crewShiftService.scheduleShift(shift1);

        // 第二次排班尝试插入重叠时段：11:00 - 14:00
        CrewShiftDTO shift2 = new CrewShiftDTO();
        shift2.setCrewId(1L);
        shift2.setShiftDate(targetDate);
        shift2.setStartTime(LocalTime.of(11, 0));
        shift2.setEndTime(LocalTime.of(14, 0));

        assertThatThrownBy(() -> crewShiftService.scheduleShift(shift2))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已存在执勤班次，不可重复排班");
    }

    @Test
    @DisplayName("测试执照已过期的机组人员不可排入执飞班次")
    public void testExpiredLicenseCannotBeScheduled() {
        // 创建一位执照已过期的飞手
        CrewStaffDTO expiredPilot = new CrewStaffDTO();
        expiredPilot.setAccountId(1L);
        expiredPilot.setCrewType(1);
        expiredPilot.setRealName("王过期飞手");
        expiredPilot.setLicenseNo("CAAC-EXP-20209999");
        expiredPilot.setLicenseExpire(LocalDate.now().minusDays(10)); // 已过期10天
        expiredPilot.setStatus(1);

        CrewStaff created = crewStaffService.createCrewStaff(expiredPilot);
        assertThat(created.getCrewId()).isNotNull();

        // 尝试为该飞手排班
        CrewShiftDTO shift = new CrewShiftDTO();
        shift.setCrewId(created.getCrewId());
        shift.setShiftDate(LocalDate.now().plusDays(1));
        shift.setStartTime(LocalTime.of(8, 0));
        shift.setEndTime(LocalTime.of(12, 0));

        assertThatThrownBy(() -> crewShiftService.scheduleShift(shift))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("执照已过期，不可排班");
    }
}
