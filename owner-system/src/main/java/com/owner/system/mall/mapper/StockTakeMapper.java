package com.owner.system.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.owner.system.mall.domain.entity.StockTake;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存盘点单 Mapper。
 */
@Mapper
public interface StockTakeMapper extends BaseMapper<StockTake> {
}
