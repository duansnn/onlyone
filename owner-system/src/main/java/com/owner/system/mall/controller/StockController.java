package com.owner.system.mall.controller;

import com.owner.common.core.domain.PageQuery;
import com.owner.common.core.domain.PageResult;
import com.owner.common.core.domain.R;
import com.owner.system.mall.domain.dto.StockAdjustDTO;
import com.owner.system.mall.domain.vo.SkuStockVO;
import com.owner.system.mall.domain.vo.StockLogVO;
import com.owner.system.mall.service.StockService;
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
 * 店铺后台-库存：按 SKU 看可售数量，手动调整，看流水。
 */
@RestController
@RequestMapping("/api/shop/{shopId}/stock")
@RequiredArgsConstructor
@Tag(name = "店铺后台-库存", description = "SKU 可售数量、手动调整、库存流水")
public class StockController {

    private final StockService stockService;

    @Operation(summary = "SKU 库存分页")
    @GetMapping("/skus")
    public R<PageResult<SkuStockVO>> skus(
            @PathVariable Long shopId,
            @Parameter(description = "规格名/货号模糊搜索") @RequestParam(required = false) String keyword,
            @Parameter(description = "限定某个商品") @RequestParam(required = false) Long productId,
            PageQuery query) {
        return R.ok(stockService.pageSku(shopId, keyword, productId, query));
    }

    @Operation(summary = "调整库存", description = "填调整后的数量和原因；不能把可售库存打成负数")
    @PostMapping("/adjust")
    public R<Void> adjust(@PathVariable Long shopId, @Valid @RequestBody StockAdjustDTO dto) {
        stockService.adjust(shopId, dto);
        return R.ok();
    }

    @Operation(summary = "库存流水")
    @GetMapping("/logs")
    public R<PageResult<StockLogVO>> logs(
            @PathVariable Long shopId,
            @Parameter(description = "限定某个规格") @RequestParam(required = false) Long skuId,
            PageQuery query) {
        return R.ok(stockService.pageLogs(shopId, skuId, query));
    }
}
