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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 店铺后台-评价：店主可看、可删，但不能改评分。
 */
@RestController
@RequestMapping("/api/shop/{shopId}/reviews")
@RequiredArgsConstructor
@Tag(name = "店铺后台-评价", description = "本店评价列表与删除")
public class AdminReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "评价分页", description = "可按星级筛选")
    @GetMapping
    public R<PageResult<ReviewVO>> page(
            @PathVariable Long shopId,
            @Parameter(description = "星级 1~5") @RequestParam(required = false) Integer score,
            PageQuery query) {
        return R.ok(reviewService.shopReviews(shopId, score, query));
    }

    @Operation(summary = "删除评价", description = "仅店主；用于处理广告、辱骂、泄露隐私，删除后该订单可重新评价")
    @DeleteMapping("/{reviewId}")
    public R<Void> delete(@PathVariable Long shopId, @PathVariable Long reviewId) {
        reviewService.delete(shopId, reviewId);
        return R.ok();
    }
}
