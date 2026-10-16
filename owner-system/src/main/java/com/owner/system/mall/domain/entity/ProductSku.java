package com.owner.system.mall.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品规格（SKU）。可售库存 = stock - lockedStock。
 */
@Data
@TableName("product_sku")
public class ProductSku implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long productId;

    private Long shopId;

    /** 规格名，如「白色 / 大号」；无多规格时写「默认」 */
    private String specName;

    /** 售价（分），必须大于 0 */
    private Long price;

    /** 划线价（分），只展示不参与结算 */
    private Long listPrice;

    /** 库存 */
    private Integer stock;

    /** 未支付订单锁定量 */
    private Integer lockedStock;

    /** 货号，店内唯一，可空 */
    private String skuCode;

    /** 重量（克） */
    private Integer weightGram;

    /** 状态：0 启用，1 停用 */
    private Integer status;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;

    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 可售库存 = stock - lockedStock（不落库） */
    public int availableStock() {
        int s = stock == null ? 0 : stock;
        int l = lockedStock == null ? 0 : lockedStock;
        return Math.max(s - l, 0);
    }
}
