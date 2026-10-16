package com.owner.system.mall.controller;

import com.owner.common.core.domain.PageQuery;
import com.owner.common.core.domain.PageResult;
import com.owner.common.core.domain.R;
import com.owner.system.mall.domain.vo.CouponUserVO;
import com.owner.system.mall.domain.vo.CouponVO;
import com.owner.system.mall.support.MallCurrentUser;
import com.owner.system.mall.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 顾客-优惠券：领券中心、领取、我的券、结算可用券。
 */
@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
@Tag(name = "顾客-优惠券", description = "领券中心、领取、我的券、结算可用券")
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "领券中心", description = "本店还能领的券")
    @GetMapping("/center")
    public R<PageResult<CouponVO>> center(@RequestParam Long shopId, PageQuery query) {
        return R.ok(couponService.receiveCenter(shopId, query));
    }

    @Operation(summary = "领取优惠券", description = "过期券不能领；受每人限领与发放总量限制")
    @PostMapping("/{couponId}/receive")
    public R<Long> receive(@PathVariable Long couponId) {
        return R.ok(couponService.receive(couponId));
    }

    @Operation(summary = "我的券", description = "未使用、已占用、已使用、已退回、已过期；到期的未使用券会懒标记为已过期")
    @GetMapping("/mine")
    public R<List<CouponUserVO>> mine(
            @Parameter(description = "店铺ID，不传就是全部") @RequestParam(required = false) Long shopId,
            @Parameter(description = "UNUSED/LOCKED/USED/RETURNED/EXPIRED")
            @RequestParam(required = false) String status) {
        return R.ok(couponService.myCoupons(shopId, status));
    }

    @Operation(summary = "结算可用券", description = "门槛已满足、在有效期内、属于本店的券")
    @GetMapping("/usable")
    public R<List<CouponUserVO>> usable(@RequestParam Long shopId,
                                       @Parameter(description = "商品总额（分），运费不参与门槛")
                                       @RequestParam long goodsAmount) {
        return R.ok(couponService.usableCoupons(MallCurrentUser.requireUserId(), shopId, goodsAmount));
    }
}
