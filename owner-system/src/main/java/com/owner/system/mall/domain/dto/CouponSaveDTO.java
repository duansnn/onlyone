package com.owner.system.mall.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 建券 / 改券入参。{@code id} 为空表示新建。
 */
@Schema(description = "保存优惠券入参")
public record CouponSaveDTO(

        @Schema(description = "券ID，新建时留空")
        Long id,

        @Schema(description = "券名称", example = "满 50 减 5")
        @NotBlank(message = "券名称不能为空")
        @Size(max = 64, message = "券名称长度不能超过 64")
        String name,

        @Schema(description = "门槛金额（分），商品总额达到才能用", example = "5000")
        @NotNull(message = "门槛金额不能为空")
        @Min(value = 1, message = "门槛金额必须大于 0")
        Long thresholdAmount,

        @Schema(description = "减免金额（分），必须小于门槛", example = "500")
        @NotNull(message = "减免金额不能为空")
        @Min(value = 1, message = "减免金额必须大于 0")
        Long reduceAmount,

        @Schema(description = "发放数量", example = "100")
        @NotNull(message = "发放数量不能为空")
        @Min(value = 1, message = "发放数量必须大于 0")
        Integer total,

        @Schema(description = "每人限领", example = "1")
        @Min(value = 1, message = "每人限领至少为 1")
        Integer perUserLimit,

        @Schema(description = "开始时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime startTime,

        @Schema(description = "结束时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime endTime,

        @Schema(description = "状态：0 启用，1 停用", example = "0")
        @Pattern(regexp = "^$|0|1", message = "状态只能是 0 或 1")
        String status
) {
}
