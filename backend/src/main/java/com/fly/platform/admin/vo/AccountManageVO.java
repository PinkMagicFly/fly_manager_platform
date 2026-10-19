package com.fly.platform.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 平台统一账户管理视图对象
 *
 * @author SoarCloud Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountManageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long accountId;

    private String loginName;

    private String mobile;

    private String email;

    private Integer accountKind;

    private String accountKindLabel;

    private Integer status;

    private String statusLabel;

    private List<String> roleCodes;

    private List<String> roleNames;

    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;

    private String lastLoginIp;
}
