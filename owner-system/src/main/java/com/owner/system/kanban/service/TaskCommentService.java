package com.owner.system.kanban.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.system.kanban.domain.entity.TaskComment;
import com.owner.system.kanban.mapper.TaskCommentMapper;
import com.owner.system.kanban.support.KanbanCurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 任务评论服务：评论归属校验（仅本人项目的任务可评论/查看）。
 */
@Service
@RequiredArgsConstructor
public class TaskCommentService {

    private final TaskCommentMapper commentMapper;
    private final TaskService taskService;

    /** 任务评论列表，按时间正序。 */
    public List<TaskComment> list(Long taskId) {
        taskService.getOwnedTask(taskId);
        return commentMapper.selectList(
                Wrappers.<TaskComment>lambdaQuery()
                        .eq(TaskComment::getTaskId, taskId)
                        .orderByAsc(TaskComment::getCreateTime));
    }

    /** 发表评论，返回评论 id。 */
    public Long add(Long taskId, String content) {
        if (content == null || content.isBlank()) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "评论内容不能为空");
        }
        taskService.getOwnedTask(taskId);
        TaskComment comment = new TaskComment();
        comment.setTaskId(taskId);
        comment.setAuthor(KanbanCurrentUser.getRequiredUsername());
        comment.setContent(content.trim());
        commentMapper.insert(comment);
        return comment.getId();
    }

    /** 删除评论（仅评论所在任务的拥有者可删）。 */
    public void delete(Long commentId) {
        TaskComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "评论不存在：" + commentId);
        }
        taskService.getOwnedTask(comment.getTaskId());
        commentMapper.deleteById(commentId);
    }
}
