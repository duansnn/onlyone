package com.owner.system.kanban.controller;

import com.owner.common.core.domain.R;
import com.owner.system.kanban.domain.entity.Note;
import com.owner.system.kanban.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 知识库笔记接口。
 */
@RestController
@RequestMapping("/api/note")
@RequiredArgsConstructor
@Tag(name = "知识库-笔记", description = "个人笔记的增删改查（数据隔离）")
public class NoteController {

    private final NoteService noteService;

    @Operation(summary = "笔记列表")
    @GetMapping
    public R<List<Note>> list(@RequestParam(required = false) String keyword,
                              @RequestParam(required = false) String category) {
        return R.ok(noteService.list(keyword, category));
    }

    @Operation(summary = "新建笔记")
    @PostMapping
    public R<Long> create(@RequestBody Map<String, Object> body) {
        return R.ok(noteService.create(str(body.get("title")), str(body.get("content")), str(body.get("category"))));
    }

    @Operation(summary = "更新笔记")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        noteService.update(id, str(body.get("title")), str(body.get("content")), str(body.get("category")),
                body.get("pinned") == null ? null : ((Number) body.get("pinned")).intValue());
        return R.ok();
    }

    @Operation(summary = "删除笔记")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        noteService.delete(id);
        return R.ok();
    }

    private String str(Object o) {
        return o == null ? null : o.toString();
    }
}
