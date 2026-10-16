package com.owner.system.mall.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价。
 */
@Schema(description = "评价")
public record ReviewVO(

        @Schema(description = "评价ID")
        Long id,

        @Schema(description = "订单ID")
        Long orderId,

        @Schema(description = "评价人ID")
        Long userId,

        @Schema(description = "评价人昵称")
        String nickname,

        @Schema(description = "评价人头像")
        String avatarUrl,

        @Schema(description = "1~5 星")
        Integer score,

        @Schema(description = "文字内容")
        String content,

        @Schema(description = "图片")
        List<String> images,

        @Schema(description = "评价时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
        LocalDateTime createTime
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
