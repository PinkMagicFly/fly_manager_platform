package com.fly.platform.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fly.platform.auth.dto.CustomerVerifyDTO;
import com.fly.platform.auth.entity.CustomerArchive;
import com.fly.platform.auth.mapper.CustomerArchiveMapper;
import com.fly.platform.auth.service.CustomerArchiveService;
import com.fly.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerArchiveServiceImpl implements CustomerArchiveService {

    private final CustomerArchiveMapper customerArchiveMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerArchive verifyCustomer(Long accountId, CustomerVerifyDTO dto) {
        CustomerArchive archive = customerArchiveMapper.selectOne(new LambdaQueryWrapper<CustomerArchive>()
                .eq(CustomerArchive::getAccountId, accountId));
        if (archive == null) {
            archive = new CustomerArchive();
            archive.setAccountId(accountId);
            archive.initAudit();
        } else {
            archive.updateAudit();
        }
        archive.setRealName(dto.getRealName().trim());
        archive.setIdCard(dto.getIdCard().trim());
        archive.setCreditLimit(dto.getCreditLimit());
        archive.setAuthState(1);
        if (archive.getCustomerId() == null) {
            customerArchiveMapper.insert(archive);
        } else {
            customerArchiveMapper.updateById(archive);
        }
        return archive;
    }

    @Override
    public CustomerArchive getMyArchive(Long accountId) {
        return customerArchiveMapper.selectOne(new LambdaQueryWrapper<CustomerArchive>()
                .eq(CustomerArchive::getAccountId, accountId));
    }

    @Override
    public List<CustomerArchive> listCustomers(Integer authState, String keyword) {
        LambdaQueryWrapper<CustomerArchive> qw = new LambdaQueryWrapper<>();
        if (authState != null) {
            qw.eq(CustomerArchive::getAuthState, authState);
        }
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(CustomerArchive::getRealName, keyword)
                    .or().like(CustomerArchive::getIdCard, keyword));
        }
        qw.orderByDesc(CustomerArchive::getCreatedAt);
        return customerArchiveMapper.selectList(qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditCustomer(Long customerId, Integer authState, java.math.BigDecimal creditLimit) {
        CustomerArchive archive = customerArchiveMapper.selectById(customerId);
        if (archive == null) throw new BusinessException("客户实名档案不存在");
        if (authState != null) {
            archive.setAuthState(authState);
        }
        if (creditLimit != null) {
            archive.setCreditLimit(creditLimit);
        }
        archive.setUpdatedAt(LocalDateTime.now());
        customerArchiveMapper.updateById(archive);
    }
}
