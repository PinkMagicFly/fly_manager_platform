package com.fly.platform.common.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 统一通用分页数据封装载体
 *
 * @param <T> 数据行记录模型类型
 * @author SoarCloud Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据明细列表 */
    private List<T> list = Collections.emptyList();

    /** 满足条件的总记录数 */
    private Long total = 0L;

    /** 当前页码 */
    private Long page = 1L;

    /** 每页条数 */
    private Long size = 10L;
}
