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
 * 收货地址：属于用户本人，不属于某一家店。下单时把当时的地址抄进订单。
 */
@Data
@TableName("user_address")
public class UserAddress implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    /** 收件人 */
    private String receiver;

    /** 手机 */
    private String mobile;

    private String province;

    private String city;

    private String district;

    /** 详细地址 */
    private String detail;

    /** 是否默认：0 否，1 是 */
    private Integer isDefault;

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
