package com.owner.system.kanban.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.owner.system.kanban.domain.entity.Project;
import org.apache.ibatis.annotations.Mapper;

/** 看板项目 Mapper。 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
}
