package com.fly.platform.airspace.domain;

import com.fly.platform.airspace.entity.AirspaceBlock;
import com.fly.platform.airspace.enums.AirspaceKindEnum;
import com.fly.platform.airspace.vo.AirspaceValidateResultVO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 毫秒级空间几何与电子围栏碰撞检测引擎
 * 采用 MBR 外接矩形粗筛 + 射线交叉/线段相交精判 + 空间时效与限高复合校验 + 智能避障纠偏算法
 *
 * @author SoarCloud Team
 */
@Component
public class AirspaceCollisionEngine {

    /** 二维点坐标辅助类 */
    public static class Point2D {
        public final double x; // 经度 (Longitude)
        public final double y; // 纬度 (Latitude)

        public Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public List<Double> toList() {
            List<Double> list = new ArrayList<>(2);
            list.add(this.x);
            list.add(this.y);
            return list;
        }
    }

    /** 最小外接矩形 (MBR) 辅助类 */
    public static class MBR {
        public final double minX;
        public final double minY;
        public final double maxX;
        public final double maxY;

        public MBR(double minX, double minY, double maxX, double maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }

        /** 快速判定两个 MBR 是否相交 (耗时 < 0.001ms) */
        public boolean intersects(MBR other) {
            return !(this.maxX < other.minX || this.minX > other.maxX ||
                     this.maxY < other.minY || this.minY > other.maxY);
        }
    }

    /**
     * 航线电子围栏全面碰撞合规检测
     */
    public AirspaceValidateResultVO validateRoute(List<List<Double>> routePoints,
                                                  Integer altPlanned,
                                                  LocalDateTime startAt,
                                                  LocalDateTime endAt,
                                                  List<AirspaceBlock> allBlocks) {
        AirspaceValidateResultVO result = new AirspaceValidateResultVO();
        if (routePoints == null || routePoints.size() < 2) {
            result.setPass(false);
            result.setConflictType("PARAM_ERROR");
            result.setMessage("规划航线点数不足，至少需要起降2个航路点");
            return result;
        }

        List<Point2D> route = parsePoints(routePoints);
        MBR routeMbr = computeMBR(route);

        for (AirspaceBlock block : allBlocks) {
            List<List<Double>> polygonPoints = block.parsePolygonPoints();
            if (polygonPoints.size() < 3) continue;

            List<Point2D> polygon = parsePoints(polygonPoints);
            MBR blockMbr = computeMBR(polygon);

            // 1. MBR 粗筛：外接矩形相离直接排除 (<0.01ms)
            if (!routeMbr.intersects(blockMbr)) {
                continue;
            }

            // 2. 空间相交精判：点在多边形内 或 线段与边相交
            if (isRouteIntersectsPolygon(route, polygon)) {
                Integer kind = block.getBlockKind();

                // 2.1 永久禁飞区碰撞（坚决阻断）
                if (AirspaceKindEnum.PERMANENT_FORBIDDEN.getCode().equals(kind)) {
                    result.setPass(false);
                    result.setConflictType("FORBIDDEN_ZONE");
                    result.setConflictBlockId(block.getBlockId());
                    result.setConflictBlockName(block.getBlockName());
                    result.setMessage(String.format("航线穿越禁飞区 (Block ID: %d, %s)", block.getBlockId(), block.getBlockName()));
                    result.setSuggestedRoute(generateAvoidanceRoute(route, polygon, blockMbr));
                    return result;
                }

                // 2.2 动态限飞区碰撞（校验巡航高度）
                if (AirspaceKindEnum.DYNAMIC_LIMITED.getCode().equals(kind)) {
                    if (block.getLimitAlt() != null && altPlanned != null && altPlanned > block.getLimitAlt()) {
                        result.setPass(false);
                        result.setConflictType("ALTITUDE_EXCEEDED");
                        result.setConflictBlockId(block.getBlockId());
                        result.setConflictBlockName(block.getBlockName());
                        result.setSuggestedAlt(block.getLimitAlt());
                        result.setMessage(String.format("航线穿越限飞区且高度超标 (限高: %d米, 申报高度: %d米, 区块: %s)",
                                block.getLimitAlt(), altPlanned, block.getBlockName()));
                        return result;
                    }
                }

                // 2.3 临时管制区碰撞（校验时段重叠）
                if (AirspaceKindEnum.TEMPORARY_CONTROL.getCode().equals(kind)) {
                    boolean timeOverlap = isTimeOverlap(startAt, endAt, block.getEffectiveAt(), block.getExpireAt());
                    if (timeOverlap) {
                        if (block.getLimitAlt() != null && altPlanned != null && altPlanned > block.getLimitAlt()) {
                            result.setPass(false);
                            result.setConflictType("TEMPORARY_CONTROL");
                            result.setConflictBlockId(block.getBlockId());
                            result.setConflictBlockName(block.getBlockName());
                            result.setSuggestedAlt(block.getLimitAlt());
                            result.setMessage(String.format("航线与临时管制区时段重叠且超高 (管制时段限高: %d米, 区块: %s)",
                                    block.getLimitAlt(), block.getBlockName()));
                            result.setSuggestedRoute(generateAvoidanceRoute(route, polygon, blockMbr));
                            return result;
                        }
                    }
                }
            }
        }

        // 所有区块校验通过
        result.setPass(true);
        result.setConflictType("NONE");
        result.setMessage("合规校验通过，规划航线无空域冲突，安全净空");
        return result;
    }

    /**
     * 判断时段是否重叠
     */
    public boolean isTimeOverlap(LocalDateTime start1, LocalDateTime end1,
                                 LocalDateTime start2, LocalDateTime end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return true;
        }
        return !end1.isBefore(start2) && !start1.isAfter(end2);
    }

    /**
     * 判断航线折线是否与多边形发生空间相交
     */
    public boolean isRouteIntersectsPolygon(List<Point2D> route, List<Point2D> polygon) {
        for (Point2D pt : route) {
            if (isPointInPolygon(pt, polygon)) {
                return true;
            }
        }

        for (int i = 0; i < route.size() - 1; i++) {
            Point2D p1 = route.get(i);
            Point2D p2 = route.get(i + 1);

            for (int j = 0; j < polygon.size() - 1; j++) {
                Point2D q1 = polygon.get(j);
                Point2D q2 = polygon.get(j + 1);

                if (isSegmentIntersect(p1, p2, q1, q2)) {
                    return true;
                }
            }
            Point2D qLast = polygon.get(polygon.size() - 1);
            Point2D qFirst = polygon.get(0);
            if (isSegmentIntersect(p1, p2, qLast, qFirst)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 射线交叉法判断点是否在多边形内部
     */
    public boolean isPointInPolygon(Point2D pt, List<Point2D> polygon) {
        boolean inside = false;
        int n = polygon.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            Point2D pi = polygon.get(i);
            Point2D pj = polygon.get(j);

            if ((pi.y > pt.y) != (pj.y > pt.y) &&
                (pt.x < (pj.x - pi.x) * (pt.y - pi.y) / (pj.y - pi.y) + pi.x)) {
                inside = !inside;
            }
        }
        return inside;
    }

    /**
     * 线段快速排斥与跨立实验相交检测
     */
    public boolean isSegmentIntersect(Point2D p1, Point2D p2, Point2D q1, Point2D q2) {
        if (Math.max(p1.x, p2.x) < Math.min(q1.x, q2.x) ||
            Math.max(q1.x, q2.x) < Math.min(p1.x, p2.x) ||
            Math.max(p1.y, p2.y) < Math.min(q1.y, q2.y) ||
            Math.max(q1.y, q2.y) < Math.min(p1.y, p2.y)) {
            return false;
        }

        double c1 = crossProduct(p1, p2, q1);
        double c2 = crossProduct(p1, p2, q2);
        double c3 = crossProduct(q1, q2, p1);
        double c4 = crossProduct(q1, q2, p2);

        return (c1 * c2 <= 0) && (c3 * c4 <= 0);
    }

    private double crossProduct(Point2D a, Point2D b, Point2D c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }

    /**
     * 计算点集最小外接矩形 (MBR)
     */
    public MBR computeMBR(List<Point2D> points) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        for (Point2D pt : points) {
            minX = Math.min(minX, pt.x);
            minY = Math.min(minY, pt.y);
            maxX = Math.max(maxX, pt.x);
            maxY = Math.max(maxY, pt.y);
        }
        return new MBR(minX, minY, maxX, maxY);
    }

    /**
     * 智能避障绕行航线规划器
     * 计算避开多边形外接矩形外延缓冲边界的双角点平滑绕行路径
     */
    public List<List<Double>> generateAvoidanceRoute(List<Point2D> originalRoute,
                                                     List<Point2D> forbiddenPolygon,
                                                     MBR blockMbr) {
        List<List<Double>> suggested = new ArrayList<>();
        if (originalRoute.isEmpty()) return suggested;

        Point2D start = originalRoute.get(0);
        Point2D end = originalRoute.get(originalRoute.size() - 1);

        suggested.add(start.toList());

        // 安全外拓缓冲距离（约 1.5 公里）
        double buffer = 0.015;
        double minX = blockMbr.minX - buffer;
        double maxX = blockMbr.maxX + buffer;
        double minY = blockMbr.minY - buffer;
        double maxY = blockMbr.maxY + buffer;

        // 四个角点外拓拐点
        Point2D nw = new Point2D(minX, maxY);
        Point2D ne = new Point2D(maxX, maxY);
        Point2D sw = new Point2D(minX, minY);
        Point2D se = new Point2D(maxX, minY);

        boolean westToEast = start.x <= end.x;
        Point2D n1 = westToEast ? nw : ne;
        Point2D n2 = westToEast ? ne : nw;
        Point2D s1 = westToEast ? sw : se;
        Point2D s2 = westToEast ? se : sw;

        double distNorth = distance(start, n1) + distance(n1, n2) + distance(n2, end);
        double distSouth = distance(start, s1) + distance(s1, s2) + distance(s2, end);

        if (distNorth <= distSouth) {
            suggested.add(n1.toList());
            suggested.add(n2.toList());
        } else {
            suggested.add(s1.toList());
            suggested.add(s2.toList());
        }

        suggested.add(end.toList());
        return suggested;
    }

    private double distance(Point2D a, Point2D b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public List<Point2D> parsePoints(List<List<Double>> rawPoints) {
        List<Point2D> points = new ArrayList<>();
        if (rawPoints == null) return points;
        for (List<Double> pt : rawPoints) {
            if (pt != null && pt.size() >= 2) {
                points.add(new Point2D(pt.get(0), pt.get(1)));
            }
        }
        return points;
    }

}
