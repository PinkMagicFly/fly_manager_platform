package com.fly.platform.common.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 空间几何数据库函数兼容与大地地理计算辅助工具
 * 包含 H2 内存库兼容 MySQL 空间函数、Haversine 球面距离计算与 WKT 航线解析
 *
 * @author SoarCloud Team
 */
public class GeoUtils {

    /** 地球平均半径（公里） */
    public static final double EARTH_RADIUS_KM = 6371.0;

    /** 地球平均半径（米） */
    public static final double EARTH_RADIUS_METERS = 6371000.0;

    /** 模拟 MySQL ST_AsText 函数 */
    public static String stAsText(String geom) {
        return geom;
    }

    /** 模拟 MySQL ST_GeomFromText 函数 */
    public static String stGeomFromText(String wkt) {
        return wkt;
    }

    /**
     * 大地球面距离计算 (Haversine Formula) - 单位：米
     *
     * @param lon1 起点经度
     * @param lat1 起点纬度
     * @param lon2 终点经度
     * @param lat2 终点纬度
     * @return 球面两点直线距离（米）
     */
    public static double haversineMeters(double lon1, double lat1, double lon2, double lat2) {
        return haversineKm(lon1, lat1, lon2, lat2) * 1000.0;
    }

    /**
     * 大地球面距离计算 (Haversine Formula) - 单位：公里 (km)
     *
     * @param lon1 起点经度
     * @param lat1 起点纬度
     * @param lon2 终点经度
     * @param lat2 终点纬度
     * @return 球面两点直线距离（公里）
     */
    public static double haversineKm(double lon1, double lat1, double lon2, double lat2) {
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(radLat1) * Math.cos(radLat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * 将 WKT 格式的 LINESTRING 转换为经纬度点序列 [[lng, lat], ...]
     * 例如："LINESTRING(113.943 22.540, 114.000 22.545, 114.062 22.553)"
     *
     * @param lineString WKT 字符串
     * @return 经纬度点序列，解析失败返回空列表
     */
    public static List<List<Double>> parseWktLineString(String lineString) {
        if (lineString == null || lineString.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            String text = lineString.trim();
            int start = text.indexOf("(");
            int end = text.lastIndexOf(")");
            if (start == -1 || end == -1 || end <= start) {
                return Collections.emptyList();
            }
            String content = text.substring(start + 1, end).trim();
            String[] pointPairs = content.split(",");
            List<List<Double>> points = new ArrayList<>(pointPairs.length);
            for (String pair : pointPairs) {
                String[] xy = pair.trim().split("\\s+");
                if (xy.length >= 2) {
                    double lng = Double.parseDouble(xy[0].trim());
                    double lat = Double.parseDouble(xy[1].trim());
                    List<Double> pt = new ArrayList<>(2);
                    pt.add(lng);
                    pt.add(lat);
                    points.add(pt);
                }
            }
            return points;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * 将经纬度点序列序列化为 WKT 格式 LINESTRING
     *
     * @param points [[lng, lat], ...]
     * @return LINESTRING(lng1 lat1, lng2 lat2, ...)
     */
    public static String toWktLineString(List<List<Double>> points) {
        if (points == null || points.isEmpty()) {
            return "LINESTRING()";
        }
        StringBuilder sb = new StringBuilder("LINESTRING(");
        for (int i = 0; i < points.size(); i++) {
            List<Double> pt = points.get(i);
            sb.append(String.format("%.6f %.6f", pt.get(0), pt.get(1)));
            if (i < points.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}

