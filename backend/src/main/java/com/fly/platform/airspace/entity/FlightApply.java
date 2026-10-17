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
 * 低空飞行计划申报与合规审批主表实体
 *
 * @author SoarCloud Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("flight_apply")
public class FlightApply extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 申报唯一标识 */
    @TableId(type = IdType.AUTO)
    private Long applyId;

    /** 申报编号（业务唯一流水号，如 FA202610170001） */
    private String applySn;

    /** 关联履约订单ID（可为空，独立申报时未关联订单） */
    private Long orderId;

    /** 申报商户企业ID */
    private Long merchantId;

    /** 执飞无人机资产ID */
    private Long uavId;

    /** 执飞机长责任人ID */
    private Long pilotId;

    /** 规划飞行航线（空间折线 WKT 格式 LINESTRING(lng lat, ...)） */
    private String routeLine;

    /** 申报飞行巡航高度（米） */
    private Integer altPlanned;

    /** 计划起飞时间 */
    private LocalDateTime startAt;

    /** 计划降落时间 */
    private LocalDateTime endAt;

    /** 系统围栏预校验：0-未校验 1-校验通过 2-冲突告警 */
    private Integer checkState;

    /** 申报审批四态：0-待审批 1-已通过 2-已驳回 3-已撤销 */
    private Integer approveState;

    /** 审批空管人员ID */
    private Long approverId;

    /** 审批批复意见或限高建议 */
    private String approveNote;

    /** 审批决策时间 */
    private LocalDateTime approveAt;

    /**
     * 将 WKT 折线文本解析为经纬度点序列 [[lng, lat], ...]
     */
    public List<List<Double>> parseRoutePoints() {
        List<List<Double>> points = new ArrayList<>();
        if (routeLine == null || routeLine.trim().isEmpty()) {
            return points;
        }
        try {
            String text = routeLine.trim();
            int start = text.indexOf("(");
            int end = text.lastIndexOf(")");
            if (start != -1 && end != -1) {
                text = text.substring(start + 1, end);
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
