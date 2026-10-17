package com.fly.platform.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fly.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 场站机位与起降预约实体（时空防冲）
 *
 * @author SoarCloud Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("site_booking")
public class SiteBooking extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 预约唯一标识 */
    @TableId(type = IdType.AUTO)
    private Long bookingId;

    /** 目标场站ID */
    private Long siteId;

    /** 预约无人机ID */
    private Long uavId;

    /** 关联飞行申报单ID */
    private Long applyId;

    /** 预约占用起始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    /** 预约占用截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    /** 预约状态：1-已预约 2-已占用 3-已释放 4-已取消 */
    private Integer bookingState;
}
