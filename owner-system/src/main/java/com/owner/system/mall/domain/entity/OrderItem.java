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
 * 订单商品行。名称、规格、图片、单价都是下单快照，之后改价不影响这笔单。
 */
@Data
@TableName("order_item")
public class OrderItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    private Long shopId;

    private Long productId;

    private Long skuId;

    /** 商品名（快照） */
    private String productName;

    /** 规格名（快照） */
    private String specName;

    /** 封面图（快照） */
    private String imageUrl;

    /** 单价（快照，分） */
    private Long price;

    private Integer quantity;

    /** 小计（分） */
    private Long amount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
