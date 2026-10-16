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
 * 订单。金额一律为分。
 */
@Data
@TableName("order_info")
public class OrderInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 展示用订单号，店内唯一 */
    private String orderNo;

    private Long shopId;

    /** 顾客 */
    private Long userId;

    /** PENDING_PAYMENT / PENDING_SHIPMENT / PENDING_RECEIPT / FINISHED / CANCELLED / REFUNDING / REFUNDED */
    private String status;

    /** 商品总额 = 各行单价 × 数量 */
    private Long goodsAmount;

    /** 运费，第一期固定 0 */
    private Long freightAmount;

    /** 优惠额 */
    private Long discountAmount;

    /** 应付 = 商品总额 + 运费 − 优惠额，最小为 0 */
    private Long payAmount;

    /** 使用的优惠券 */
    private Long couponId;

    /** 使用的领用记录 */
    private Long couponUserId;

    /** 收件人（下单快照） */
    private String receiverName;

    /** 收件手机（下单快照） */
    private String receiverMobile;

    /** 收货地址（下单快照） */
    private String receiverAddress;

    /** 买家留言 */
    private String buyerRemark;

    /** WECHAT / ALIPAY，未支付为空 */
    private String payChannel;

    /** 支付平台单号 */
    private String payNo;

    private LocalDateTime payTime;

    /** 实付金额 */
    private Long paidAmount;

    private String shipCompany;

    private String shipNo;

    private LocalDateTime shipTime;

    private Long shipUserId;

    private LocalDateTime finishTime;

    private LocalDateTime cancelTime;

    private String cancelReason;

    /** 待付款超时时间（下单 +30 分钟） */
    private LocalDateTime expireTime;

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
