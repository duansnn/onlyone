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
 * 店铺：既是租户，也是卖货的那一家店。
 * <p>商品、库存、购物车、订单、优惠券都带店铺编号，一家店看不见另一家店的数据。</p>
 */
@Data
@TableName("shop")
public class Shop implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花），同时是全系统租户编号 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 店名 */
    private String name;

    /** 简介（500 字以内） */
    private String intro;

    /** 公告，可空 */
    private String notice;

    /** 联系人 */
    private String contactName;

    /** 联系电话 */
    private String contactMobile;

    /** 发货地址（售后退货默认用它） */
    private String shipAddress;

    /** 店招图 */
    private String bannerUrl;

    /** 营业时间说明 */
    private String businessHours;

    /** 收款说明 */
    private String payNote;

    /** 状态：0 营业，1 停用 */
    private Integer status;

    /** 逻辑删除 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;

    /** 版本号（乐观锁） */
    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
