package com.fly.platform.trace.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 航迹上报点单条数据 DTO
 *
 * @author SoarCloud Team
 */
@Data
public class TracePointDTO {

    /** 瞬时采样经度 */
    @NotNull(message = "航迹点经度不能为空")
    private Double lng;

    /** 瞬时采样纬度 */
    @NotNull(message = "航迹点纬度不能为空")
    private Double lat;

    /** 实测飞行高度（米） */
    private Double alt;

    /** 瞬时地速（米/秒） */
    private Double speed;

    /** 剩余电量百分比 */
    private Integer batteryRemain;

    /** 设备采样时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime reportAt;
}
