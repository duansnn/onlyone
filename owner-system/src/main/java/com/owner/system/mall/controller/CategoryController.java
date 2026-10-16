package com.owner.system.mall.controller;

import com.owner.common.core.domain.R;
import com.owner.system.mall.domain.dto.CategorySaveDTO;
import com.owner.system.mall.domain.vo.CategoryVO;
import com.owner.system.mall.service.CategoryService;
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
 * 店铺后台-类目：两级维护，只停用不删除。
 */
@RestController
@RequestMapping("/api/shop/{shopId}/categories")
@RequiredArgsConstructor
@Tag(name = "店铺后台-类目", description = "两级类目列表、保存、停用")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "类目树")
    @GetMapping
    public R<List<CategoryVO>> tree(
            @PathVariable Long shopId,
            @Parameter(description = "是否包含停用类目") @RequestParam(defaultValue = "true") boolean includeDisabled) {
        return R.ok(categoryService.tree(shopId, includeDisabled));
    }

    @Operation(summary = "保存类目", description = "id 为空新增，否则修改；只支持两级")
    @PostMapping
    public R<Long> save(@PathVariable Long shopId, @Valid @RequestBody CategorySaveDTO dto) {
        return R.ok(categoryService.save(shopId, dto));
    }

    @Operation(summary = "停用 / 启用类目", description = "0 启用，1 停用")
    @PutMapping("/{categoryId}/status")
    public R<Void> setStatus(@PathVariable Long shopId,
                             @PathVariable Long categoryId,
                             @Parameter(description = "0 启用，1 停用") @RequestParam int status) {
        categoryService.setStatus(shopId, categoryId, status);
        return R.ok();
    }
}
