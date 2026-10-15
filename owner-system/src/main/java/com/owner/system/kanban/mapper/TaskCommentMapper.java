package com.owner.system.kanban.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.owner.system.kanban.domain.entity.TaskComment;
import org.apache.ibatis.annotations.Mapper;

/** 任务评论 Mapper。 */
@Mapper
public interface TaskCommentMapper extends BaseMapper<TaskComment> {
}
