package com.owner.system.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.owner.system.mall.domain.entity.OrderInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订单 Mapper。
 */
@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    /** 某店某时间段内的支付金额合计（分） */
    @Select("""
            SELECT COALESCE(SUM(pay_amount), 0)
              FROM order_info
             WHERE del_flag = 0
               AND shop_id = #{shopId}
               AND pay_time >= #{start}
               AND pay_time < #{end}
            """)
    Long sumPaidAmountByShop(@Param("shopId") Long shopId,
                             @Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end);

    /** 全平台某时间段内的支付金额合计（分） */
    @Select("""
            SELECT COALESCE(SUM(pay_amount), 0)
              FROM order_info
             WHERE del_flag = 0
               AND pay_time >= #{start}
               AND pay_time < #{end}
            """)
    Long sumPaidAmountAll(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 按店铺统计待发货数 */
    @Select("""
            SELECT shop_id AS shopId, COUNT(*) AS cnt
              FROM order_info
             WHERE del_flag = 0
               AND status = 'PENDING_SHIPMENT'
             GROUP BY shop_id
            """)
    List<Map<String, Object>> countPendingShipmentGroupByShop();

    /** 按店铺统计某时间段内的支付金额（分） */
    @Select("""
            SELECT shop_id AS shopId, COALESCE(SUM(pay_amount), 0) AS amount
              FROM order_info
             WHERE del_flag = 0
               AND pay_time >= #{start}
               AND pay_time < #{end}
             GROUP BY shop_id
            """)
    List<Map<String, Object>> sumPaidAmountGroupByShop(@Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end);

    /**
     * 经营分析：某店某时间段内按天统计的已支付订单数 / 支付金额（分）。
     * 仅统计已付款订单（pay_time 非空），保证与「收款」口径一致。
     */
    @Select("""
            SELECT DATE(pay_time)  AS day,
                   COUNT(*)        AS orderCount,
                   COALESCE(SUM(pay_amount), 0) AS paidAmount
              FROM order_info
             WHERE del_flag = 0
               AND shop_id = #{shopId}
               AND pay_time IS NOT NULL
               AND pay_time >= #{start}
               AND pay_time < #{end}
             GROUP BY DATE(pay_time)
             ORDER BY day
            """)
    List<Map<String, Object>> dailySalesTrend(@Param("shopId") Long shopId,
                                              @Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    /**
     * 经营分析：某店某时间段内热销商品 Top N（按销量，取商品名快照）。
     */
    @Select("""
            SELECT oi.product_name          AS productName,
                   COALESCE(SUM(oi.quantity), 0) AS quantity,
                   COALESCE(SUM(oi.amount), 0)   AS amount
              FROM order_item oi
              JOIN order_info o ON o.id = oi.order_id
             WHERE o.del_flag = 0
               AND o.shop_id = #{shopId}
               AND o.pay_time IS NOT NULL
               AND o.pay_time >= #{start}
               AND o.pay_time < #{end}
             GROUP BY oi.product_name
             ORDER BY quantity DESC, amount DESC
             LIMIT #{limit}
            """)
    List<Map<String, Object>> topProducts(@Param("shopId") Long shopId,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end,
                                          @Param("limit") int limit);

    /**
     * 经营分析：某店某时间段内按类目统计的销售额占比（只取销售额 > 0 的类目）。
     */
    @Select("""
            SELECT c.name                    AS categoryName,
                   COALESCE(SUM(oi.amount), 0) AS amount
              FROM order_item oi
              JOIN order_info o ON o.id = oi.order_id
              JOIN product p ON p.id = oi.product_id
              JOIN category c ON c.id = p.category_id
             WHERE o.del_flag = 0
               AND o.shop_id = #{shopId}
               AND o.pay_time IS NOT NULL
               AND o.pay_time >= #{start}
               AND o.pay_time < #{end}
             GROUP BY c.name
             ORDER BY amount DESC
            """)
    List<Map<String, Object>> categorySales(@Param("shopId") Long shopId,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);
}
