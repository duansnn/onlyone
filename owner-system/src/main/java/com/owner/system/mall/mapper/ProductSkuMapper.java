package com.owner.system.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.owner.system.mall.domain.entity.ProductSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商品规格 Mapper。
 */
@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {

    /**
     * 条件更新库存与锁定量：只有当可售库存足够时才生效。
     * <p>用 SQL 原子判断替代「先查后改」，避免并发下单超卖。</p>
     *
     * @return 影响行数，0 表示可售库存不足
     */
    @Update("""
            UPDATE product_sku
               SET locked_stock = locked_stock + #{qty},
                   update_time  = NOW()
             WHERE id = #{skuId}
               AND del_flag = 0
               AND status = 0
               AND stock - locked_stock >= #{qty}
            """)
    int lockStock(@Param("skuId") Long skuId, @Param("qty") int qty);

    /**
     * 释放锁定量（取消 / 超时）。锁定量不会小于 0。
     *
     * @return 影响行数
     */
    @Update("""
            UPDATE product_sku
               SET locked_stock = locked_stock - #{qty},
                   update_time  = NOW()
             WHERE id = #{skuId}
               AND locked_stock >= #{qty}
            """)
    int releaseLockedStock(@Param("skuId") Long skuId, @Param("qty") int qty);

    /**
     * 支付成功：把锁定量转成扣减（stock 与 locked_stock 同时减少）。
     *
     * @return 影响行数
     */
    @Update("""
            UPDATE product_sku
               SET stock        = stock - #{qty},
                   locked_stock = locked_stock - #{qty},
                   update_time  = NOW()
             WHERE id = #{skuId}
               AND locked_stock >= #{qty}
               AND stock >= #{qty}
            """)
    int confirmLockedStock(@Param("skuId") Long skuId, @Param("qty") int qty);

    /**
     * 售后退回：把库存加回去。
     *
     * @return 影响行数
     */
    @Update("""
            UPDATE product_sku
               SET stock = stock + #{qty},
                   update_time = NOW()
             WHERE id = #{skuId}
            """)
    int increaseStock(@Param("skuId") Long skuId, @Param("qty") int qty);
}
