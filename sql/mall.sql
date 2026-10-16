-- ======================================================================
--  日用铺电商系统 —— 业务表 DDL
--  数据库：MySQL 8.x
--
--  使用方式：
--    1) 新库：先执行 sql/init.sql（sys_user 已含 password_hash /
--       platform_admin / avatar_url），再执行本文件。
--    2) 老库升级：若 sys_user 还是旧结构，先执行下面的 ALTER：
--
--       ALTER TABLE `sys_user`
--           ADD COLUMN `password_hash`  VARCHAR(100) NULL COMMENT 'BCrypt 密码',
--           ADD COLUMN `platform_admin` TINYINT NOT NULL DEFAULT 0 COMMENT '平台超级管理员：0 否 1 是',
--           ADD COLUMN `avatar_url`     VARCHAR(255) NULL COMMENT '头像';
--
--  约定：
--    - 金额一律为「分」，BIGINT 整数；
--    - 主键为雪花算法 BIGINT；时间 DATETIME；
--    - 删除用 `del_flag`；有订单引用的商品 / SKU 只停用不物理删除。
-- ======================================================================

USE `owner`;

SET NAMES utf8mb4;

-- ----------------------------------------------------------------------
-- shop 店铺（租户）
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `shop`
(
    `id`             BIGINT       NOT NULL COMMENT '主键（雪花），同时是全系统租户编号',
    `name`           VARCHAR(64)  NOT NULL COMMENT '店名',
    `intro`          VARCHAR(500)          DEFAULT NULL COMMENT '简介（500 字以内）',
    `notice`         VARCHAR(500)          DEFAULT NULL COMMENT '公告，可空',
    `contact_name`   VARCHAR(32)           DEFAULT NULL COMMENT '联系人',
    `contact_mobile` VARCHAR(20)           DEFAULT NULL COMMENT '联系电话',
    `ship_address`   VARCHAR(255)          DEFAULT NULL COMMENT '发货地址（售后退货默认用它）',
    `banner_url`     VARCHAR(255)          DEFAULT NULL COMMENT '店招图',
    `business_hours` VARCHAR(64)           DEFAULT NULL COMMENT '营业时间说明',
    `pay_note`       VARCHAR(255)          DEFAULT NULL COMMENT '收款说明',
    `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0 营业，1 停用',
    `del_flag`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
    `version`        INT          NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）',
    `create_time`    DATETIME              DEFAULT NULL COMMENT '创建时间',
    `update_time`    DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_shop_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='店铺（租户）';

-- ----------------------------------------------------------------------
-- shop_member 店铺成员
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `shop_member`
(
    `id`          BIGINT      NOT NULL COMMENT '主键',
    `shop_id`     BIGINT      NOT NULL COMMENT '店铺',
    `user_id`     BIGINT      NOT NULL COMMENT '用户',
    `role`        VARCHAR(16) NOT NULL COMMENT '角色：OWNER 店主，STAFF 店员',
    `status`      TINYINT     NOT NULL DEFAULT 0 COMMENT '状态：0 正常，1 停用',
    `del_flag`    TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `version`     INT         NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）',
    `create_time` DATETIME             DEFAULT NULL,
    `update_time` DATETIME             DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_shop_member_shop_user` (`shop_id`, `user_id`),
    KEY `idx_shop_member_user` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='店铺成员';

-- ----------------------------------------------------------------------
-- user_address 收货地址（属于用户本人，不属于某一家店）
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_address`
(
    `id`          BIGINT       NOT NULL COMMENT '主键',
    `user_id`     BIGINT       NOT NULL COMMENT '用户',
    `receiver`    VARCHAR(32)  NOT NULL COMMENT '收件人',
    `mobile`      VARCHAR(20)  NOT NULL COMMENT '手机',
    `province`    VARCHAR(32)           DEFAULT NULL COMMENT '省',
    `city`        VARCHAR(32)           DEFAULT NULL COMMENT '市',
    `district`    VARCHAR(32)           DEFAULT NULL COMMENT '区',
    `detail`      VARCHAR(255) NOT NULL COMMENT '详细地址',
    `is_default`  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认：0 否 1 是',
    `del_flag`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `version`     INT          NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）',
    `create_time` DATETIME              DEFAULT NULL,
    `update_time` DATETIME              DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_user_address_user` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='收货地址';

-- ----------------------------------------------------------------------
-- category 类目（店内两级）
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `category`
(
    `id`          BIGINT      NOT NULL COMMENT '主键',
    `shop_id`     BIGINT      NOT NULL COMMENT '店铺',
    `parent_id`   BIGINT      NOT NULL DEFAULT 0 COMMENT '父类目，一级为 0',
    `name`        VARCHAR(32) NOT NULL COMMENT '类目名',
    `sort`        INT         NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    `status`      TINYINT     NOT NULL DEFAULT 0 COMMENT '状态：0 启用，1 停用',
    `del_flag`    TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `version`     INT         NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）',
    `create_time` DATETIME             DEFAULT NULL,
    `update_time` DATETIME             DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_category_shop_parent` (`shop_id`, `parent_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商品类目';

-- ----------------------------------------------------------------------
-- product 商品（SPU）
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `product`
(
    `id`            BIGINT       NOT NULL COMMENT '主键',
    `shop_id`       BIGINT       NOT NULL COMMENT '店铺',
    `category_id`   BIGINT       NOT NULL COMMENT '二级类目',
    `name`          VARCHAR(60)  NOT NULL COMMENT '名称，2~60 字',
    `selling_point` VARCHAR(200)          DEFAULT NULL COMMENT '卖点，200 字以内',
    `detail`        LONGTEXT              DEFAULT NULL COMMENT '详情图文',
    `status`        VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT 草稿，ON_SALE 在售，OFF_SHELF 已下架',
    `sort`          INT          NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    `del_flag`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `version`       INT          NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）',
    `create_time`   DATETIME              DEFAULT NULL,
    `update_time`   DATETIME              DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_product_shop_status` (`shop_id`, `status`),
    KEY `idx_product_category` (`category_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商品 SPU';

-- ----------------------------------------------------------------------
-- product_image 商品主图（sort = 0 为封面）
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `product_image`
(
    `id`          BIGINT       NOT NULL COMMENT '主键',
    `product_id`  BIGINT       NOT NULL COMMENT '商品',
    `url`         VARCHAR(255) NOT NULL COMMENT '图片地址',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序，0 为封面',
    `create_time` DATETIME              DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_product_image_product` (`product_id`, `sort`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商品主图';

-- ----------------------------------------------------------------------
-- product_sku 商品规格
--   可售库存 = stock - locked_stock
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `product_sku`
(
    `id`           BIGINT      NOT NULL COMMENT '主键',
    `product_id`   BIGINT      NOT NULL COMMENT '商品',
    `shop_id`      BIGINT      NOT NULL COMMENT '店铺（冗余，便于按店查询）',
    `spec_name`    VARCHAR(64) NOT NULL DEFAULT '默认' COMMENT '规格名，如「白色 / 大号」',
    `price`        BIGINT      NOT NULL COMMENT '售价（分），必须大于 0',
    `list_price`   BIGINT               DEFAULT NULL COMMENT '划线价（分），只展示不参与结算',
    `stock`        INT         NOT NULL DEFAULT 0 COMMENT '库存，不能为负',
    `locked_stock` INT         NOT NULL DEFAULT 0 COMMENT '未支付订单锁定量',
    `sku_code`     VARCHAR(64)          DEFAULT NULL COMMENT '货号，店内唯一，可空',
    `weight_gram`  INT                  DEFAULT NULL COMMENT '重量（克），留给以后算运费',
    `status`       TINYINT     NOT NULL DEFAULT 0 COMMENT '状态：0 启用，1 停用',
    `del_flag`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `version`      INT         NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）',
    `create_time`  DATETIME             DEFAULT NULL,
    `update_time`  DATETIME             DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_sku_code` (`shop_id`, `sku_code`),
    KEY `idx_product_sku_product` (`product_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商品规格 SKU';

-- ----------------------------------------------------------------------
-- stock_log 库存流水
--   change_qty 为本次对 stock 的实际增减（锁定 / 释放锁定为 0）
--   reason：INIT 初始入库，MANUAL 手动调整，LOCK 下单锁定，
--           PAY 支付扣减，CANCEL 取消释放，RETURN 售后退回
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `stock_log`
(
    `id`           BIGINT      NOT NULL COMMENT '主键',
    `shop_id`      BIGINT      NOT NULL COMMENT '店铺',
    `sku_id`       BIGINT      NOT NULL COMMENT '规格',
    `change_qty`   INT         NOT NULL COMMENT '本次 stock 增减量',
    `after_stock`  INT         NOT NULL COMMENT '变动后库存',
    `after_locked` INT         NOT NULL DEFAULT 0 COMMENT '变动后锁定量',
    `reason`       VARCHAR(16) NOT NULL COMMENT '变动原因',
    `order_id`     BIGINT               DEFAULT NULL COMMENT '关联订单',
    `operator_id`  BIGINT               DEFAULT NULL COMMENT '操作人',
    `remark`       VARCHAR(255)         DEFAULT NULL COMMENT '备注',
    `create_time`  DATETIME             DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_stock_log_sku` (`sku_id`, `create_time`),
    KEY `idx_stock_log_shop` (`shop_id`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='库存流水';

-- ----------------------------------------------------------------------
-- cart_item 购物车（同一用户同一 SKU 一条；删除为物理删除）
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cart_item`
(
    `id`          BIGINT   NOT NULL COMMENT '主键',
    `user_id`     BIGINT   NOT NULL COMMENT '用户',
    `shop_id`     BIGINT   NOT NULL COMMENT '店铺',
    `sku_id`      BIGINT   NOT NULL COMMENT '规格',
    `quantity`    INT      NOT NULL DEFAULT 1 COMMENT '数量',
    `version`     INT      NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）',
    `create_time` DATETIME          DEFAULT NULL,
    `update_time` DATETIME          DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cart_item_user_sku` (`user_id`, `sku_id`),
    KEY `idx_cart_item_shop` (`shop_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='购物车';

-- ----------------------------------------------------------------------
-- order_info 订单
--   status：PENDING_PAYMENT 待付款，PENDING_SHIPMENT 待发货，
--           PENDING_RECEIPT 待收货，FINISHED 已完成，CANCELLED 已取消，
--           REFUNDING 退款中，REFUNDED 已退款
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `order_info`
(
    `id`                BIGINT       NOT NULL COMMENT '主键',
    `order_no`          VARCHAR(32)  NOT NULL COMMENT '展示用订单号，店内唯一',
    `shop_id`           BIGINT       NOT NULL COMMENT '店铺',
    `user_id`           BIGINT       NOT NULL COMMENT '顾客',
    `status`            VARCHAR(20)  NOT NULL COMMENT '订单状态',
    `goods_amount`      BIGINT       NOT NULL DEFAULT 0 COMMENT '商品总额（分）',
    `freight_amount`    BIGINT       NOT NULL DEFAULT 0 COMMENT '运费（分）',
    `discount_amount`   BIGINT       NOT NULL DEFAULT 0 COMMENT '优惠额（分）',
    `pay_amount`        BIGINT       NOT NULL DEFAULT 0 COMMENT '应付（分）',
    `coupon_id`         BIGINT                DEFAULT NULL COMMENT '使用的优惠券',
    `coupon_user_id`    BIGINT                DEFAULT NULL COMMENT '使用的领用记录',
    `receiver_name`     VARCHAR(32)  NOT NULL COMMENT '收件人（下单快照）',
    `receiver_mobile`   VARCHAR(20)  NOT NULL COMMENT '收件手机（下单快照）',
    `receiver_address`  VARCHAR(300) NOT NULL COMMENT '收货地址（下单快照）',
    `buyer_remark`      VARCHAR(200)          DEFAULT NULL COMMENT '买家留言',
    `pay_channel`       VARCHAR(16)           DEFAULT NULL COMMENT 'WECHAT / ALIPAY，未支付为空',
    `pay_no`            VARCHAR(64)           DEFAULT NULL COMMENT '支付平台单号',
    `pay_time`          DATETIME              DEFAULT NULL COMMENT '支付时间',
    `paid_amount`       BIGINT                DEFAULT NULL COMMENT '实付金额（分）',
    `ship_company`      VARCHAR(32)           DEFAULT NULL COMMENT '快递公司',
    `ship_no`           VARCHAR(40)           DEFAULT NULL COMMENT '运单号',
    `ship_time`         DATETIME              DEFAULT NULL COMMENT '发货时间',
    `ship_user_id`      BIGINT                DEFAULT NULL COMMENT '发货人',
    `finish_time`       DATETIME              DEFAULT NULL COMMENT '完成时间',
    `cancel_time`       DATETIME              DEFAULT NULL COMMENT '取消时间',
    `cancel_reason`     VARCHAR(64)           DEFAULT NULL COMMENT '取消原因',
    `expire_time`       DATETIME              DEFAULT NULL COMMENT '待付款超时时间（下单 +30 分钟）',
    `del_flag`          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `version`           INT          NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）',
    `create_time`       DATETIME              DEFAULT NULL,
    `update_time`       DATETIME              DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`shop_id`, `order_no`),
    KEY `idx_order_user_status` (`user_id`, `status`),
    KEY `idx_order_shop_status` (`shop_id`, `status`),
    KEY `idx_order_shop_create` (`shop_id`, `create_time`),
    KEY `idx_order_status_expire` (`status`, `expire_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单';

-- ----------------------------------------------------------------------
-- order_item 订单商品（名称 / 规格 / 图片 / 单价 均为下单快照）
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `order_item`
(
    `id`           BIGINT      NOT NULL COMMENT '主键',
    `order_id`     BIGINT      NOT NULL COMMENT '订单',
    `shop_id`      BIGINT      NOT NULL COMMENT '店铺',
    `product_id`   BIGINT      NOT NULL COMMENT '商品',
    `sku_id`       BIGINT      NOT NULL COMMENT '规格',
    `product_name` VARCHAR(60) NOT NULL COMMENT '商品名（快照）',
    `spec_name`    VARCHAR(64) NOT NULL COMMENT '规格名（快照）',
    `image_url`    VARCHAR(255)         DEFAULT NULL COMMENT '封面图（快照）',
    `price`        BIGINT      NOT NULL COMMENT '单价（快照，分）',
    `quantity`     INT         NOT NULL COMMENT '数量',
    `amount`       BIGINT      NOT NULL COMMENT '小计（分）',
    `create_time`  DATETIME             DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_order_item_order` (`order_id`),
    KEY `idx_order_item_product` (`product_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单商品';

-- ----------------------------------------------------------------------
-- order_log 订单状态记录
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `order_log`
(
    `id`            BIGINT      NOT NULL COMMENT '主键',
    `order_id`      BIGINT      NOT NULL COMMENT '订单',
    `from_status`   VARCHAR(20)          DEFAULT NULL COMMENT '原状态',
    `to_status`     VARCHAR(20) NOT NULL COMMENT '新状态',
    `operator_id`   BIGINT               DEFAULT NULL COMMENT '操作人',
    `operator_type` VARCHAR(16) NOT NULL COMMENT 'CUSTOMER / SHOP / PLATFORM / SYSTEM',
    `remark`        VARCHAR(255)         DEFAULT NULL COMMENT '备注',
    `create_time`   DATETIME             DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_order_log_order` (`order_id`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单状态记录';

-- ----------------------------------------------------------------------
-- payment 支付记录
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `payment`
(
    `id`                BIGINT      NOT NULL COMMENT '主键',
    `order_id`          BIGINT      NOT NULL COMMENT '订单',
    `shop_id`           BIGINT      NOT NULL COMMENT '店铺',
    `channel`           VARCHAR(16) NOT NULL COMMENT 'WECHAT / ALIPAY',
    `amount`            BIGINT      NOT NULL COMMENT '金额（分）',
    `out_trade_no`      VARCHAR(64) NOT NULL COMMENT '商户单号',
    `platform_trade_no` VARCHAR(64)          DEFAULT NULL COMMENT '支付平台单号',
    `status`            VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / SUCCESS / FAILED',
    `fail_reason`       VARCHAR(255)         DEFAULT NULL COMMENT '失败原因',
    `notify_body`       TEXT                 DEFAULT NULL COMMENT '异步通知原文',
    `create_time`       DATETIME             DEFAULT NULL,
    `update_time`       DATETIME             DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_out_trade_no` (`out_trade_no`),
    KEY `idx_payment_order` (`order_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='支付记录';

-- ----------------------------------------------------------------------
-- refund 售后
--   type：REFUND_ONLY 仅退款，RETURN_REFUND 退货退款
--   status：PENDING 待处理，AGREED 已同意，WAITING_RETURN 待退货，
--           RECEIVED 已收货，REJECTED 已拒绝，SUCCESS 退款成功
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `refund`
(
    `id`             BIGINT       NOT NULL COMMENT '主键',
    `order_id`       BIGINT       NOT NULL COMMENT '订单',
    `shop_id`        BIGINT       NOT NULL COMMENT '店铺',
    `user_id`        BIGINT       NOT NULL COMMENT '顾客',
    `type`           VARCHAR(16)  NOT NULL COMMENT 'REFUND_ONLY / RETURN_REFUND',
    `reason`         VARCHAR(64)  NOT NULL COMMENT '原因',
    `description`    VARCHAR(300)          DEFAULT NULL COMMENT '说明，300 字以内',
    `status`         VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '售后状态',
    `refund_amount`  BIGINT       NOT NULL DEFAULT 0 COMMENT '退款金额（分）',
    `refuse_reason`  VARCHAR(255)          DEFAULT NULL COMMENT '拒绝理由',
    `return_company` VARCHAR(32)           DEFAULT NULL COMMENT '退货快递公司',
    `return_ship_no` VARCHAR(40)           DEFAULT NULL COMMENT '退货运单号',
    `return_address` VARCHAR(300)          DEFAULT NULL COMMENT '退货收件信息',
    `return_time`    DATETIME              DEFAULT NULL COMMENT '顾客填单号时间',
    `finish_time`    DATETIME              DEFAULT NULL COMMENT '完成时间',
    `del_flag`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `version`        INT          NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）',
    `create_time`    DATETIME              DEFAULT NULL,
    `update_time`    DATETIME              DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_refund_order` (`order_id`),
    KEY `idx_refund_shop_status` (`shop_id`, `status`),
    KEY `idx_refund_user` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='售后';

-- ----------------------------------------------------------------------
-- review 评价（一笔订单一条）
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `review`
(
    `id`          BIGINT        NOT NULL COMMENT '主键',
    `order_id`    BIGINT        NOT NULL COMMENT '订单',
    `shop_id`     BIGINT        NOT NULL COMMENT '店铺',
    `user_id`     BIGINT        NOT NULL COMMENT '顾客',
    `product_id`  BIGINT                 DEFAULT NULL COMMENT '主商品（首行商品）',
    `score`       TINYINT       NOT NULL COMMENT '1~5 星',
    `content`     VARCHAR(300)  NOT NULL COMMENT '文字 10~300 字',
    `image_urls`  VARCHAR(1000)          DEFAULT NULL COMMENT '图片，最多 3 张，逗号分隔',
    `create_time` DATETIME               DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_review_order` (`order_id`),
    KEY `idx_review_shop` (`shop_id`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='评价';

-- ----------------------------------------------------------------------
-- coupon 优惠券（店内）
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `coupon`
(
    `id`               BIGINT      NOT NULL COMMENT '主键',
    `shop_id`          BIGINT      NOT NULL COMMENT '店铺',
    `name`             VARCHAR(64) NOT NULL COMMENT '名称',
    `threshold_amount` BIGINT      NOT NULL COMMENT '门槛金额（分）',
    `reduce_amount`    BIGINT      NOT NULL COMMENT '减免金额（分），必须小于门槛',
    `total`            INT         NOT NULL COMMENT '发放数量',
    `received`         INT         NOT NULL DEFAULT 0 COMMENT '已领取数量',
    `per_user_limit`   INT         NOT NULL DEFAULT 1 COMMENT '每人限领',
    `start_time`       DATETIME             DEFAULT NULL COMMENT '开始时间',
    `end_time`         DATETIME             DEFAULT NULL COMMENT '结束时间',
    `status`           TINYINT     NOT NULL DEFAULT 0 COMMENT '状态：0 启用，1 停用',
    `del_flag`         TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `version`          INT         NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）',
    `create_time`      DATETIME             DEFAULT NULL,
    `update_time`      DATETIME             DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_coupon_shop_status` (`shop_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='优惠券';

-- ----------------------------------------------------------------------
-- coupon_user 优惠券领用
--   status：UNUSED 未使用，LOCKED 已占用，USED 已使用，
--           RETURNED 已退回，EXPIRED 已过期
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `coupon_user`
(
    `id`           BIGINT      NOT NULL COMMENT '主键',
    `coupon_id`    BIGINT      NOT NULL COMMENT '优惠券',
    `shop_id`      BIGINT      NOT NULL COMMENT '店铺',
    `user_id`      BIGINT      NOT NULL COMMENT '用户',
    `status`       VARCHAR(16) NOT NULL DEFAULT 'UNUSED' COMMENT '领用状态',
    `order_id`     BIGINT               DEFAULT NULL COMMENT '占用 / 使用的订单',
    `receive_time` DATETIME             DEFAULT NULL COMMENT '领取时间',
    `use_time`     DATETIME             DEFAULT NULL COMMENT '使用时间',
    `del_flag`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `version`      INT         NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）',
    `create_time`  DATETIME             DEFAULT NULL,
    `update_time`  DATETIME             DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_coupon_user_user` (`user_id`, `status`),
    KEY `idx_coupon_user_coupon` (`coupon_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='优惠券领用';
