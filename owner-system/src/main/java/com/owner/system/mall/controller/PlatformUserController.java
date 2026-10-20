package com.owner.system.mall.controller;

import com.owner.common.core.domain.PageQuery;
import com.owner.common.core.domain.PageResult;
import com.owner.common.core.domain.R;
import com.owner.system.mall.domain.dto.ResetPasswordDTO;
import com.owner.system.mall.domain.vo.CustomerDetailVO;
import com.owner.system.mall.domain.vo.PlatformUserVO;
import com.owner.system.mall.service.PlatformUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台-顾客管理：查询、详情、停用、重置密码。只有店主（koko）能进。
 * <p>顾客是自己在店面注册的，这里不建账号、也不改顾客资料。</p>
 */
@RestController
@RequestMapping("/api/platform/users")
@RequiredArgsConstructor
@Tag(name = "后台-顾客管理", description = "顾客查询、详情、停用启用、重置密码")
public class PlatformUserController {

    private final PlatformUserService platformUserService;

    @Operation(summary = "顾客分页", description = "按账号/昵称/手机号模糊查；带订单数、累计消费、最后下单时间")
    @GetMapping
    public R<PageResult<PlatformUserVO>> page(
            @Parameter(description = "账号 / 昵称 / 手机号") @RequestParam(required = false) String keyword,
            @Parameter(description = "0 正常，1 停用") @RequestParam(required = false) Integer status,
            PageQuery query) {
        return R.ok(platformUserService.page(keyword, status, query));
    }

    @Operation(summary = "顾客详情", description = "基础档案 + 消费统计 + 收货地址 + 最近 20 条订单")
    @GetMapping("/{userId}")
    public R<CustomerDetailVO> detail(@PathVariable Long userId) {
        return R.ok(platformUserService.detail(userId));
    }

    @Operation(summary = "停用或启用账号", description = "0 正常，1 停用；停用后不能登录，也不能停用自己")
    @PutMapping("/{userId}/status")
    public R<Void> setStatus(@PathVariable Long userId,
                             @Parameter(description = "0 正常，1 停用") @RequestParam int status) {
        platformUserService.setStatus(userId, status);
        return R.ok();
    }

    @Operation(summary = "重置用户密码", description = "只写入新的 BCrypt，不返回明文")
    @PutMapping("/{userId}/password")
    public R<Void> resetPassword(@PathVariable Long userId,
                                 @Valid @RequestBody ResetPasswordDTO dto) {
        platformUserService.resetPassword(userId, dto.password());
        return R.ok();
    }
}
