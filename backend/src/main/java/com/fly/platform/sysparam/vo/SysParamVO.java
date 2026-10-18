package com.fly.platform.sysparam.vo;

import lombok.Data;

/**
 * 系统运行参数表现层视图对象
 *
 * @author SoarCloud Team
 */
@Data
public class SysParamVO {

    /** 参数主键ID */
    private Long paramId;

    /** 参数分组 */
    private String groupName;

    /** 参数Key */
    private String paramKey;

    /** 参数值 */
    private String paramValue;

    /** 说明备注 */
    private String remark;
}
