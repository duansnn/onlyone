package com.owner.system.mall.controller;

import com.owner.common.core.domain.PageQuery;
import com.owner.common.core.domain.PageResult;
import com.owner.common.core.domain.R;
import com.owner.system.mall.domain.vo.ReviewVO;
import com.owner.system.mall.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 店铺后台-评价：只读。店主不能删除差评。
 */
@RestController
@RequestMapping("/api/shop/{shopId}/reviews")
@RequiredArgsConstructor
@Tag(name = "店铺后台-评价", description = "本店评价只读列表")
public class AdminReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "评价分页", description = "可按星级筛选；只读，没有删除入口")
    @GetMapping
    public R<PageResult<ReviewVO>> page(
            @PathVariable Long shopId,
            @Parameter(description = "星级 1~5") @RequestParam(required = false) Integer score,
            PageQuery query) {
        return R.ok(reviewService.shopReviews(shopId, score, query));
    }
}
