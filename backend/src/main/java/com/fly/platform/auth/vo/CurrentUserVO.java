package com.fly.platform.auth.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 当前登录用户信息视图对象
 *
 * @author SoarCloud Team
 */
@Data
@Builder
public class CurrentUserVO {

    /** 账户ID */
    private Long accountId;

    /** 登录用户名 */
    private String loginName;

    /** 掩码脱敏手机号 */
    private String mobile;

    /** 账户类别 */
    private Integer accountKind;

    /** 状态 */
    private Integer status;

    /** 所属角色编码列表 */
    private List<String> roles;

    /** 权限码列表 */
    private List<String> permissions;

    /** 商户企业ID (商户专有) */
    private Long merchantId;

    /** 商户企业名称 (商户专有) */
    private String corpName;
}
