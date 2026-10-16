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
}
