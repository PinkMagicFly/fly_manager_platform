package com.fly.platform.asset.service;

import com.fly.platform.asset.dto.CrewStaffDTO;
import com.fly.platform.asset.entity.CrewStaff;
import com.fly.platform.asset.vo.CrewStaffVO;

import java.util.List;

/**
 * 机组人员资质台账服务接口
 *
 * @author SoarCloud Team
 */
public interface CrewStaffService {

    /** 获取所有机组人员档案（包含执照临期预警信息） */
    List<CrewStaffVO> listCrewStaff();

    /** 根据ID获取机组人员详情 */
    CrewStaff getById(Long crewId);

    /** 新增机组人员档案 */
    CrewStaff createCrewStaff(CrewStaffDTO dto);
}
