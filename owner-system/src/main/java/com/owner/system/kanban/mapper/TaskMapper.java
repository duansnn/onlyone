package com.owner.system.kanban.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.owner.system.kanban.domain.entity.Task;
import org.apache.ibatis.annotations.Mapper;

/** 看板任务 Mapper。 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
