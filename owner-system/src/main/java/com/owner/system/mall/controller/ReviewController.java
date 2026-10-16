package com.owner.system.mall.controller;

import com.owner.common.core.domain.R;
import com.owner.system.mall.domain.dto.ReviewSubmitDTO;
import com.owner.system.mall.domain.vo.ReviewVO;
import com.owner.system.mall.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 顾客-评价：提交、查看自己的评价。
 */
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
@Tag(name = "顾客-评价", description = "提交评价、查看某笔订单的评价")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "提交评价", description = "仅已完成且未评价的订单可以评价，一笔订单一条，提交后不能改")
    @PostMapping
    public R<Long> submit(@Valid @RequestBody ReviewSubmitDTO dto) {
        return R.ok(reviewService.submit(dto));
    }

    @Operation(summary = "某笔订单的评价", description = "没评价过返回 null")
    @GetMapping("/orders/{orderId}")
    public R<ReviewVO> myReview(@PathVariable Long orderId) {
        return R.ok(reviewService.myReview(orderId));
    }
}
