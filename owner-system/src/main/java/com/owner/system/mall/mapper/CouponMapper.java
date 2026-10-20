package com.owner.system.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.owner.system.mall.domain.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 优惠券 Mapper。
 */
@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 领取计数 +1：只有未领完且启用时才生效。
     *
     * @return 影响行数，0 表示已领完
     */
    @Update("""
            UPDATE coupon
               SET received = received + 1,
                   update_time = NOW()
             WHERE id = #{couponId}
               AND del_flag = 0
               AND status = 0
               AND received < total
            """)
    int increaseReceived(@Param("couponId") Long couponId);

    /**
     * 领取计数 -1：顾客删掉自己的券时把名额退回券池，别人还能领。
     *
     * @return 影响行数
     */
    @Update("""
            UPDATE coupon
               SET received = IF(received > 0, received - 1, 0),
                   update_time = NOW()
             WHERE id = #{couponId}
               AND del_flag = 0
            """)
    int decreaseReceived(@Param("couponId") Long couponId);
}
