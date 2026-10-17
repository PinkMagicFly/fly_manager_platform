package com.fly.platform.common.util;

/**
 * 空间几何数据库函数兼容辅助工具
 * 用于 H2 内存库兼容 MySQL 空间函数 (ST_AsText, ST_GeomFromText)
 *
 * @author SoarCloud Team
 */
public class GeoUtils {

    /** 模拟 MySQL ST_AsText 函数 */
    public static String stAsText(String geom) {
        return geom;
    }

    /** 模拟 MySQL ST_GeomFromText 函数 */
    public static String stGeomFromText(String wkt) {
        return wkt;
    }
}

