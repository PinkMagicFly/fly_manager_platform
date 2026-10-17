package com.fly.platform.asset.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

/**
 * 机组执勤人员资质与档案展示 VO
 *
 * @author SoarCloud Team
 */
@Data
public class CrewStaffVO {

    /** 机组人员档案唯一标识 */
    private Long crewId;

    /** 关联系统账户ID */
    private Long accountId;

    /** 岗位类别：1-商用机长(飞手) 2-外场运维专员 */
    private Integer crewType;

    /** 岗位描述 */
    private String crewTypeDesc;

    /** 真实姓名 */
    private String realName;

    /** 民用无人机驾驶员执照编号 */
    private String licenseNo;

    /** 执照有效期截止日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate licenseExpire;

    /** 距执照到期剩余天数 */
    private Long expireDaysLeft;

    /** 是否即将到期（<=30天） */
    private Boolean isExpiringSoon;

    /** 执照是否已过期 */
    private Boolean isExpired;

    /** 累计安全执飞时长（小时） */
    private Integer flyHours;

    /** 执勤状态：1-在岗 2-轮休 3-离职 */
    private Integer status;

    /** 执勤状态描述 */
    private String statusDesc;
}
