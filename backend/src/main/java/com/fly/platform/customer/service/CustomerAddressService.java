package com.fly.platform.customer.service;

import com.fly.platform.customer.dto.CustomerAddressDTO;
import com.fly.platform.customer.vo.CustomerAddressVO;

import java.util.List;

/**
 * 客户常用地址簿业务接口
 *
 * @author SoarCloud Team
 */
public interface CustomerAddressService {

    /**
     * 获取当前客户的所有常用地址列表（默认地址排在最前）
     *
     * @return 地址VO列表
     */
    List<CustomerAddressVO> listMine();

    /**
     * 新增常用地址
     *
     * @param dto 地址表单
     * @return 新建的地址ID
     */
    Long create(CustomerAddressDTO dto);

    /**
     * 更新常用地址
     *
     * @param id  地址ID
     * @param dto 地址表单
     */
    void update(Long id, CustomerAddressDTO dto);

    /**
     * 删除常用地址
     *
     * @param id 地址ID
     */
    void delete(Long id);

    /**
     * 设置某个地址为默认地址
     *
     * @param id 地址ID
     */
    void setDefault(Long id);
}
