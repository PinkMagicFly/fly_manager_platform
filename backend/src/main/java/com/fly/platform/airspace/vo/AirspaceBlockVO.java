package com.fly.platform.airspace.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 空域网格区块展示视图对象
 *
 * @author SoarCloud Team
 */
@Data
public class AirspaceBlockVO {

    /** 空域区块ID */
    private Long blockId;

    /** 区块中文名称 */
    private String blockName;

    /** 管控类别：1-永久禁飞区 2-动态限飞区 3-临时管制区 */
    private Integer blockKind;

    /** 管控类别中文描述 */
    private String blockKindDesc;

    /** 多边形经纬度坐标序列 [[lng, lat], ...] */
    private List<List<Double>> coordinates;

    /** 限飞高度上限（米） */
    private Integer limitAlt;

    /** 管制生效起始时间 */
    private LocalDateTime effectiveAt;

    /** 管制失效截止时间 */
    private LocalDateTime expireAt;

    /** 数据来源 */
    private String source;

    /** 地图渲染描边与填充主色调（十六进制颜色代码） */
    private String color;

    /** 填充不透明度 */
    private Double fillOpacity;
}
