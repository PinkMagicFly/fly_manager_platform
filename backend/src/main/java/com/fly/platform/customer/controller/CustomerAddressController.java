package com.fly.platform.customer.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.fly.platform.common.api.Result;
import com.fly.platform.common.audit.Audit;
import com.fly.platform.customer.dto.CustomerAddressDTO;
import com.fly.platform.customer.service.CustomerAddressService;
import com.fly.platform.customer.vo.CustomerAddressVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客户常用地址簿控制器
 *
 * @author SoarCloud Team
 */
@RestController
@RequestMapping({"/api/v1/customer/address", "/api/v1/address"})
@RequiredArgsConstructor
public class CustomerAddressController {

    private final CustomerAddressService addressService;

    /**
     * 获取当前客户常用地址列表
     */
    @GetMapping({"/list", ""})
    @SaCheckLogin
    public Result<List<CustomerAddressVO>> listMine() {
        return Result.ok(addressService.listMine());
    }

    /**
     * 新增常用地址
     */
    @PostMapping
    @SaCheckLogin
    @Audit(module = "地址簿", action = "新增常用地址")
    public Result<Long> create(@Valid @RequestBody CustomerAddressDTO dto) {
        return Result.ok(addressService.create(dto));
    }

    /**
     * 修改常用地址
     */
    @PutMapping("/{id}")
    @SaCheckLogin
    @Audit(module = "地址簿", action = "更新常用地址")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CustomerAddressDTO dto) {
        addressService.update(id, dto);
        return Result.ok();
    }

    /**
     * 删除常用地址
     */
    @DeleteMapping("/{id}")
    @SaCheckLogin
    @Audit(module = "地址簿", action = "删除常用地址")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.delete(id);
        return Result.ok();
    }

    /**
     * 设为默认常用地址
     */
    @PutMapping("/{id}/default")
    @SaCheckLogin
    @Audit(module = "地址簿", action = "设置默认常用地址")
    public Result<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(id);
        return Result.ok();
    }
}
