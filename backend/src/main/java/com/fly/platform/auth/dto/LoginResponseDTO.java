package com.fly.platform.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 登录认证成功返回响应 DTO
 *
 * @author SoarCloud Team
 */
@Data
@Builder
public class LoginResponseDTO {

    /** 会话鉴权 Token 串 */
    private String token;

    /** 账户主键ID */
    private Long accountId;

    /** 登录用户名 */
    private String loginName;

    /** 掩码脱敏手机号 (如 138****0000) */
    private String mobile;

    /** 账户类别：1-客户 2-商户 3-管理员 4-空管 5-机长 6-运维 */
    private Integer accountKind;

    /** 关联的角色编码列表 (如 [ROLE_ADMIN]) */
    private List<String> roles;

    /** 拥有的按钮与接口权限码列表 */
    private List<String> permissions;

    /** 关联的商户企业档案ID (仅商户角色返回) */
    private Long merchantId;

    /** 商户企业全称 (仅商户角色返回) */
    private String corpName;
}
