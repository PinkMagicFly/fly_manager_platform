package com.fly.platform.sysparam.service;

import com.fly.platform.sysparam.dto.SysParamUpdateDTO;
import com.fly.platform.sysparam.vo.SysParamVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 平台系统参数配置服务接口
 * 支持本地高速缓存、热刷新及写库强审计留痕
 *
 * @author SoarCloud Team
 */
public interface SysParamService {

    /**
     * 获取指定参数键的文本值
     *
     * @param key 参数键
     * @return 参数文本值，若不存在返回 null
     */
    String get(String key);

    /**
     * 获取数值型参数值
     *
     * @param key          参数键
     * @param defaultValue 缺省默认值
     * @return 实际参数值或默认值
     */
    BigDecimal getDecimal(String key, BigDecimal defaultValue);

    /**
     * 获取整型参数值
     *
     * @param key          参数键
     * @param defaultValue 缺省默认值
     * @return 实际参数值或默认值
     */
    Integer getInt(String key, Integer defaultValue);

    /**
     * 更新指定系统参数（热生效并记录操作审计）
     *
     * @param key   参数键
     * @param value 新参数值
     */
    void put(String key, String value);

    /**
     * 查询全量系统运行参数列表
     *
     * @return 参数列表
     */
    List<SysParamVO> listAll();

    /**
     * 批量更新运行参数
     *
     * @param params 待更新参数列表
     */
    void updateParams(List<SysParamUpdateDTO> params);

    /**
     * 清理全量本地缓存
     */
    void clearCache();
}
