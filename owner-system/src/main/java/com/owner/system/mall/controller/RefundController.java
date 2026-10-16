package com.owner.system.mall.controller;

import com.owner.common.core.domain.PageQuery;
import com.owner.common.core.domain.PageResult;
import com.owner.common.core.domain.R;
import com.owner.system.mall.domain.dto.RefundApplyDTO;
import com.owner.system.mall.domain.dto.RefundReturnShipDTO;
import com.owner.system.mall.domain.vo.RefundVO;
import com.owner.system.mall.service.OrderService;
import com.owner.system.mall.service.RefundService;
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
 * 顾客-售后：申请、我的售后、填退货运单号。
 */
@RestController
@RequestMapping("/api/refund")
@RequiredArgsConstructor
@Tag(name = "顾客-售后", description = "申请售后、查看进度、填写退货运单号")
public class RefundController {

    private final RefundService refundService;

    @Operation(summary = "申请售后", description = "待发货只能申请仅退款；待收货或已完成 7 天内申请退货退款")
    @PostMapping("/orders/{orderId}")
    public R<Long> apply(@PathVariable Long orderId,
                         @Valid @RequestBody RefundApplyDTO dto) {
        return R.ok(refundService.apply(orderId, dto));
    }

    @Operation(summary = "我的售后列表", description = "可按状态筛选，或直接传分组筛选 PENDING/AGREED/REJECTED/SUCCESS")
    @GetMapping("/mine")
    public R<PageResult<RefundVO>> mine(
            @Parameter(description = "售后状态或分组筛选") @RequestParam(required = false) String status,
            PageQuery query) {
        return R.ok(refundService.myRefunds(status, query));
    }

    @Operation(summary = "售后详情")
    @GetMapping("/{refundId}")
    public R<RefundVO> detail(@PathVariable Long refundId) {
        return R.ok(refundService.myRefundDetail(refundId));
    }

    @Operation(summary = "填写退货运单号", description = "店里同意退货后，顾客把寄回的快递公司与运单号填进来")
    @PostMapping("/{refundId}/return-ship")
    public R<Void> submitReturnShip(@PathVariable Long refundId,
                                    @Valid @RequestBody RefundReturnShipDTO dto) {
        refundService.submitReturnShip(refundId, dto);
        return R.ok();
    }

    @Operation(summary = "售后相关选项", description = "两类原因列表与售后窗口")
    @GetMapping("/options")
    public R<Map<String, Object>> options() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("refundOnlyReasons", RefundService.REFUND_ONLY_REASONS);
        data.put("returnRefundReasons", RefundService.RETURN_REFUND_REASONS);
        data.put("statusFilters", RefundService.STATUS_FILTERS);
        data.put("refundWindowDays", OrderService.REFUND_WINDOW_DAYS);
        return R.ok(data);
    }
}
