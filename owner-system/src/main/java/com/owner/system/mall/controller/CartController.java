package com.owner.system.mall.controller;

import com.owner.common.core.domain.R;
import com.owner.system.mall.domain.dto.CartLineDTO;
import com.owner.system.mall.domain.dto.CartMergeDTO;
import com.owner.system.mall.domain.dto.CartQuantityDTO;
import com.owner.system.mall.domain.vo.CartVO;
import com.owner.system.mall.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

/**
 * 顾客-购物车。
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "顾客-购物车", description = "购物车列表、加购、改数量、删除、合并本机购物车")
public class CartController {

    private final CartService cartService;

    @Operation(summary = "我的购物车", description = "按店铺分组，失效行单独返回")
    @GetMapping
    public R<CartVO> list() {
        return R.ok(cartService.list());
    }

    @Operation(summary = "购物车行数")
    @GetMapping("/count")
    public R<Long> count() {
        return R.ok(cartService.count());
    }

    @Operation(summary = "加入购物车", description = "同一 SKU 数量相加；停用规格不能加购")
    @PostMapping
    public R<Integer> add(@Valid @RequestBody CartLineDTO dto) {
        return R.ok(cartService.add(dto.skuId(), dto.quantity()));
    }

    @Operation(summary = "合并本机购物车", description = "登录后调用；同一 SKU 数量相加，超过库存时收成库存数")
    @PostMapping("/merge")
    public R<Void> merge(@Valid @RequestBody CartMergeDTO dto) {
        dto.items().forEach(cartService::merge);
        return R.ok();
    }

    @Operation(summary = "改数量", description = "传 0 表示删除该行")
    @PutMapping("/quantity")
    public R<Integer> updateQuantity(@Valid @RequestBody CartQuantityDTO dto) {
        return R.ok(cartService.updateQuantity(dto.skuId(), dto.quantity()));
    }

    @Operation(summary = "删除一行")
    @DeleteMapping("/{skuId}")
    public R<Void> remove(@Parameter(description = "规格ID") @PathVariable Long skuId) {
        cartService.remove(skuId);
        return R.ok();
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping
    public R<Void> clear() {
        cartService.clear();
        return R.ok();
    }
}
