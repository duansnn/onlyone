package com.owner.system.kanban.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.system.kanban.domain.entity.Article;
import com.owner.system.kanban.mapper.ArticleMapper;
import com.owner.system.kanban.support.KanbanCurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 博客文章服务（数据按 userId 隔离）。
 */
@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleMapper articleMapper;

    /** 文章列表：按状态/分类/关键词筛选，更新时间倒序。 */
    public List<Article> list(String status, String category, String keyword) {
        Long userId = KanbanCurrentUser.getRequiredUserId();
        return articleMapper.selectList(Wrappers.<Article>lambdaQuery()
                .eq(Article::getUserId, userId)
                .eq(status != null && !status.isBlank(), Article::getStatus, status)
                .eq(category != null && !category.isBlank(), Article::getCategory, category)
                .like(keyword != null && !keyword.isBlank(), Article::getTitle, keyword)
                .orderByDesc(Article::getUpdateTime));
    }

    /** 新建/保存文章，返回 id。 */
    public Long save(Long id, String title, String content, String category, String tags, String status) {
        String t = require(title, "标题不能为空");
        String c = content == null ? "" : content;
        String st = status == null ? "draft" : status;
        String summary = c.length() > 120 ? c.substring(0, 120) : c;

        if (id == null) {
            Article a = new Article();
            a.setUserId(KanbanCurrentUser.getRequiredUserId());
            a.setTitle(t);
            a.setContent(c);
            a.setCategory(category);
            a.setTags(tags);
            a.setStatus(st);
            a.setSummary(summary);
            articleMapper.insert(a);
            return a.getId();
        }
        Article a = getOwned(id);
        a.setTitle(t);
        a.setContent(c);
        a.setCategory(category);
        a.setTags(tags);
        a.setStatus(st);
        a.setSummary(summary);
        articleMapper.updateById(a);
        return a.getId();
    }

    /** 文章详情。 */
    public Article detail(Long id) {
        return getOwned(id);
    }

    /** 删除文章。 */
    public void delete(Long id) {
        getOwned(id);
        articleMapper.deleteById(id);
    }

    private Article getOwned(Long id) {
        Article a = articleMapper.selectById(id);
        if (a == null || !a.getUserId().equals(KanbanCurrentUser.getRequiredUserId())) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "文章不存在：" + id);
        }
        return a;
    }

    private String require(String value, String msg) {
        if (value == null || value.isBlank()) {
            throw new ServiceException(ResultCode.BAD_REQUEST, msg);
        }
        return value.trim();
    }
}
