package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 合并本机购物车入参：登录后把未登录时暂存的行并入账号购物车，
 * 同一 SKU 数量相加，超过库存时收成库存数。
 */
@Schema(description = "合并本机购物车入参")
public record CartMergeDTO(

        @Schema(description = "本机暂存的购物车行")
        @NotEmpty(message = "购物车行不能为空")
        @Size(max = 100, message = "一次最多合并 100 行")
        List<@Valid CartLineDTO> items
) {
}
