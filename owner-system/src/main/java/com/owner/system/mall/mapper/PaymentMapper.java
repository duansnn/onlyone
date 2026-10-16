package com.owner.system.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.owner.system.mall.domain.entity.Payment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付记录 Mapper。
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
}
