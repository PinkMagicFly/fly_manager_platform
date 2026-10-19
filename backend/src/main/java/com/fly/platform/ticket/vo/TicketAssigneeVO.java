package com.fly.platform.ticket.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 可选客服专员简要信息视图对象
 * 用于商户总账号分派工单时拉取客服人员列表
 *
 * @author SoarCloud Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketAssigneeVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 客服专员账户ID */
    private Long accountId;

    /** 客服登录名 */
    private String loginName;

    /** 掩码手机号 */
    private String mobile;

    /** 岗位角色中文描述（如：商户客服专员） */
    private String roleLabel;
}
