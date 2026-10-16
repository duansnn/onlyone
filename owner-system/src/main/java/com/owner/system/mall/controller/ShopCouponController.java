package com.owner.system.mall.controller;

import com.owner.common.core.domain.PageQuery;
import com.owner.common.core.domain.PageResult;
import com.owner.common.core.domain.R;
import com.owner.system.mall.domain.dto.CouponSaveDTO;
import com.owner.system.mall.domain.vo.CouponVO;
import com.owner.system.mall.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 店铺后台-优惠券：店主可建券；店员只读。
 */
@RestController
@RequestMapping("/api/shop/{shopId}/coupons")
@RequiredArgsConstructor
@Tag(name = "店铺后台-优惠券", description = "建券、改券、停用、发给指定用户")
public class ShopCouponController {

    private final CouponService couponService;

    @Operation(summary = "本店优惠券分页", description = "店主与店员都能看")
    @GetMapping
    public R<PageResult<CouponVO>> page(
            @PathVariable Long shopId,
            @Parameter(description = "0 启用，1 停用") @RequestParam(required = false) String status,
            PageQuery query) {
        return R.ok(couponService.shopCoupons(shopId, status, query));
    }

    @Operation(summary = "新建或修改优惠券", description = "仅店主；减免金额必须小于门槛金额；id 留空表示新建")
    @PostMapping
    public R<Long> save(@PathVariable Long shopId, @Valid @RequestBody CouponSaveDTO dto) {
        return R.ok(couponService.saveCoupon(shopId, dto));
    }

    @Operation(summary = "停用或启用优惠券", description = "仅店主；0 启用，1 停用")
    @PutMapping("/{couponId}/status")
    public R<Void> setStatus(@PathVariable Long shopId,
                             @PathVariable Long couponId,
                             @Parameter(description = "0 启用，1 停用") @RequestParam int status) {
        couponService.setCouponStatus(shopId, couponId, status);
        return R.ok();
    }

    @Operation(summary = "把券发给指定用户", description = "仅店主；按手机号定位用户")
    @PostMapping("/{couponId}/grant")
    public R<Void> grant(@PathVariable Long shopId,
                         @PathVariable Long couponId,
                         @Parameter(description = "用户手机号") @RequestParam String mobile) {
        couponService.grantToUser(shopId, couponId, mobile);
        return R.ok();
    }
}
