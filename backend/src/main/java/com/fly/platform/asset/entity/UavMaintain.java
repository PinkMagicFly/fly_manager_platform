package com.fly.platform.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fly.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 无人机维保与故障记录实体
 *
 * @author SoarCloud Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("uav_maintain")
public class UavMaintain extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 维保记录唯一标识 */
    @TableId(type = IdType.AUTO)
    private Long maintainId;

    /** 关联无人机ID */
    private Long uavId;

    /** 类型：1-定期巡检保养 2-故障上报维修 */
    private Integer maintainType;

    /** 故障现象描述 */
    private String faultDesc;

    /** 维保处理与更换配件记录 */
    private String repairContent;

    /** 执行维保人员账户ID */
    private Long operatorId;

    /** 维保状态：0-待维保 1-维保中 2-已验收合格 */
    private Integer maintainState;
}
