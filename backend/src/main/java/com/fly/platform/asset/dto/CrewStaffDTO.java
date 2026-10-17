package com.fly.platform.asset.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 机组执勤人员登记与修订入参
 *
 * @author SoarCloud Team
 */
@Data
public class CrewStaffDTO {

    /** 关联系统账户ID */
    @NotNull(message = "关联系统账户ID不可为空")
    private Long accountId;

    /** 岗位类别：1-商用机长(飞手) 2-外场运维专员 */
    @NotNull(message = "岗位类别不可为空")
    private Integer crewType;

    /** 真实姓名 */
    @NotBlank(message = "真实姓名不可为空")
    private String realName;

    /** 民用无人机驾驶员执照编号 */
    @NotBlank(message = "执照编号不可为空")
    private String licenseNo;

    /** 执照有效期截止日期 */
    @NotNull(message = "执照有效期截止日期不可为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate licenseExpire;

    /** 执勤状态：1-在岗 2-轮休 3-离职 */
    private Integer status;
}
