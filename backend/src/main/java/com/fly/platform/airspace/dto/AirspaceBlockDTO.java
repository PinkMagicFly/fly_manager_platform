package com.fly.platform.airspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 空域管制区块网格维护传输对象
 *
 * @author SoarCloud Team
 */
@Data
public class AirspaceBlockDTO {

    /** 区块ID（更新时必填） */
    private Long blockId;

    /** 区块中文名称 */
    @NotBlank(message = "空域区块名称不能为空")
    private String blockName;

    /** 管控类别：1-永久禁飞区 2-动态限飞区 3-临时管制区 */
    @NotNull(message = "空域管控类别不能为空")
    private Integer blockKind;

    /** WKT 几何多边形文本 (POLYGON((...))) */
    private String geoPolygon;

    /** 前端坐标序列 [[lng, lat], ...] */
    private List<List<Double>> coordinates;

    /** 限飞高度上限（米） */
    private Integer limitAlt;

    /** 管制生效起始时间 */
    private LocalDateTime effectiveAt;

    /** 管制失效截止时间 */
    private LocalDateTime expireAt;

    /** 数据来源渠道 */
    private String source;
}
