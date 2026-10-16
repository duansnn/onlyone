package com.owner.system.mall.controller;

import com.owner.common.core.domain.PageQuery;
import com.owner.common.core.domain.PageResult;
import com.owner.common.core.domain.R;
import com.owner.system.mall.domain.dto.OrderCancelDTO;
import com.owner.system.mall.domain.dto.OrderPreviewDTO;
import com.owner.system.mall.domain.dto.OrderSubmitDTO;
import com.owner.system.mall.domain.vo.OrderDetailVO;
import com.owner.system.mall.domain.vo.OrderSubmitVO;
import com.owner.system.mall.domain.vo.OrderVO;
import com.owner.system.mall.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 顾客-订单：计价、提交、我的订单、详情、取消、确认收货。
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Tag(name = "顾客-订单", description = "确认订单计价、提交订单、我的订单、取消、确认收货")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "确认订单页计价", description = "运费本期固定 0；返回可用优惠券与被拦下的失效行")
    @PostMapping("/preview")
    public R<com.owner.system.mall.domain.vo.OrderPreviewVO> preview(
            @Valid @RequestBody OrderPreviewDTO dto) {
        return R.ok(orderService.preview(dto));
    }

    @Operation(summary = "提交订单", description = "生成待付款订单并锁定库存，30 分钟未支付自动取消")
    @PostMapping("/submit")
    public R<OrderSubmitVO> submit(@Valid @RequestBody OrderSubmitDTO dto) {
        return R.ok(orderService.submit(dto));
    }

    @Operation(summary = "我的订单", description = "可按状态筛选")
    @GetMapping("/mine")
    public R<PageResult<OrderVO>> mine(
            @Parameter(description = "PENDING_PAYMENT / PENDING_SHIPMENT / PENDING_RECEIPT / FINISHED / CANCELLED / REFUNDING / REFUNDED")
            @RequestParam(required = false) String status,
            PageQuery query) {
        return R.ok(orderService.myOrders(status, query));
    }

    @Operation(summary = "我的订单详情")
    @GetMapping("/{orderId}")
    public R<OrderDetailVO> detail(@PathVariable Long orderId) {
        return R.ok(orderService.myOrderDetail(orderId));
    }

    @Operation(summary = "取消待付款订单")
    @PostMapping("/{orderId}/cancel")
    public R<Void> cancel(@PathVariable Long orderId,
                          @RequestBody(required = false) OrderCancelDTO dto) {
        orderService.cancel(orderId, dto);
        return R.ok();
    }

    @Operation(summary = "确认收货")
    @PostMapping("/{orderId}/confirm")
    public R<Void> confirmReceipt(@PathVariable Long orderId) {
        orderService.confirmReceipt(orderId);
        return R.ok();
    }

    @Operation(summary = "订单相关选项", description = "取消原因、快递公司列表")
    @GetMapping("/options")
    public R<Map<String, Object>> options() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cancelReasons", OrderService.CANCEL_REASONS);
        data.put("shipCompanies", OrderService.SHIP_COMPANIES);
        data.put("payTimeoutMinutes", OrderService.PAY_TIMEOUT_MINUTES);
        data.put("autoFinishDays", OrderService.AUTO_FINISH_DAYS);
        data.put("refundWindowDays", OrderService.REFUND_WINDOW_DAYS);
        return R.ok(data);
    }
}
