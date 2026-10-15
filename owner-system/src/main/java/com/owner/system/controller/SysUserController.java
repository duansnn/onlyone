package com.owner.system.controller;

import com.owner.common.core.domain.PageResult;
import com.owner.common.core.domain.R;
import com.owner.common.core.enums.ResultCode;
import com.owner.system.domain.dto.SysUserCreateDTO;
import com.owner.system.domain.dto.SysUserQuery;
import com.owner.system.domain.dto.SysUserUpdateDTO;
import com.owner.system.domain.vo.SysUserVO;
import com.owner.system.service.ISysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 门户用户接口。
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/portal/users")
@RequiredArgsConstructor
@Tag(name = "门户用户", description = "门户用户的增删改查示例接口")
public class SysUserController {

    private final ISysUserService sysUserService;

    @Operation(summary = "分页查询门户用户")
    @GetMapping
    public R<PageResult<SysUserVO>> page(SysUserQuery query) {
        return R.ok(sysUserService.pageUsers(query));
    }

    @Operation(summary = "按 ID 查询门户用户", description = "结果会写入 Redis 缓存（Spring Cache REDIS）")
    @GetMapping("/{id}")
    public R<SysUserVO> detail(@Parameter(description = "用户ID") @PathVariable Long id) {
        SysUserVO user = sysUserService.getUserById(id);
        if (user == null) {
            return R.fail(ResultCode.DATA_NOT_FOUND, "用户不存在或已删除：" + id);
        }
        return R.ok(user);
    }

    @Operation(summary = "新增门户用户")
    @PostMapping
    public R<Long> create(@Valid @RequestBody SysUserCreateDTO dto) {
        return R.ok(sysUserService.createUser(dto));
    }

    @Operation(summary = "修改门户用户", description = "基于 @Version 乐观锁")
    @PutMapping
    public R<Void> update(@Valid @RequestBody SysUserUpdateDTO dto) {
        sysUserService.updateUser(dto);
        return R.ok();
    }

    @Operation(summary = "删除门户用户", description = "逻辑删除")
    @DeleteMapping("/{id}")
    public R<Void> remove(@Parameter(description = "用户ID") @PathVariable Long id) {
        sysUserService.removeUser(id);
        return R.ok();
    }

    @Operation(summary = "向所有 WebSocket 客户端推送该用户的欢迎消息")
    @PostMapping("/{id}/welcome")
    public R<Void> pushWelcome(@Parameter(description = "用户ID") @PathVariable Long id) {
        sysUserService.pushWelcome(id);
        return R.ok();
    }
}
