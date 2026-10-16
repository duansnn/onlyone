package com.owner.system.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.owner.system.mall.domain.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单商品 Mapper。
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
