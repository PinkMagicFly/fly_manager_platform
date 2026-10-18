package com.fly.platform.trace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 飞行轨迹明细数据实体 (fly_trace)
 * 记录无人机在途高频采样的瞬时经纬度、飞行高度、速度与电量状态
 *
 * @author SoarCloud Team
 */
@Data
@TableName("fly_trace")
public class FlyTrace implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 航迹点记录唯一标识 */
    @TableId(type = IdType.AUTO)
    private Long traceId;

    /** 关联飞行申报单ID */
    private Long applyId;

    /** 执飞无人机资产ID */
    private Long uavId;

    /** 瞬时经度 (由空间字段 point_geo 解析) */
    @TableField(exist = false)
    private Double lng;

    /** 瞬时纬度 (由空间字段 point_geo 解析) */
    @TableField(exist = false)
    private Double lat;

    /** 空间几何点 WKT 表达式 (POINT(lng lat)) */
    @TableField(exist = false)
    private String pointWkt;

    /** 实测飞行真高海拔（米） */
    @TableField("altitude")
    private Double alt;

    /** 瞬时水平飞行地速（米/秒） */
    private Double speed;

    /** 实时剩余电量百分比 */
    private Integer batteryRemain;

    /** 设备硬件采样时间戳 */
    @TableField("report_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime reportAt;

    /** 入库持久化时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    /** 获取/构造 WKT 格式点字符串 */
    public String getPointWkt() {
        if (pointWkt != null && !pointWkt.trim().isEmpty()) {
            return pointWkt;
        }
        if (lng != null && lat != null) {
            return String.format(java.util.Locale.US, "POINT(%.6f %.6f)", lng, lat);
        }
        return null;
    }

    /** 从 ST_AsText(point_geo) 的 WKT 字符串解析 lng, lat */
    public void setPointWkt(String pointWkt) {
        this.pointWkt = pointWkt;
        if (pointWkt == null || pointWkt.trim().isEmpty()) return;
        try {
            String text = pointWkt.trim();
            int start = text.indexOf("(");
            int end = text.lastIndexOf(")");
            if (start != -1 && end != -1) {
                text = text.substring(start + 1, end).trim();
                String[] xy = text.split("\\s+");
                if (xy.length >= 2) {
                    this.lng = Double.parseDouble(xy[0]);
                    this.lat = Double.parseDouble(xy[1]);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
