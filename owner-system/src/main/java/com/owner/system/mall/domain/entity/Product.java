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
 * 商品（SPU）。规格是 SKU，价格和库存都在 SKU 上。
 */
@Data
@TableName("product")
public class Product implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long shopId;

    /** 二级类目 */
    private Long categoryId;

    /** 名称，2~60 字 */
    private String name;

    /** 卖点，200 字以内 */
    private String sellingPoint;

    /** 详情图文 */
    private String detail;

    /** DRAFT 草稿，ON_SALE 在售，OFF_SHELF 已下架 */
    private String status;

    /** 排序，越小越靠前 */
    private Integer sort;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;

    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
