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
 * 支付记录。
 */
@Data
@TableName("payment")
public class Payment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    private Long shopId;

    /** WECHAT / ALIPAY */
    private String channel;

    /** 金额（分），必须等于订单应付 */
    private Long amount;

    /** 商户单号 */
    private String outTradeNo;

    /** 支付平台单号 */
    private String platformTradeNo;

    /** PENDING / SUCCESS / FAILED */
    private String status;

    private String failReason;

    /** 异步通知原文 */
    private String notifyBody;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
