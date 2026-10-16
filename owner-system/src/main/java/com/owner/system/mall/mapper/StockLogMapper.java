package com.owner.system.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.owner.system.mall.domain.entity.StockLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存流水 Mapper。
 */
@Mapper
public interface StockLogMapper extends BaseMapper<StockLog> {
}
