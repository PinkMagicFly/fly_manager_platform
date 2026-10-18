package com.fly.platform.trace.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 航迹点视图模型 VO
 *
 * @author SoarCloud Team
 */
@Data
public class TracePointVO {

    /** 采样经度 */
    private Double lng;

    /** 采样纬度 */
    private Double lat;

    /** 飞行真高（米） */
    private Double alt;

    /** 飞行地速（米/秒） */
    private Double speed;

    /** 剩余电量百分比 */
    private Integer batteryRemain;

    /** 采样时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime reportAt;
}
