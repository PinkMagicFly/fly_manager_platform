package com.fly.platform.asset.vo;

import lombok.Data;

/**
 * 场站就近吸附与服务范围匹配结果视图对象
 *
 * @author SoarCloud Team
 */
@Data
public class SiteSnapVO {

    /** 场站唯一标识 */
    private Long siteId;

    /** 场站编码 */
    private String siteCode;

    /** 场站中文名称 */
    private String siteName;

    /** 场站中心经度 */
    private Double longitude;

    /** 场站中心纬度 */
    private Double latitude;

    /** 停机位与换电机柜总容量 */
    private Integer capacity;

    /** 当前已占用机位数 */
    private Integer occupied;

    /** 场站运行状态：1-正常运营 2-临时停运 3-设备维护 */
    private Integer siteState;

    /** 场站服务覆盖半径（米） */
    private Double serviceRadius;

    /** 距离查询坐标的实际球面距离（米） */
    private Double distanceMeters;
}
