package com.fly.platform.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fly.platform.customer.entity.CustomerAddress;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户常用地址簿数据访问层
 *
 * @author SoarCloud Team
 */
@Mapper
public interface CustomerAddressMapper extends BaseMapper<CustomerAddress> {
}
