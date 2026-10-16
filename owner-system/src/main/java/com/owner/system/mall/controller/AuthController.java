package com.owner.system.mall.controller;

import com.owner.common.core.domain.R;
import com.owner.system.mall.domain.dto.ChangeMobileDTO;
import com.owner.system.mall.domain.dto.ChangePasswordDTO;
import com.owner.system.mall.domain.dto.LoginDTO;
import com.owner.system.mall.domain.dto.RegisterDTO;
import com.owner.system.mall.domain.dto.UpdateProfileDTO;
import com.owner.system.mall.domain.vo.AuthUserVO;
import com.owner.system.mall.domain.vo.LoginVO;
import com.owner.system.mall.domain.vo.MobileCodeVO;
import com.owner.system.mall.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：注册、登录、退出、当前用户、改资料、改密码、改手机号。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证", description = "注册、登录、退出、个人资料")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "注册", description = "店面注册，账号用手机号，注册成功即登录，身份是顾客")
    @PostMapping("/register")
    public R<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        return R.ok(authService.register(dto));
    }

    @Operation(summary = "登录", description = "账号可以是登录名或手机号；连续失败 5 次锁定 15 分钟")
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    @Operation(summary = "退出登录", description = "当前令牌立即失效")
    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public R<AuthUserVO> me() {
        return R.ok(authService.me());
    }

    @Operation(summary = "修改个人资料", description = "可改昵称、头像、邮箱")
    @PutMapping("/profile")
    public R<AuthUserVO> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        return R.ok(authService.updateProfile(dto));
    }

    @Operation(summary = "修改密码", description = "必须填当前密码")
    @PutMapping("/password")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(dto);
        return R.ok();
    }

    @Operation(summary = "发送手机验证码", description = "改手机号前先调用；未接短信通道时验证码只记录在日志")
    @PostMapping("/mobile/code")
    public R<MobileCodeVO> sendMobileCode(
            @Parameter(description = "新手机号") @RequestParam String mobile) {
        return R.ok(authService.sendMobileCode(mobile));
    }

    @Operation(summary = "修改手机号", description = "需带验证码")
    @PutMapping("/mobile")
    public R<Void> changeMobile(@Valid @RequestBody ChangeMobileDTO dto) {
        authService.changeMobile(dto);
        return R.ok();
    }
}
