package com.fly.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fly.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色-权限授权实体
 *
 * @author SoarCloud Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("uam_role_grant")
public class UamRoleGrant extends BaseEntity {

    /** 授权主键ID */
    @TableId(type = IdType.AUTO)
    private Long grantId;

    /** 角色ID */
    private Long roleId;

    /** 权限编码 */
    private String permCode;

    /** 权限名称 */
    private String permName;
}
