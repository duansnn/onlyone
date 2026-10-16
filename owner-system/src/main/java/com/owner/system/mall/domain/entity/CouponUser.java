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
 * 优惠券领用记录。
 */
@Data
@TableName("coupon_user")
public class CouponUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long couponId;

    private Long shopId;

    private Long userId;

    /** UNUSED / LOCKED / USED / RETURNED / EXPIRED */
    private String status;

    /** 占用 / 使用的订单 */
    private Long orderId;

    private LocalDateTime receiveTime;

    private LocalDateTime useTime;

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
