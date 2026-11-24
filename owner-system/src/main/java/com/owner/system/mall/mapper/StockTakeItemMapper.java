package com.owner.system.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.owner.system.mall.domain.entity.StockTakeItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存盘点明细 Mapper。
 */
@Mapper
public interface StockTakeItemMapper extends BaseMapper<StockTakeItem> {
}
