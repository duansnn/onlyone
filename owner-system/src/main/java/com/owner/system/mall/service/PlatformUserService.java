package com.owner.system.mall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.common.core.domain.PageQuery;
import com.owner.common.core.domain.PageResult;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.common.core.utils.StringUtils;
import com.owner.system.domain.entity.SysUser;
import com.owner.system.mall.domain.vo.PlatformUserVO;
import com.owner.system.mall.support.MallCurrentUser;
import com.owner.system.mall.support.PasswordService;
import com.owner.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 平台超管的用户管理（PRD §12）。
 *
 * <p>只能查询、停用/启用、重置密码。能不能当某家店的店主，由「店铺成员」决定，
 * 不在这里改——这里不碰 {@code shop_member}。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformUserService {

    private static final int USER_ENABLED = 0;
    private static final int USER_DISABLED = 1;

    private final SysUserMapper sysUserMapper;
    private final PasswordService passwordService;

    /** 用户分页：按账号 / 昵称 / 手机号模糊查，手机号打星 */
    public PageResult<PlatformUserVO> page(String keyword, Integer status, PageQuery query) {
        MallCurrentUser.requirePlatformAdmin();
        Page<SysUser> page = sysUserMapper.selectPage(
                new Page<>(query.getPageNumOrDefault(), query.getPageSizeOrDefault()),
                Wrappers.<SysUser>lambdaQuery()
                        .eq(status != null, SysUser::getStatus, status)
                        .and(StringUtils.isNotBlank(keyword), w -> w
                                .like(SysUser::getUsername, keyword)
                                .or().like(SysUser::getNickname, keyword)
                                .or().like(SysUser::getMobile, keyword))
                        .orderByDesc(SysUser::getCreateTime)
                        .orderByDesc(SysUser::getId));

        List<PlatformUserVO> rows = new ArrayList<>(page.getRecords().size());
        for (SysUser user : page.getRecords()) {
            rows.add(toVo(user));
        }
        return PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 停用 / 启用账号。停用后不能登录；不能停用自己。 */
    @Transactional(rollbackFor = Exception.class)
    public void setStatus(Long userId, int status) {
        Long operatorId = MallCurrentUser.requireUserId();
        MallCurrentUser.requirePlatformAdmin();
        if (status != USER_ENABLED && status != USER_DISABLED) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "状态只能是 0 或 1");
        }
        if (status == USER_DISABLED && operatorId.equals(userId)) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "不能停用自己当前的账号");
        }
        SysUser user = requireUser(userId);
        if (user.getStatus() != null && user.getStatus() == status) {
            return;
        }
        user.setStatus(status);
        if (sysUserMapper.updateById(user) == 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "数据已被他人修改，请刷新后重试");
        }
        log.info("[PlatformUser] userId={} 状态改为 {}", userId, status == USER_DISABLED ? "停用" : "正常");
    }

    /** 重置密码。只写新的 BCrypt，不返回明文。 */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, String newPassword) {
        MallCurrentUser.requirePlatformAdmin();
        SysUser user = requireUser(userId);
        user.setPasswordHash(passwordService.encode(newPassword));
        if (sysUserMapper.updateById(user) == 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "数据已被他人修改，请刷新后重试");
        }
        log.info("[PlatformUser] 已重置 userId={} 的密码", userId);
    }

    private SysUser requireUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "用户不存在：" + userId);
        }
        return user;
    }

    private PlatformUserVO toVo(SysUser user) {
        int status = user.getStatus() == null ? USER_ENABLED : user.getStatus();
        return new PlatformUserVO(user.getId(), user.getUsername(), user.getNickname(),
                StringUtils.maskMobile(user.getMobile()), user.getEmail(),
                user.getPlatformAdmin() != null && user.getPlatformAdmin() == 1,
                status, status == USER_DISABLED ? "停用" : "正常",
                user.getPasswordHash() != null && !user.getPasswordHash().isBlank(),
                user.getCreateTime());
    }
}
