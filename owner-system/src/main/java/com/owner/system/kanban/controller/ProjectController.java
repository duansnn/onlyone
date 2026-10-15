package com.owner.system.kanban.controller;

import com.owner.common.core.domain.R;
import com.owner.system.kanban.domain.dto.ProjectSaveDTO;
import com.owner.system.kanban.domain.vo.ProjectVO;
import com.owner.system.kanban.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 看板项目接口（PRD §6 项目接口）。
 */
@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
@Tag(name = "看板-项目", description = "项目管理（数据隔离：仅本人项目）")
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "获取当前用户全部项目", description = "附各项目任务数")
    @GetMapping("/list")
    public R<List<ProjectVO>> list() {
        return R.ok(projectService.list());
    }

    @Operation(summary = "创建新项目")
    @PostMapping
    public R<Long> create(@Valid @RequestBody ProjectSaveDTO dto) {
        return R.ok(projectService.create(dto));
    }

    @Operation(summary = "修改项目名称/描述")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "项目ID") @PathVariable Long id,
                          @Valid @RequestBody ProjectSaveDTO dto) {
        projectService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除项目", description = "级联删除项目下全部任务")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "项目ID") @PathVariable Long id) {
        projectService.delete(id);
        return R.ok();
    }
}
