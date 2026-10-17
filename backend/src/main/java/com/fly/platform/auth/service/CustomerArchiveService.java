package com.fly.platform.auth.service;

import com.fly.platform.auth.dto.CustomerVerifyDTO;
import com.fly.platform.auth.entity.CustomerArchive;
import java.util.List;

public interface CustomerArchiveService {
    CustomerArchive verifyCustomer(Long accountId, CustomerVerifyDTO dto);
    CustomerArchive getMyArchive(Long accountId);
    List<CustomerArchive> listCustomers(Integer authState, String keyword);
    void auditCustomer(Long customerId, Integer authState);
}
