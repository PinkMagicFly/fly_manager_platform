package com.fly.platform.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 全局操作审计日志实体
 *
 * @author SoarCloud Team
 */
@Data
@TableName("audit_log")
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 审计日志唯一ID */
    @TableId(type = IdType.AUTO)
    private Long logId;

    /** 执行操作人账户ID */
    private Long accountId;

    /** 操作人登录账号名称 */
    private String loginName;

    /** 所属业务功能模块 */
    private String moduleName;

    /** 具体操作行为 */
    private String actionName;

    /** 请求HTTP接口路由 */
    private String requestUri;

    /** 客户端发起真实IP */
    private String clientIp;

    /** 发起终端：pc 或 app */
    private String clientKind;

    /** 服务端处理执行耗时（毫秒） */
    private Long costTime;

    /** 请求入参Payload（脱敏后） */
    private String reqParams;

    /** 业务执行响应状态码 */
    private Integer respStatus;

    /** 异常失败捕获堆栈或提示 */
    private String errorMsg;

    /** 操作行为发生时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
