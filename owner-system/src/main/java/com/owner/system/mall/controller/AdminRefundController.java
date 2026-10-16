package com.owner.system.mall.controller;

import com.owner.common.core.domain.PageQuery;
import com.owner.common.core.domain.PageResult;
import com.owner.common.core.domain.R;
import com.owner.system.mall.domain.dto.RefundRefuseDTO;
import com.owner.system.mall.domain.vo.RefundVO;
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

/**
 * 店铺后台-售后：处理退款与退货。店主与店员都能操作。
 */
@RestController
@RequestMapping("/api/shop/{shopId}/refunds")
@RequiredArgsConstructor
@Tag(name = "店铺后台-售后", description = "售后筛选、同意、拒绝、确认收到退货")
public class AdminRefundController {

    private final RefundService refundService;

    @Operation(summary = "售后分页", description = "按 PENDING 待处理 / AGREED 已同意 / REJECTED 已拒绝 / SUCCESS 已完成 筛选")
    @GetMapping
    public R<PageResult<RefundVO>> page(
            @PathVariable Long shopId,
            @Parameter(description = "分组筛选：PENDING/AGREED/REJECTED/SUCCESS")
            @RequestParam(required = false) String status,
            PageQuery query) {
        return R.ok(refundService.shopRefunds(shopId, status, query));
    }

    @Operation(summary = "售后详情")
    @GetMapping("/{refundId}")
    public R<RefundVO> detail(@PathVariable Long shopId, @PathVariable Long refundId) {
        return R.ok(refundService.shopRefundDetail(shopId, refundId));
    }

    @Operation(summary = "同意售后",
            description = "仅退款：立刻原路退款并退回库存，订单变已退款；退货退款：给出退货收件信息，等顾客寄回")
    @PostMapping("/{refundId}/agree")
    public R<Void> agree(@PathVariable Long shopId, @PathVariable Long refundId) {
        refundService.agree(shopId, refundId);
        return R.ok();
    }

    @Operation(summary = "拒绝售后", description = "必须填写理由，订单回到申请前的状态")
    @PostMapping("/{refundId}/refuse")
    public R<Void> refuse(@PathVariable Long shopId,
                          @PathVariable Long refundId,
                          @Valid @RequestBody RefundRefuseDTO dto) {
        refundService.refuse(shopId, refundId, dto);
        return R.ok();
    }

    @Operation(summary = "确认收到退货", description = "顾客填了退货运单号之后才能确认，确认后随即发起退款")
    @PostMapping("/{refundId}/received")
    public R<Void> confirmReceived(@PathVariable Long shopId, @PathVariable Long refundId) {
        refundService.confirmReceived(shopId, refundId);
        return R.ok();
    }
}
