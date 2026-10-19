package com.fly.platform.customer;

import cn.dev33.satoken.stp.StpUtil;
import com.fly.platform.common.exception.BusinessException;
import com.fly.platform.customer.dto.CustomerAddressDTO;
import com.fly.platform.customer.service.CustomerAddressService;
import com.fly.platform.customer.vo.CustomerAddressVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 客户常用地址簿业务与越权隔离测试
 *
 * @author SoarCloud Team
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class CustomerAddressTest {

    @Autowired
    private CustomerAddressService addressService;

    @AfterEach
    void tearDown() {
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
    }

    @Test
    @DisplayName("地址簿全生命周期测试: 新增 -> 列表查阅 -> 设为默认 -> 修改 -> 逻辑删除")
    void testAddressLifecycle() {
        // 1. 模拟客户 customer_demo (accountId=6) 登录
        StpUtil.login(6L);

        CustomerAddressDTO dto = new CustomerAddressDTO();
        dto.setContactName("张业务");
        dto.setContactPhone("13912345678");
        dto.setProvince("广东省");
        dto.setCity("深圳市");
        dto.setDistrict("南山区");
        dto.setStreet("粤海街道");
        dto.setLng(new BigDecimal("113.943000"));
        dto.setLat(new BigDecimal("22.540000"));
        dto.setDetail("科技南路深投控大厦12楼B座");
        dto.setIsDefault(0);

        Long addressId = addressService.create(dto);
        assertThat(addressId).isNotNull();

        // 2. 查看我的地址簿
        List<CustomerAddressVO> list = addressService.listMine();
        assertThat(list).isNotEmpty();
        assertThat(list.stream().anyMatch(a -> a.getAddressId().equals(addressId))).isTrue();

        // 3. 设为默认地址
        addressService.setDefault(addressId);
        List<CustomerAddressVO> afterDefaultList = addressService.listMine();
        assertThat(afterDefaultList.get(0).getAddressId()).isEqualTo(addressId);
        assertThat(afterDefaultList.get(0).getIsDefault()).isEqualTo(1);

        // 4. 修改地址
        dto.setDetail("科技南路深投控大厦16楼创新中心");
        addressService.update(addressId, dto);

        List<CustomerAddressVO> updatedList = addressService.listMine();
        CustomerAddressVO updated = updatedList.stream()
                .filter(a -> a.getAddressId().equals(addressId))
                .findFirst().orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getDetail()).contains("16楼创新中心");

        // 5. 逻辑删除地址
        addressService.delete(addressId);
        List<CustomerAddressVO> afterDeleteList = addressService.listMine();
        assertThat(afterDeleteList.stream().anyMatch(a -> a.getAddressId().equals(addressId))).isFalse();
    }

    @Test
    @DisplayName("越权保护守卫测试: 客户不可修改或删除其他客户的常用地址")
    void testCrossCustomerSecurityGuard() {
        // 登录客户1 (accountId=6)
        StpUtil.login(6L);
        CustomerAddressDTO dto = new CustomerAddressDTO();
        dto.setContactName("王主管");
        dto.setContactPhone("13800000006");
        dto.setProvince("广东省");
        dto.setCity("深圳市");
        dto.setDistrict("福田区");
        dto.setStreet("莲花街道");
        dto.setLng(new BigDecimal("114.062000"));
        dto.setLat(new BigDecimal("22.553000"));
        dto.setDetail("深业上城");
        dto.setIsDefault(0);
        Long addrId = addressService.create(dto);

        // 切换为另一账号进行越权尝试 (如 accountId=1 超管或其它用户)
        StpUtil.login(1L);

        assertThatThrownBy(() -> addressService.update(addrId, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能修改属于您本人的常用地址");

        assertThatThrownBy(() -> addressService.delete(addrId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能删除属于您本人的常用地址");
    }
}
