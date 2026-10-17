package com.fly.platform.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fly.platform.asset.entity.CrewStaff;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 机组执勤人员持久层 Mapper
 *
 * @author SoarCloud Team
 */
@Mapper
public interface CrewStaffMapper extends BaseMapper<CrewStaff> {

    /** 根据执照编号查询 */
    @Select("SELECT * FROM crew_staff WHERE license_no = #{licenseNo} AND is_deleted = 0 LIMIT 1")
    CrewStaff selectByLicenseNo(@Param("licenseNo") String licenseNo);

    /** 根据系统账号查询关联机组档案 */
    @Select("SELECT * FROM crew_staff WHERE account_id = #{accountId} AND is_deleted = 0 LIMIT 1")
    CrewStaff selectByAccountId(@Param("accountId") Long accountId);

    /** 查询所有在岗机组人员列表 */
    @Select("SELECT * FROM crew_staff WHERE is_deleted = 0 ORDER BY crew_id ASC")
    List<CrewStaff> selectAllCrew();
}
