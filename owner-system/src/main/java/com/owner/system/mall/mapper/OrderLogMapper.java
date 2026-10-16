package com.owner.system.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.owner.system.mall.domain.entity.OrderLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单状态记录 Mapper。
 */
@Mapper
public interface OrderLogMapper extends BaseMapper<OrderLog> {
}
