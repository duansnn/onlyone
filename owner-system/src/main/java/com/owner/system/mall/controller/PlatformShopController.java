package com.owner.system.mall.controller;

import com.owner.common.core.domain.PageQuery;
import com.owner.common.core.domain.PageResult;
import com.owner.common.core.domain.R;
import com.owner.system.mall.domain.dto.ShopCreateDTO;
import com.owner.system.mall.domain.vo.PlatformOverviewVO;
import com.owner.system.mall.domain.vo.PlatformShopVO;
import com.owner.system.mall.service.ShopService;
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

/**
 * 平台后台接口，只允许 {@code platform_admin = 1}（koko）访问。
 * <p>平台只建店、停店、看经营数据，不代替店员改价改库存发货。</p>
 */
@RestController
@RequestMapping("/api/platform/shops")
@RequiredArgsConstructor
@Tag(name = "平台-店铺", description = "建店、停店、只读经营概览（仅平台超管）")
public class PlatformShopController {

    private final ShopService shopService;

    @Operation(summary = "平台工作台")
    @GetMapping("/overview")
    public R<PlatformOverviewVO> overview() {
        return R.ok(shopService.platformOverview());
    }

    @Operation(summary = "店铺列表", description = "含在售商品数、待发货数、今日支付金额，只读")
    @GetMapping
    public R<PageResult<PlatformShopVO>> list(
            @Parameter(description = "店名模糊搜索") @RequestParam(required = false) String keyword,
            PageQuery query) {
        return R.ok(shopService.platformShops(keyword, query));
    }

    @Operation(summary = "新建店铺", description = "指定店主（必须是已有用户，或给手机号+初始密码新建）")
    @PostMapping
    public R<Long> create(@Valid @RequestBody ShopCreateDTO dto) {
        return R.ok(shopService.createShop(dto));
    }

    @Operation(summary = "停用 / 启用店铺", description = "0 启用，1 停用")
    @PutMapping("/{shopId}/status")
    public R<Void> setStatus(@PathVariable Long shopId,
                             @Parameter(description = "0 启用，1 停用") @RequestParam int status) {
        shopService.setShopStatus(shopId, status);
        return R.ok();
    }
}
