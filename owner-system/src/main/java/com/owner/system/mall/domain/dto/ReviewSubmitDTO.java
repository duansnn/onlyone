package com.owner.system.mall.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 提交评价入参。仅已完成且未评价的订单可以评价，一笔订单一条。
 */
@Schema(description = "提交评价入参")
public record ReviewSubmitDTO(

        @Schema(description = "订单ID")
        @NotNull(message = "订单不能为空")
        Long orderId,

        @Schema(description = "1~5 星", example = "5")
        @NotNull(message = "评分不能为空")
        @Min(value = 1, message = "评分最少 1 星")
        @Max(value = 5, message = "评分最多 5 星")
        Integer score,

        @Schema(description = "文字内容，10~300 字")
        @NotBlank(message = "评价内容不能为空")
        @Size(min = 10, max = 300, message = "评价内容需 10 到 300 字")
        String content,

        @Schema(description = "图片，最多 3 张")
        @Size(max = 3, message = "最多上传 3 张图片")
        List<String> imageUrls
) {
}
