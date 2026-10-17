package com.fly.platform.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fly.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 无人机资产台账实体
 *
 * @author SoarCloud Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("uav_info")
public class UavInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 无人机资产唯一标识 */
    @TableId(type = IdType.AUTO)
    private Long uavId;

    /** 机身设备唯一编号 */
    private String uavSn;

    /** 无人机机型名称 */
    private String model;

    /** 所属物流商户企业ID */
    private Long merchantId;

    /** 最大额定载重（千克） */
    private BigDecimal maxLoad;

    /** 最大标称航程（千米） */
    private BigDecimal maxRangeKm;

    /** 当前电池剩余电量百分比（0-100） */
    private Integer batteryPct;

    /** 设备健康综合评分（0-100） */
    private Integer healthScore;

    /** 累计飞行总时长（小时） */
    private Integer flyHours;

    /** 下次计划维保日期 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime nextMaintainAt;

    /** 设备五态：0-闲置 1-任务中 2-维保中 3-故障 4-离线 */
    private Integer state;

    /** 常驻基地场站ID */
    private Long homeSiteId;
}
