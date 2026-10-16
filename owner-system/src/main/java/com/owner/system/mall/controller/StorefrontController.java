package com.owner.system.mall.controller;

import com.owner.common.core.domain.PageQuery;
import com.owner.common.core.domain.PageResult;
import com.owner.common.core.domain.R;
import com.owner.system.mall.domain.vo.ProductCardVO;
import com.owner.system.mall.domain.vo.ProductDetailVO;
import com.owner.system.mall.domain.vo.ReviewVO;
import com.owner.system.mall.domain.vo.ShopBriefVO;
import com.owner.system.mall.domain.vo.StoreHomeVO;
import com.owner.system.mall.service.StorefrontService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 店面前台接口（无需登录）：首页、类目列表、搜索、详情、评价。
 */
@RestController
@RequestMapping("/api/storefront")
@RequiredArgsConstructor
@Tag(name = "店面", description = "首页、类目商品、搜索、商品详情、评价")
public class StorefrontController {

    private final StorefrontService storefrontService;

    @Operation(summary = "营业中的店铺列表", description = "公开接口，前端用于确定默认浏览哪家店")
    @GetMapping("/shops")
    public R<List<ShopBriefVO>> shops() {
        return R.ok(storefrontService.enabledShops());
    }

    @Operation(summary = "店面首页")
    @GetMapping("/shops/{shopId}/home")
    public R<StoreHomeVO> home(@PathVariable Long shopId) {
        return R.ok(storefrontService.home(shopId));
    }

    @Operation(summary = "类目商品列表", description = "每页 12 个；类目可传一级")
    @GetMapping("/shops/{shopId}/products")
    public R<PageResult<ProductCardVO>> products(
            @PathVariable Long shopId,
            @Parameter(description = "类目ID，可空") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "priceAsc / priceDesc / 其它=最新上架") @RequestParam(required = false) String sort,
            PageQuery query) {
        return R.ok(storefrontService.productPage(shopId, categoryId, sort, query));
    }

    @Operation(summary = "搜索本店在售商品", description = "两字起搜，只搜本店")
    @GetMapping("/shops/{shopId}/search")
    public R<PageResult<ProductCardVO>> search(
            @PathVariable Long shopId,
            @Parameter(description = "关键字，至少两个字") @RequestParam String keyword,
            @Parameter(description = "priceAsc / priceDesc / 其它=最新上架") @RequestParam(required = false) String sort,
            PageQuery query) {
        return R.ok(storefrontService.search(shopId, keyword, sort, query));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/shops/{shopId}/products/{productId}")
    public R<ProductDetailVO> detail(@PathVariable Long shopId, @PathVariable Long productId) {
        return R.ok(storefrontService.detail(shopId, productId));
    }

    @Operation(summary = "商品评价列表", description = "每页 10 条，默认最新")
    @GetMapping("/shops/{shopId}/products/{productId}/reviews")
    public R<PageResult<ReviewVO>> reviews(@PathVariable Long shopId,
                                           @PathVariable Long productId,
                                           PageQuery query) {
        return R.ok(storefrontService.reviews(shopId, productId, query));
    }
}
