package com.owner.system.kanban.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.owner.system.kanban.domain.entity.Article;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
}
