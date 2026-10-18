package com.fly.platform.airspace.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实时在飞无人机态势视图 VO
 *
 * @author SoarCloud Team
 */
@Data
public class FlyingUavVO {

    /** 无人机ID */
    private Long uavId;

    /** 无人机机号/资产编号 */
    private String uavSn;

    /** 无人机型号 */
    private String model;

    /** 实时瞬时经度 */
    private Double lng;

    /** 实时瞬时纬度 */
    private Double lat;

    /** 实时真高海拔（米） */
    private Double alt;

    /** 实时水平地速（米/秒） */
    private Double speed;

    /** 剩余电量百分比 */
    private Integer batteryRemain;

    /** 最新航迹采样时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime reportAt;

    /** 关联履约任务申报ID */
    private Long applyId;

    /** 任务申报流水号 */
    private String applySn;

    /** 执飞机长姓名 */
    private String pilotName;

    /** 任务计划起飞时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startAt;

    /** 任务计划降落时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endAt;

    /** 任务状态描述 */
    private String approveStateDesc;
}
