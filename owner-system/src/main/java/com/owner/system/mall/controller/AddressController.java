package com.owner.system.mall.controller;

import com.owner.common.core.domain.R;
import com.owner.system.mall.domain.dto.AddressSaveDTO;
import com.owner.system.mall.domain.vo.AddressVO;
import com.owner.system.mall.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 顾客-收货地址。地址属于用户本人，不属于某一家店。
 */
@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
@Tag(name = "顾客-收货地址", description = "地址列表、保存、删除、设默认")
public class AddressController {

    private final AddressService addressService;

    @Operation(summary = "我的地址列表", description = "默认地址排最前，每人最多 20 条")
    @GetMapping
    public R<List<AddressVO>> list() {
        return R.ok(addressService.list());
    }

    @Operation(summary = "保存地址", description = "id 为空新增，否则修改")
    @PostMapping
    public R<Long> save(@Valid @RequestBody AddressSaveDTO dto) {
        return R.ok(addressService.save(dto));
    }

    @Operation(summary = "设为默认地址")
    @PutMapping("/{addressId}/default")
    public R<Void> setDefault(@PathVariable Long addressId) {
        addressService.setDefault(addressId);
        return R.ok();
    }

    @Operation(summary = "删除地址")
    @DeleteMapping("/{addressId}")
    public R<Void> remove(@PathVariable Long addressId) {
        addressService.remove(addressId);
        return R.ok();
    }
}
