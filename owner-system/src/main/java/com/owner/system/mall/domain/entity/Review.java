package com.owner.system.mall.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评价：仅已完成且未评价的订单可以评价，一笔订单一条。
 */
@Data
@TableName("review")
public class Review implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    private Long shopId;

    private Long userId;

    /** 主商品（订单首行商品） */
    private Long productId;

    /** 1~5 星 */
    private Integer score;

    /** 文字 10~300 字 */
    private String content;

    /** 图片，最多 3 张，逗号分隔 */
    private String imageUrls;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
