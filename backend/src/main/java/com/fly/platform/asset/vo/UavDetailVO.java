package com.fly.platform.asset.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 无人机资产台账展示 VO
 *
 * @author SoarCloud Team
 */
@Data
public class UavDetailVO {

    /** 无人机资产唯一标识 */
    private Long uavId;

    /** 机身设备唯一编号 */
    private String uavSn;

    /** 无人机机型名称 */
    private String model;

    /** 所属物流商户企业ID */
    private Long merchantId;

    /** 所属物流商户企业名称 */
    private String merchantCorpName;

    /** 最大额定载重（千克） */
    private BigDecimal maxLoad;

    /** 最大标称航程（千米） */
    private BigDecimal maxRangeKm;

    /** 当前电池剩余电量百分比 */
    private Integer batteryPct;

    /** 设备健康综合评分 */
    private Integer healthScore;

    /** 累计飞行总时长（小时） */
    private Integer flyHours;

    /** 下次计划维保日期 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime nextMaintainAt;

    /** 设备五态：0-闲置 1-任务中 2-维保中 3-故障 4-离线 */
    private Integer state;

    /** 设备五态描述 */
    private String stateDesc;

    /** 常驻基地场站ID */
    private Long homeSiteId;

    /** 常驻基地场站名称 */
    private String homeSiteName;

    /** 记录创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
