package com.fly.platform.airspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 飞行违规案件定责与行政处罚下达传输对象
 *
 * @author SoarCloud Team
 */
@Data
public class ViolationHandleDTO {

    /** 案件ID */
    @NotNull(message = "违规案件ID不能为空")
    private Long caseId;

    /** 行政处罚分级：1-警告留痕 2-企业罚款 3-停飞整顿 4-吊销资质 */
    @NotNull(message = "行政处罚分级不能为空")
    private Integer level;

    /** 行政处罚文书说明与执行要求 */
    @NotBlank(message = "行政处罚文书说明不能为空")
    private String punishDesc;
}
