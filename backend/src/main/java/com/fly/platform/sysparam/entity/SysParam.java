package com.fly.platform.sysparam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fly.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 平台基础运行参数配置实体
 *
 * @author SoarCloud Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_param")
public class SysParam extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 参数唯一主键ID */
    @TableId(type = IdType.AUTO)
    @TableField("param_id")
    private Long paramId;

    /** 参数分组 (fee, order, alarm) */
    @TableField("param_group")
    private String groupName;

    /** 参数Key（唯一标识） */
    @TableField("param_key")
    private String paramKey;

    /** 参数实际生效值 */
    @TableField("param_value")
    private String paramValue;

    /** 业务参数中文说明 */
    @TableField("param_desc")
    private String remark;
}
