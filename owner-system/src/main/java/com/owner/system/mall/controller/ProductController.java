package com.owner.system.mall.controller;

import com.owner.common.core.domain.PageQuery;
import com.owner.common.core.domain.PageResult;
import com.owner.common.core.domain.R;
import com.owner.system.mall.domain.dto.ProductSaveDTO;
import com.owner.system.mall.domain.vo.ProductAdminDetailVO;
import com.owner.system.mall.domain.vo.ProductAdminVO;
import com.owner.system.mall.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 店铺后台-商品：新建、编辑、上下架、删除。
 */
@RestController
@RequestMapping("/api/shop/{shopId}/products")
@RequiredArgsConstructor
@Tag(name = "店铺后台-商品", description = "商品列表、详情、保存、上下架")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "商品分页")
    @GetMapping
    public R<PageResult<ProductAdminVO>> page(
            @PathVariable Long shopId,
            @Parameter(description = "名称/卖点模糊搜索") @RequestParam(required = false) String keyword,
            @Parameter(description = "DRAFT / ON_SALE / OFF_SHELF") @RequestParam(required = false) String status,
            @Parameter(description = "二级类目") @RequestParam(required = false) Long categoryId,
            PageQuery query) {
        return R.ok(productService.adminPage(shopId, keyword, status, categoryId, query));
    }

    @Operation(summary = "导出商品 Excel", description = "筛选条件与商品列表相同。一行一商品，规格拼在同一格。没有商品或超过 5000 个时返回业务错误。")
    @GetMapping("/export")
    public void export(@PathVariable Long shopId,
                       @Parameter(description = "名称/卖点模糊搜索") @RequestParam(required = false) String keyword,
                       @Parameter(description = "DRAFT / ON_SALE / OFF_SHELF") @RequestParam(required = false) String status,
                       @Parameter(description = "二级类目") @RequestParam(required = false) Long categoryId,
                       HttpServletResponse response) throws java.io.IOException {
        byte[] body = productService.exportShopProducts(shopId, keyword, status, categoryId);
        String filename = "商品-" + DateTimeFormatter.ofPattern("yyyyMMddHHmm").format(LocalDateTime.now()) + ".xlsx";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
    }

    @Operation(summary = "商品详情（编辑用）")
    @GetMapping("/{productId}")
    public R<ProductAdminDetailVO> detail(@PathVariable Long shopId, @PathVariable Long productId) {
        return R.ok(productService.adminDetail(shopId, productId));
    }

    @Operation(summary = "新建商品", description = "主图 + 规格一并提交；不满足上架要求会自动存为草稿")
    @PostMapping
    public R<Long> create(@PathVariable Long shopId, @Valid @RequestBody ProductSaveDTO dto) {
        return R.ok(productService.create(shopId, dto));
    }

    @Operation(summary = "修改商品")
    @PutMapping("/{productId}")
    public R<Void> update(@PathVariable Long shopId,
                          @PathVariable Long productId,
                          @Valid @RequestBody ProductSaveDTO dto) {
        productService.update(shopId, productId, dto);
        return R.ok();
    }

    @Operation(summary = "上架 / 下架", description = "ON_SALE 在售，OFF_SHELF 已下架")
    @PutMapping("/{productId}/status")
    public R<Void> setStatus(@PathVariable Long shopId,
                             @PathVariable Long productId,
                             @Parameter(description = "ON_SALE / OFF_SHELF") @RequestParam String status) {
        productService.setStatus(shopId, productId, status);
        return R.ok();
    }

    @Operation(summary = "删除商品",
            description = "逻辑删除，规格与主图一并标记删除；有未完结订单时拒绝")
    @DeleteMapping("/{productId}")
    public R<Void> delete(@PathVariable Long shopId, @PathVariable Long productId) {
        productService.delete(shopId, productId);
        return R.ok();
    }
}
