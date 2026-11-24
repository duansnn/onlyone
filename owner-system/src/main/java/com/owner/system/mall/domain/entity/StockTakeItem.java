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
 * 库存盘点明细。
 */
@Data
@TableName("stock_take_item")
public class StockTakeItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long takeId;

    private Long shopId;

    private Long skuId;

    /** 账面库存（提交时快照） */
    private Integer bookStock;

    /** 实盘库存 */
    private Integer actualStock;

    /** 差异 = 实盘 - 账面 */
    private Integer diffQty;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
