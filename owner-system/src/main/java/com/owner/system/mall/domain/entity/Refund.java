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
 * 售后：仅退款 / 退货退款，按整单处理。
 */
@Data
@TableName("refund")
public class Refund implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    private Long shopId;

    private Long userId;

    /** REFUND_ONLY 仅退款，RETURN_REFUND 退货退款 */
    private String type;

    private String reason;

    /** 说明，300 字以内 */
    private String description;

    /** PENDING / AGREED / WAITING_RETURN / RECEIVED / REJECTED / SUCCESS */
    private String status;

    /** 退款金额（分） */
    private Long refundAmount;

    private String refuseReason;

    private String returnCompany;

    private String returnShipNo;

    /** 退货收件信息（默认用店铺发货地址） */
    private String returnAddress;

    private LocalDateTime returnTime;

    private LocalDateTime finishTime;

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
