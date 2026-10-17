package com.fly.platform.airspace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fly.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 低空空域管控区块地理网格实体
 *
 * @author SoarCloud Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("airspace_block")
public class AirspaceBlock extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 空域区块唯一标识 */
    @TableId(type = IdType.AUTO)
    private Long blockId;

    /** 空域区块中文名称 */
    private String blockName;

    /** 管控类别：1-永久禁飞区 2-动态限飞区 3-临时管制区 */
    private Integer blockKind;

    /** 区块多边形几何空间范围 (WKT格式 POLYGON((lng lat, ...))) */
    private String geoPolygon;

    /** 限飞高度上限（米） */
    private Integer limitAlt;

    /** 管制生效起始时间 */
    private LocalDateTime effectiveAt;

    /** 管制失效截止时间 */
    private LocalDateTime expireAt;

    /** 数据来源渠道 */
    private String source;

    /**
     * 将 WKT 多边形文本解析为经纬度点序列 [[lng, lat], ...]
     */
    public List<List<Double>> parsePolygonPoints() {
        List<List<Double>> points = new ArrayList<>();
        if (geoPolygon == null || geoPolygon.trim().isEmpty()) {
            return points;
        }
        try {
            String text = geoPolygon.trim();
            int start = text.indexOf("((");
            int end = text.lastIndexOf("))");
            if (start != -1 && end != -1) {
                text = text.substring(start + 2, end);
            } else {
                start = text.indexOf("(");
                end = text.lastIndexOf(")");
                if (start != -1 && end != -1) {
                    text = text.substring(start + 1, end);
                }
            }
            String[] coordPairs = text.split(",");
            for (String pair : coordPairs) {
                String clean = pair.trim();
                if (clean.isEmpty()) continue;
                String[] xy = clean.split("\\s+");
                if (xy.length >= 2) {
                    double lng = Double.parseDouble(xy[0]);
                    double lat = Double.parseDouble(xy[1]);
                    List<Double> pt = new ArrayList<>();
                    pt.add(lng);
                    pt.add(lat);
                    points.add(pt);
                }
            }
        } catch (Exception ignored) {
        }
        return points;
    }
}
