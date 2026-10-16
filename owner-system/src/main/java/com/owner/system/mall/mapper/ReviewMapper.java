package com.owner.system.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.owner.system.mall.domain.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 评价 Mapper。
 * <p>店面详情页要展示「该商品相关订单的评价」，而评价是按订单记录的，
 * 因此这里用订单行做关联查询（review 本身无 del_flag，不会注入逻辑删除条件）。</p>
 */
@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    /** 某商品的评价（按订单行关联，去重） */
    @Select("""
            SELECT DISTINCT r.*
              FROM review r
              JOIN order_item oi ON oi.order_id = r.order_id
             WHERE r.shop_id = #{shopId}
               AND oi.product_id = #{productId}
             ORDER BY r.create_time DESC
             LIMIT #{offset}, #{limit}
            """)
    List<Review> selectPageByProduct(@Param("shopId") Long shopId,
                                     @Param("productId") Long productId,
                                     @Param("offset") long offset,
                                     @Param("limit") long limit);

    /** 某商品的评价总数 */
    @Select("""
            SELECT COUNT(DISTINCT r.id)
              FROM review r
              JOIN order_item oi ON oi.order_id = r.order_id
             WHERE r.shop_id = #{shopId}
               AND oi.product_id = #{productId}
            """)
    long countByProduct(@Param("shopId") Long shopId, @Param("productId") Long productId);

    /** 某商品的平均分，无评价返回 null */
    @Select("""
            SELECT AVG(r.score)
              FROM review r
              JOIN order_item oi ON oi.order_id = r.order_id
             WHERE r.shop_id = #{shopId}
               AND oi.product_id = #{productId}
            """)
    Double averageScoreByProduct(@Param("shopId") Long shopId, @Param("productId") Long productId);
}
