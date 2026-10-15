package com.owner.system.kanban.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.owner.system.kanban.domain.entity.Note;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoteMapper extends BaseMapper<Note> {
}
