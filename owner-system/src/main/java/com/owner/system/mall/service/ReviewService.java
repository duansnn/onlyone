package com.owner.system.mall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.common.core.domain.PageQuery;
import com.owner.common.core.domain.PageResult;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.common.core.utils.StringUtils;
import com.owner.system.mall.domain.dto.ReviewSubmitDTO;
import com.owner.system.mall.domain.entity.OrderInfo;
import com.owner.system.mall.domain.entity.OrderItem;
import com.owner.system.mall.domain.entity.Refund;
import com.owner.system.mall.domain.entity.Review;
import com.owner.system.mall.domain.enums.OrderStatus;
import com.owner.system.mall.domain.enums.RefundStatus;
import com.owner.system.mall.domain.vo.ReviewVO;
import com.owner.system.mall.mapper.OrderItemMapper;
import com.owner.system.mall.mapper.RefundMapper;
import com.owner.system.mall.mapper.ReviewMapper;
import com.owner.system.mall.support.MallCurrentUser;
import com.owner.system.mall.support.ShopGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 评价服务（PRD §10）。
 *
 * <p>只有「已完成」的订单能评价，一笔订单评一次，按订单记录而不是按商品拆开；
 * 提交后不能改也不能删，店主也不能删差评，后台只读。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderItemMapper orderItemMapper;
    private final RefundMapper refundMapper;
    private final OrderService orderService;
    private final StorefrontService storefrontService;
    private final ShopGuard shopGuard;

    // ------------------------------------------------------------------
    // 顾客侧
    // ------------------------------------------------------------------

    /** 提交评价 */
    @Transactional(rollbackFor = Exception.class)
    public Long submit(ReviewSubmitDTO dto) {
        Long userId = MallCurrentUser.requireUserId();
        OrderInfo order = orderService.requireOrder(dto.orderId());
        if (!userId.equals(order.getUserId())) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "订单不存在");
        }
        if (!OrderStatus.FINISHED.name().equals(order.getStatus())) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "只有已完成的订单可以评价");
        }
        if (hasProcessingRefund(order.getId())) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "这笔订单有未处理完的售后，处理完再评价");
        }
        if (reviewMapper.selectCount(Wrappers.<Review>lambdaQuery()
                .eq(Review::getOrderId, order.getId())) > 0) {
            throw new ServiceException(ResultCode.DATA_EXISTS, "这笔订单已经评价过了，评价不能修改或删除");
        }

        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, order.getId())
                .orderByAsc(OrderItem::getId));
        if (items.isEmpty()) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "订单没有商品，无法评价");
        }

        List<String> images = dto.imageUrls() == null ? List.of() : dto.imageUrls().stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .limit(3)
                .toList();

        Review review = new Review();
        review.setOrderId(order.getId());
        review.setShopId(order.getShopId());
        review.setUserId(userId);
        review.setProductId(items.get(0).getProductId());
        review.setScore(dto.score());
        review.setContent(dto.content().trim());
        review.setImageUrls(images.isEmpty() ? null : String.join(",", images));
        reviewMapper.insert(review);

        log.info("[Review] 评价成功 reviewId={}, orderId={}, score={}",
                review.getId(), order.getId(), dto.score());
        return review.getId();
    }

    /** 某笔订单的评价，没有返回 null */
    public ReviewVO myReview(Long orderId) {
        Long userId = MallCurrentUser.requireUserId();
        Review review = reviewMapper.selectOne(Wrappers.<Review>lambdaQuery()
                .eq(Review::getOrderId, orderId)
                .last("LIMIT 1"));
        if (review == null || !userId.equals(review.getUserId())) {
            return null;
        }
        List<ReviewVO> rows = storefrontService.toReviewVos(List.of(review));
        return rows.isEmpty() ? null : rows.get(0);
    }

    // ------------------------------------------------------------------
    // 店铺后台（只读）
    // ------------------------------------------------------------------

    /** 本店评价分页，店主与店员都能看，没有删除入口 */
    public PageResult<ReviewVO> shopReviews(Long shopId, Integer score, PageQuery query) {
        shopGuard.requireMember(shopId);
        Page<Review> page = reviewMapper.selectPage(
                new Page<>(query.getPageNumOrDefault(), query.getPageSizeOrDefault()),
                Wrappers.<Review>lambdaQuery()
                        .eq(Review::getShopId, shopId)
                        .eq(score != null, Review::getScore, score)
                        .orderByDesc(Review::getCreateTime)
                        .orderByDesc(Review::getId));
        return PageResult.of(storefrontService.toReviewVos(page.getRecords()),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    // ------------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------------

    private boolean hasProcessingRefund(Long orderId) {
        return refundMapper.selectCount(Wrappers.<Refund>lambdaQuery()
                .eq(Refund::getOrderId, orderId)
                .in(Refund::getStatus, RefundStatus.PENDING.name(), RefundStatus.AGREED.name(),
                        RefundStatus.WAITING_RETURN.name(), RefundStatus.RECEIVED.name())) > 0;
    }
}
