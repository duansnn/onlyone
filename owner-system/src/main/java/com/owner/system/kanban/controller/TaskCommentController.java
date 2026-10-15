package com.owner.system.kanban.controller;

import com.owner.common.core.domain.R;
import com.owner.system.kanban.domain.entity.TaskComment;
import com.owner.system.kanban.service.TaskCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 任务评论接口（看板可选迭代功能）。
 */
@RestController
@RequestMapping("/api/task")
@RequiredArgsConstructor
@Tag(name = "看板-任务评论", description = "任务评论的查看与发表（数据隔离：仅本人项目任务）")
public class TaskCommentController {

    private final TaskCommentService commentService;

    @Operation(summary = "获取任务评论列表")
    @GetMapping("/{id}/comment")
    public R<List<TaskComment>> list(@Parameter(description = "任务ID") @PathVariable Long id) {
        return R.ok(commentService.list(id));
    }

    @Operation(summary = "发表评论")
    @PostMapping("/{id}/comment")
    public R<Long> add(@Parameter(description = "任务ID") @PathVariable Long id,
                       @RequestBody Map<String, String> body) {
        return R.ok(commentService.add(id, body.get("content")));
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/comment/{commentId}")
    public R<Void> delete(@Parameter(description = "评论ID") @PathVariable Long commentId) {
        commentService.delete(commentId);
        return R.ok();
    }
}
