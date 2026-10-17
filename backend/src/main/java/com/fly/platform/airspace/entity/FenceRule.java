package com.fly.platform.airspace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fly.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 电子围栏合规校验规则字典实体
 *
 * @author SoarCloud Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fence_rule")
public class FenceRule extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 规则唯一标识 */
    @TableId(type = IdType.AUTO)
    private Long ruleId;

    /** 规则名称 */
    private String ruleName;

    /** 外围预警安全缓冲带距离（米） */
    private Integer bufferDistance;

    /** 低空默认法定飞行真高上限（米） */
    private Integer maxAltitude;

    /** 碰撞触发动作：1-弹窗告警 2-阻断申报 3-强制降落 */
    private Integer actionPolicy;
}
