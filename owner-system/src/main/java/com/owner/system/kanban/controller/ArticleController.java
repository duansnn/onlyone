package com.owner.system.kanban.controller;

import com.owner.common.core.domain.R;
import com.owner.system.kanban.domain.entity.Article;
import com.owner.system.kanban.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 博客文章接口。
 */
@RestController
@RequestMapping("/api/article")
@RequiredArgsConstructor
@Tag(name = "博客-文章", description = "文章编写与发布（数据隔离）")
public class ArticleController {

    private final ArticleService articleService;

    @Operation(summary = "文章列表")
    @GetMapping
    public R<List<Article>> list(@RequestParam(required = false) String status,
                                 @RequestParam(required = false) String category,
                                 @RequestParam(required = false) String keyword) {
        return R.ok(articleService.list(status, category, keyword));
    }

    @Operation(summary = "新建/保存文章")
    @PostMapping
    public R<Long> save(@RequestBody Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(body.get("id").toString());
        return R.ok(articleService.save(id, str(body.get("title")), str(body.get("content")),
                str(body.get("category")), str(body.get("tags")), str(body.get("status"))));
    }

    @Operation(summary = "文章详情")
    @GetMapping("/{id}")
    public R<Article> detail(@PathVariable Long id) {
        return R.ok(articleService.detail(id));
    }

    @Operation(summary = "删除文章")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return R.ok();
    }

    private String str(Object o) {
        return o == null ? null : o.toString();
    }
}
