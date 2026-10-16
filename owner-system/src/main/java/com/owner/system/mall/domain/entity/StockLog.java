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
 * 库存流水：每次库存变化写一条。
 * <p>{@code changeQty} 为本次对 {@code stock} 的实际增减量；
 * 下单锁定与取消释放只动锁定量，因此该值为 0，锁定量看 {@code afterLocked}。</p>
 */
@Data
@TableName("stock_log")
public class StockLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long shopId;

    private Long skuId;

    /** 本次 stock 增减量 */
    private Integer changeQty;

    /** 变动后库存 */
    private Integer afterStock;

    /** 变动后锁定量 */
    private Integer afterLocked;

    /** 变动原因：INIT / MANUAL / LOCK / PAY / CANCEL / RETURN */
    private String reason;

    /** 关联订单 */
    private Long orderId;

    /** 操作人 */
    private Long operatorId;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
