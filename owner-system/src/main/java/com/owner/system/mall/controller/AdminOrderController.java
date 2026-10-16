package com.owner.system.mall.controller;

import com.owner.common.core.domain.PageQuery;
import com.owner.common.core.domain.PageResult;
import com.owner.common.core.domain.R;
import com.owner.system.mall.domain.dto.ShipDTO;
import com.owner.system.mall.domain.vo.OrderDetailVO;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 店铺后台-订单：筛选、详情、发货、改运单号。店主与店员都能操作。
 */
@RestController
@RequestMapping("/api/shop/{shopId}/orders")
@RequiredArgsConstructor
@Tag(name = "店铺后台-订单", description = "订单筛选、详情、发货")
public class AdminOrderController {

    private final OrderService orderService;

    @Operation(summary = "订单分页", description = "可按状态、订单号、收件人手机、下单日期筛选；列表里手机号打星")
    @GetMapping
    public R<PageResult<OrderVO>> page(
            @PathVariable Long shopId,
            @Parameter(description = "订单状态") @RequestParam(required = false) String status,
            @Parameter(description = "订单号，精确") @RequestParam(required = false) String orderNo,
            @Parameter(description = "收件人手机号，精确") @RequestParam(required = false) String receiverMobile,
            @Parameter(description = "下单日期起 yyyy-MM-dd") @RequestParam(required = false) String dateFrom,
            @Parameter(description = "下单日期止 yyyy-MM-dd") @RequestParam(required = false) String dateTo,
            PageQuery query) {
        return R.ok(orderService.shopOrders(shopId, status, orderNo, receiverMobile, dateFrom, dateTo, query));
    }

    @Operation(summary = "常用快递公司")
    @GetMapping("/ship-companies")
    public R<List<String>> shipCompanies(@PathVariable Long shopId) {
        return R.ok(OrderService.SHIP_COMPANIES);
    }

    @Operation(summary = "订单详情", description = "详情里能看到完整收件手机号，因为要发货")
    @GetMapping("/{orderId}")
    public R<OrderDetailVO> detail(@PathVariable Long shopId, @PathVariable Long orderId) {
        return R.ok(orderService.shopOrderDetail(shopId, orderId));
    }

    @Operation(summary = "发货", description = "待发货订单填快递公司与运单号；有未处理售后时先处理售后")
    @PostMapping("/{orderId}/ship")
    public R<Void> ship(@PathVariable Long shopId,
                        @PathVariable Long orderId,
                        @Valid @RequestBody ShipDTO dto) {
        orderService.ship(shopId, orderId, dto);
        return R.ok();
    }

    @Operation(summary = "修改运单号", description = "待收货状态只能改一次")
    @PutMapping("/{orderId}/ship")
    public R<Void> updateShip(@PathVariable Long shopId,
                              @PathVariable Long orderId,
                              @Valid @RequestBody ShipDTO dto) {
        orderService.updateShip(shopId, orderId, dto);
        return R.ok();
    }
}
