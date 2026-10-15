package com.owner.system.kanban.controller;

import com.owner.common.core.domain.R;
import com.owner.system.kanban.domain.dto.LoginDTO;
import com.owner.system.kanban.domain.dto.RegisterDTO;
import com.owner.system.kanban.domain.vo.LoginVO;
import com.owner.system.kanban.service.KanbanAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 看板账号接口（PRD §6 用户接口）。
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "看板-账号", description = "任务看板注册 / 登录（bcrypt + JWT）")
public class KanbanAuthController {

    private final KanbanAuthService kanbanAuthService;

    @Operation(summary = "用户注册", description = "用户名唯一，密码 BCrypt 加密存储")
    @PostMapping("/register")
    public R<Long> register(@Valid @RequestBody RegisterDTO dto) {
        return R.ok("注册成功", kanbanAuthService.register(dto));
    }

    @Operation(summary = "用户登录", description = "校验通过后返回 JWT，有效期默认 72 小时")
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(kanbanAuthService.login(dto));
    }
}
