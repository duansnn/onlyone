package com.owner.system.kanban.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.owner.system.kanban.domain.entity.KanbanUser;
import org.apache.ibatis.annotations.Mapper;

/** 看板用户 Mapper。 */
@Mapper
public interface KanbanUserMapper extends BaseMapper<KanbanUser> {
}
