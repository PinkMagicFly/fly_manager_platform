package com.fly.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fly.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * RBAC 角色实体类
 *
 * @author SoarCloud Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("uam_role")
public class UamRole extends BaseEntity {

    /** 角色ID (主键自增) */
    @TableId(type = IdType.AUTO)
    private Long roleId;

    /** 角色编码 (如 ROLE_ADMIN) */
    private String roleCode;

    /** 角色中文名称 */
    private String roleName;

    /** 角色描述 */
    private String description;
}
