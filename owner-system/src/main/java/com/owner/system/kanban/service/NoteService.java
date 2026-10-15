package com.owner.system.kanban.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.system.kanban.domain.entity.Note;
import com.owner.system.kanban.mapper.NoteMapper;
import com.owner.system.kanban.support.KanbanCurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识库笔记服务（个人笔记，数据按 userId 隔离）。
 */
@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteMapper noteMapper;

    /** 当前用户笔记列表，支持分类/关键词筛选，置顶优先、按更新时间倒序。 */
    public List<Note> list(String keyword, String category) {
        Long userId = KanbanCurrentUser.getRequiredUserId();
        return noteMapper.selectList(Wrappers.<Note>lambdaQuery()
                .eq(Note::getUserId, userId)
                .like(keyword != null && !keyword.isBlank(), Note::getTitle, keyword)
                .eq(category != null && !category.isBlank(), Note::getCategory, category)
                .orderByDesc(Note::getPinned)
                .orderByDesc(Note::getUpdateTime));
    }

    /** 新建笔记，返回 id。 */
    public Long create(String title, String content, String category) {
        Note note = new Note();
        note.setUserId(KanbanCurrentUser.getRequiredUserId());
        note.setTitle(require(title, "标题不能为空"));
        note.setContent(content == null ? "" : content);
        note.setCategory(category);
        note.setPinned(0);
        noteMapper.insert(note);
        return note.getId();
    }

    /** 更新笔记（标题/正文/分类/置顶）。 */
    public void update(Long id, String title, String content, String category, Integer pinned) {
        Note note = getOwned(id);
        if (title != null) note.setTitle(title);
        if (content != null) note.setContent(content);
        if (category != null) note.setCategory(category);
        if (pinned != null) note.setPinned(pinned);
        noteMapper.updateById(note);
    }

    /** 删除笔记。 */
    public void delete(Long id) {
        getOwned(id);
        noteMapper.deleteById(id);
    }

    /** 取当前用户名下笔记，不存在或非本人抛异常。 */
    public Note getOwned(Long id) {
        Note note = noteMapper.selectById(id);
        if (note == null || !note.getUserId().equals(KanbanCurrentUser.getRequiredUserId())) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "笔记不存在：" + id);
        }
        return note;
    }

    private String require(String value, String msg) {
        if (value == null || value.isBlank()) {
            throw new ServiceException(ResultCode.BAD_REQUEST, msg);
        }
        return value.trim();
    }
}
