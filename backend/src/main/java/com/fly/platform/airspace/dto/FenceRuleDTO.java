package com.fly.platform.airspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 电子围栏合规校验规则更新传输对象
 *
 * @author SoarCloud Team
 */
@Data
public class FenceRuleDTO {

    /** 规则ID */
    @NotNull(message = "规则ID不能为空")
    private Long ruleId;

    /** 规则名称 */
    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    /** 外围预警安全缓冲带距离（米） */
    @NotNull(message = "安全缓冲带距离不能为空")
    private Integer bufferDistance;

    /** 低空默认法定飞行真高上限（米） */
    @NotNull(message = "法定飞行真高上限不能为空")
    private Integer maxAltitude;

    /** 碰撞触发动作：1-弹窗告警 2-阻断申报 3-强制降落 */
    @NotNull(message = "碰撞触发动作不能为空")
    private Integer actionPolicy;
}
