package com.fly.platform.customer.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fly.platform.common.desensitize.DesensitizeUtils;
import com.fly.platform.common.exception.BusinessException;
import com.fly.platform.customer.dto.CustomerAddressDTO;
import com.fly.platform.customer.entity.CustomerAddress;
import com.fly.platform.customer.mapper.CustomerAddressMapper;
import com.fly.platform.customer.service.CustomerAddressService;
import com.fly.platform.customer.vo.CustomerAddressVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 客户常用地址簿业务实现
 *
 * @author SoarCloud Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerAddressServiceImpl implements CustomerAddressService {

    private final CustomerAddressMapper addressMapper;

    @Override
    public List<CustomerAddressVO> listMine() {
        Long accountId = getCurrentAccountId();
        List<CustomerAddress> list = addressMapper.selectList(new LambdaQueryWrapper<CustomerAddress>()
                .eq(CustomerAddress::getCustomerId, accountId)
                .eq(CustomerAddress::getIsDeleted, 0)
                .orderByDesc(CustomerAddress::getIsDefault)
                .orderByDesc(CustomerAddress::getCreatedAt));

        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        List<CustomerAddressVO> voList = new ArrayList<>();
        for (CustomerAddress item : list) {
            voList.add(convertToVO(item));
        }
        return voList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CustomerAddressDTO dto) {
        Long accountId = getCurrentAccountId();

        if (Integer.valueOf(1).equals(dto.getIsDefault())) {
            clearDefaultAddress(accountId);
        } else {
            Long count = addressMapper.selectCount(new LambdaQueryWrapper<CustomerAddress>()
                    .eq(CustomerAddress::getCustomerId, accountId)
                    .eq(CustomerAddress::getIsDeleted, 0));
            if (count == 0) {
                dto.setIsDefault(1);
            }
        }

        CustomerAddress address = new CustomerAddress();
        address.setCustomerId(accountId);
        address.setContactName(dto.getContactName().trim());
        address.setContactPhone(dto.getContactPhone().trim());
        address.setProvince(dto.getProvince().trim());
        address.setCity(dto.getCity().trim());
        address.setDistrict(dto.getDistrict().trim());
        address.setStreet(dto.getStreet().trim());
        address.setLng(dto.getLng());
        address.setLat(dto.getLat());
        address.setDetail(dto.getDetail().trim());
        address.setIsDefault(Integer.valueOf(1).equals(dto.getIsDefault()) ? 1 : 0);
        address.initAudit();

        addressMapper.insert(address);
        log.info("客户添加常用地址: accountId={}, addressId={}", accountId, address.getAddressId());
        return address.getAddressId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CustomerAddressDTO dto) {
        Long accountId = getCurrentAccountId();
        CustomerAddress address = addressMapper.selectById(id);
        if (address == null) {
            throw new BusinessException(404, "地址记录不存在或已被删除");
        }
        if (!accountId.equals(address.getCustomerId())) {
            throw new BusinessException(403, "只能修改属于您本人的常用地址");
        }

        if (Integer.valueOf(1).equals(dto.getIsDefault())) {
            clearDefaultAddress(accountId);
            address.setIsDefault(1);
        } else {
            address.setIsDefault(0);
        }

        address.setContactName(dto.getContactName().trim());
        address.setContactPhone(dto.getContactPhone().trim());
        address.setProvince(dto.getProvince().trim());
        address.setCity(dto.getCity().trim());
        address.setDistrict(dto.getDistrict().trim());
        address.setStreet(dto.getStreet().trim());
        address.setLng(dto.getLng());
        address.setLat(dto.getLat());
        address.setDetail(dto.getDetail().trim());
        address.setUpdatedAt(LocalDateTime.now());

        addressMapper.updateById(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long accountId = getCurrentAccountId();
        CustomerAddress address = addressMapper.selectById(id);
        if (address == null) {
            throw new BusinessException(404, "地址记录不存在或已被删除");
        }
        if (!accountId.equals(address.getCustomerId())) {
            throw new BusinessException(403, "只能删除属于您本人的常用地址");
        }

        addressMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        Long accountId = getCurrentAccountId();
        CustomerAddress address = addressMapper.selectById(id);
        if (address == null) {
            throw new BusinessException(404, "地址记录不存在或已被删除");
        }
        if (!accountId.equals(address.getCustomerId())) {
            throw new BusinessException(403, "只能操作属于您本人的常用地址");
        }

        clearDefaultAddress(accountId);
        address.setIsDefault(1);
        address.setUpdatedAt(LocalDateTime.now());
        addressMapper.updateById(address);
    }

    private void clearDefaultAddress(Long accountId) {
        addressMapper.update(null, new LambdaUpdateWrapper<CustomerAddress>()
                .eq(CustomerAddress::getCustomerId, accountId)
                .set(CustomerAddress::getIsDefault, 0));
    }

    private CustomerAddressVO convertToVO(CustomerAddress a) {
        String fullRegion = a.getProvince() + a.getCity() + a.getDistrict() + a.getStreet();
        String fullAddr = fullRegion + a.getDetail();

        return CustomerAddressVO.builder()
                .addressId(a.getAddressId())
                .customerId(a.getCustomerId())
                .contactName(a.getContactName())
                .contactPhone(DesensitizeUtils.maskMobile(a.getContactPhone()))
                .province(a.getProvince())
                .city(a.getCity())
                .district(a.getDistrict())
                .street(a.getStreet())
                .fullRegionText(fullRegion)
                .lng(a.getLng())
                .lat(a.getLat())
                .detail(a.getDetail())
                .fullAddress(fullAddr)
                .isDefault(a.getIsDefault())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private Long getCurrentAccountId() {
        if (StpUtil.isLogin()) {
            try {
                return StpUtil.getLoginIdAsLong();
            } catch (Exception ignored) {}
        }
        throw new BusinessException(401, "请先登录客户账号");
    }
}
