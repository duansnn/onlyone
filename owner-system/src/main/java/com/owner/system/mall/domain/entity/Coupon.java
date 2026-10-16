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
 * 优惠券（店内）。
 */
@Data
@TableName("coupon")
public class Coupon implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long shopId;

    private String name;

    /** 门槛金额（分），商品总额达到才能用，运费不参与 */
    private Long thresholdAmount;

    /** 减免金额（分），必须小于门槛 */
    private Long reduceAmount;

    /** 发放数量 */
    private Integer total;

    /** 已领取数量 */
    private Integer received;

    /** 每人限领 */
    private Integer perUserLimit;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

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
}
