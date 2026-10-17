package com.fly.platform.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fly.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 起降场站与智能换电设施台账实体
 *
 * @author SoarCloud Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_site")
public class BaseSite extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 场站唯一标识 */
    @TableId(type = IdType.AUTO)
    private Long siteId;

    /** 场站编码（唯一） */
    private String siteCode;

    /** 场站中文名称 */
    private String siteName;

    /** 场站中心经纬度坐标 (经度,纬度) */
    private String siteGeo;

    /** 停机位与换电机柜总容量 */
    private Integer capacity;

    /** 当前已占用机位数 */
    private Integer occupied;

    /** 场站运行状态：1-正常运营 2-临时停运 3-设备维护 */
    private Integer siteState;

    /** 获取经度 */
    public Double getLongitude() {
        if (siteGeo != null && siteGeo.contains(",")) {
            String[] parts = siteGeo.split(",");
            try {
                return Double.parseDouble(parts[0].trim());
            } catch (Exception ignored) {}
        }
        return 114.0;
    }

    /** 获取纬度 */
    public Double getLatitude() {
        if (siteGeo != null && siteGeo.contains(",")) {
            String[] parts = siteGeo.split(",");
            try {
                return Double.parseDouble(parts[1].trim());
            } catch (Exception ignored) {}
        }
        return 22.5;
    }
}
