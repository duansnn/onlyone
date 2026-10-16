package com.owner.system.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.owner.system.mall.domain.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 商品 Mapper。
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /** 按店铺统计在售商品数 */
    @Select("""
            SELECT shop_id AS shopId, COUNT(*) AS cnt
              FROM product
             WHERE del_flag = 0
               AND status = 'ON_SALE'
             GROUP BY shop_id
            """)
    List<Map<String, Object>> countOnSaleGroupByShop();

    /**
     * 店面列表 / 搜索：只出「在售」且「至少一个启用规格可售」的商品。
     *
     * @param categoryIds 类目（一级会展开为其二级类目），空表示不限
     * @param keyword     商品名 / 卖点模糊，空表示不限
     * @param sort        priceAsc / priceDesc / 其它（默认按排序 + 上新时间）
     */
    @Select("""
            <script>
            SELECT p.*
              FROM product p
             WHERE p.del_flag = 0
               AND p.shop_id = #{shopId}
               AND p.status = 'ON_SALE'
               AND EXISTS (SELECT 1
                             FROM product_sku s
                            WHERE s.product_id = p.id
                              AND s.del_flag = 0
                              AND s.status = 0
                              AND s.stock - s.locked_stock &gt; 0)
            <if test="categoryIds != null and categoryIds.size() &gt; 0">
               AND p.category_id IN
               <foreach collection="categoryIds" item="cid" open="(" separator="," close=")">#{cid}</foreach>
            </if>
            <if test="keyword != null and keyword != ''">
               AND (p.name LIKE CONCAT('%', #{keyword}, '%')
                    OR p.selling_point LIKE CONCAT('%', #{keyword}, '%'))
            </if>
             ORDER BY
            <choose>
              <when test="sort == 'priceAsc'">
                (SELECT MIN(s2.price) FROM product_sku s2
                  WHERE s2.product_id = p.id AND s2.del_flag = 0 AND s2.status = 0) ASC, p.id DESC
              </when>
              <when test="sort == 'priceDesc'">
                (SELECT MIN(s2.price) FROM product_sku s2
                  WHERE s2.product_id = p.id AND s2.del_flag = 0 AND s2.status = 0) DESC, p.id DESC
              </when>
              <otherwise>p.sort ASC, p.create_time DESC, p.id DESC</otherwise>
            </choose>
             LIMIT #{offset}, #{limit}
            </script>
            """)
    List<Product> selectStorefrontList(@Param("shopId") Long shopId,
                                      @Param("categoryIds") List<Long> categoryIds,
                                      @Param("keyword") String keyword,
                                      @Param("sort") String sort,
                                      @Param("offset") long offset,
                                      @Param("limit") long limit);

    /** 与 {@link #selectStorefrontList} 同条件的总数 */
    @Select("""
            <script>
            SELECT COUNT(*)
              FROM product p
             WHERE p.del_flag = 0
               AND p.shop_id = #{shopId}
               AND p.status = 'ON_SALE'
               AND EXISTS (SELECT 1
                             FROM product_sku s
                            WHERE s.product_id = p.id
                              AND s.del_flag = 0
                              AND s.status = 0
                              AND s.stock - s.locked_stock &gt; 0)
            <if test="categoryIds != null and categoryIds.size() &gt; 0">
               AND p.category_id IN
               <foreach collection="categoryIds" item="cid" open="(" separator="," close=")">#{cid}</foreach>
            </if>
            <if test="keyword != null and keyword != ''">
               AND (p.name LIKE CONCAT('%', #{keyword}, '%')
                    OR p.selling_point LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            </script>
            """)
    long countStorefrontList(@Param("shopId") Long shopId,
                             @Param("categoryIds") List<Long> categoryIds,
                             @Param("keyword") String keyword);
}
