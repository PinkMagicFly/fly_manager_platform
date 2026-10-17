package com.fly.platform.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fly.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 机组执勤人员档案实体
 *
 * @author SoarCloud Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crew_staff")
public class CrewStaff extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 机组人员档案唯一标识 */
    @TableId(type = IdType.AUTO)
    private Long crewId;

    /** 关联系统账户ID */
    private Long accountId;

    /** 岗位类别：1-商用机长(飞手) 2-外场运维专员 */
    private Integer crewType;

    /** 真实姓名 */
    private String realName;

    /** 民用无人机驾驶员执照编号 */
    private String licenseNo;

    /** 执照有效期截止日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate licenseExpire;

    /** 累计安全执飞时长（小时） */
    private Integer flyHours;

    /** 执勤状态：1-在岗 2-轮休 3-离职 */
    private Integer status;
}
