package com.fly.platform.asset.vo;

import lombok.Data;

/**
 * 场站一张图聚合展示视图 VO
 *
 * @author SoarCloud Team
 */
@Data
public class SiteMapVO {

    /** 场站唯一标识 */
    private Long siteId;

    /** 场站编码 */
    private String siteCode;

    /** 场站中文名称 */
    private String siteName;

    /** 经度 */
    private Double longitude;

    /** 纬度 */
    private Double latitude;

    /** 停机位与充电柜总容量 */
    private Integer capacity;

    /** 当前已占用机位数 */
    private Integer occupied;

    /** 当前剩余空闲机位 */
    private Integer available;

    /** 场站运行状态：1-正常运营 2-临时停运 3-设备维护 */
    private Integer siteState;

    /** 运行状态描述 */
    private String siteStateDesc;

    /** 智能换电柜备用电池总数 */
    private Integer batteryStock;

    /** 正在充电电池数 */
    private Integer chargingCount;
}
