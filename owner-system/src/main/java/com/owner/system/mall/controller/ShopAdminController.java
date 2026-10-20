package com.owner.system.mall.controller;

import com.owner.common.core.domain.R;
import com.owner.system.mall.domain.dto.MemberCreateDTO;
import com.owner.system.mall.domain.dto.MemberUpdateDTO;
import com.owner.system.mall.domain.dto.ShopUpdateDTO;
import com.owner.system.mall.domain.vo.ShopDashboardVO;
import com.owner.system.mall.domain.vo.ShopMemberVO;
import com.owner.system.mall.domain.vo.ShopVO;
import com.owner.system.mall.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 店铺后台：店铺资料、成员、工作台。
 * <p>资料与成员仅店主可改，工作台店主与店员都可看。</p>
 */
@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
@Tag(name = "店铺后台-店铺", description = "店铺资料、成员、工作台")
public class ShopAdminController {

    private final ShopService shopService;

    @Operation(summary = "店铺工作台")
    @GetMapping("/{shopId}/dashboard")
    public R<ShopDashboardVO> dashboard(@PathVariable Long shopId) {
        return R.ok(shopService.dashboard(shopId));
    }

    @Operation(summary = "店铺资料")
    @GetMapping("/{shopId}/profile")
    public R<ShopVO> profile(@PathVariable Long shopId) {
        return R.ok(shopService.shopProfile(shopId));
    }

    @Operation(summary = "修改店铺资料", description = "仅店主")
    @PutMapping("/{shopId}/profile")
    public R<ShopVO> updateProfile(@PathVariable Long shopId,
                                   @Valid @RequestBody ShopUpdateDTO dto) {
        return R.ok(shopService.updateShopProfile(shopId, dto));
    }

    @Operation(summary = "成员列表", description = "只显示本店；koko 不出现在名单里")
    @GetMapping("/{shopId}/members")
    public R<List<ShopMemberVO>> members(@PathVariable Long shopId) {
        return R.ok(shopService.listMembers(shopId));
    }

    @Operation(summary = "添加成员", description = "仅店主；指定手机号、初始密码和角色")
    @PostMapping("/{shopId}/members")
    public R<Long> addMember(@PathVariable Long shopId,
                             @Valid @RequestBody MemberCreateDTO dto) {
        return R.ok(shopService.addMember(shopId, dto));
    }

    @Operation(summary = "变更成员资料 / 角色 / 启停",
            description = "仅店主；可改昵称、手机号（登录账号同步）、重置密码，一家店至少保留一名店主")
    @PutMapping("/{shopId}/members/{memberId}")
    public R<Void> updateMember(@PathVariable Long shopId,
                                @PathVariable Long memberId,
                                @Valid @RequestBody MemberUpdateDTO dto) {
        shopService.updateMember(shopId, memberId, dto);
        return R.ok();
    }
}
