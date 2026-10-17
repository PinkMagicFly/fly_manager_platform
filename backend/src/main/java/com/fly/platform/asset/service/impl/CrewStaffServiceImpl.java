package com.fly.platform.asset.service.impl;

import com.fly.platform.asset.dto.CrewStaffDTO;
import com.fly.platform.asset.entity.CrewStaff;
import com.fly.platform.asset.mapper.CrewStaffMapper;
import com.fly.platform.asset.service.CrewStaffService;
import com.fly.platform.asset.vo.CrewStaffVO;
import com.fly.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 机组人员资质台账业务实现类
 *
 * @author SoarCloud Team
 */
@Service
@RequiredArgsConstructor
public class CrewStaffServiceImpl implements CrewStaffService {

    private final CrewStaffMapper crewStaffMapper;

    @Override
    public List<CrewStaffVO> listCrewStaff() {
        List<CrewStaff> list = crewStaffMapper.selectAllCrew();
        List<CrewStaffVO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (CrewStaff staff : list) {
            CrewStaffVO vo = new CrewStaffVO();
            BeanUtils.copyProperties(staff, vo);

            vo.setCrewTypeDesc(staff.getCrewType() == 1 ? "商用机长(飞手)" : "外场运维专员");

            if (staff.getStatus() == 1) {
                vo.setStatusDesc("在岗执勤");
            } else if (staff.getStatus() == 2) {
                vo.setStatusDesc("轮休");
            } else {
                vo.setStatusDesc("离职");
            }

            if (staff.getLicenseExpire() != null) {
                long days = ChronoUnit.DAYS.between(today, staff.getLicenseExpire());
                vo.setExpireDaysLeft(days);
                vo.setIsExpired(days < 0);
                vo.setIsExpiringSoon(days >= 0 && days <= 30);
            } else {
                vo.setIsExpired(true);
                vo.setIsExpiringSoon(false);
                vo.setExpireDaysLeft(-1L);
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public CrewStaff getById(Long crewId) {
        return crewStaffMapper.selectById(crewId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrewStaff createCrewStaff(CrewStaffDTO dto) {
        CrewStaff conflict = crewStaffMapper.selectByLicenseNo(dto.getLicenseNo());
        if (conflict != null) {
            throw new BusinessException(400, "民用无人机驾驶员执照编号已存在: " + dto.getLicenseNo());
        }

        CrewStaff staff = new CrewStaff();
        BeanUtils.copyProperties(dto, staff);
        staff.setFlyHours(0);
        staff.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        staff.initAudit();
        crewStaffMapper.insert(staff);
        return staff;
    }
}
