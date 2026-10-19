package com.fly.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fly.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 统一账户实体类
 *
 * @author SoarCloud Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("uam_account")
public class UamAccount extends BaseEntity {

    /** 账户唯一标识 (主键自增) */
    @TableId(type = IdType.AUTO)
    private Long accountId;

    /** 登录用户名 (唯一) */
    private String loginName;

    /** 绑定手机号 (唯一) */
    private String mobile;

    /** 电子邮箱 */
    private String email;

    /** 加盐散列密码密文 (序列化时忽略，防止泄露) */
    @JsonIgnore
    private String pwdDigest;

    /** 账户类别：1-客户 2-商户 3-管理员 4-空管 5-机长 6-运维 */
    private Integer accountKind;

    /** 状态：0-待审核 1-正常 2-冻结 3-注销 */
    private Integer status;

    /** 最后登录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastLoginAt;

    /** 最后登录IP */
    private String lastLoginIp;

    /** 拥有的岗位角色中文名称列表 (非持久化展示字段) */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private java.util.List<String> roleNames;

    /** 拥有的角色代码列表 (非持久化展示字段) */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private java.util.List<String> roleCodes;
}
