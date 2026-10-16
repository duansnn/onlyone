package com.owner.system.mall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.common.core.utils.StringUtils;
import com.owner.system.domain.entity.SysUser;
import com.owner.system.mall.domain.dto.ChangeMobileDTO;
import com.owner.system.mall.domain.dto.ChangePasswordDTO;
import com.owner.system.mall.domain.dto.LoginDTO;
import com.owner.system.mall.domain.dto.RegisterDTO;
import com.owner.system.mall.domain.dto.UpdateProfileDTO;
import com.owner.system.mall.domain.entity.Shop;
import com.owner.system.mall.domain.entity.ShopMember;
import com.owner.system.mall.domain.enums.ShopRole;
import com.owner.system.mall.domain.vo.AuthUserVO;
import com.owner.system.mall.domain.vo.LoginVO;
import com.owner.system.mall.domain.vo.MobileCodeVO;
import com.owner.system.mall.domain.vo.ShopBriefVO;
import com.owner.system.mall.mapper.ShopMapper;
import com.owner.system.mall.mapper.ShopMemberMapper;
import com.owner.system.mall.support.JwtTokenService;
import com.owner.system.mall.support.LoginAttemptService;
import com.owner.system.mall.support.MallCurrentUser;
import com.owner.system.mall.support.PasswordService;
import com.owner.system.mall.support.TokenBlacklistService;
import com.owner.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 认证服务：注册、登录、退出、改资料、改密码、改手机号。
 *
 * <p>要点：一个登录入口；密码错误与账号不存在提示同一句；
 * 连续失败 5 次锁 15 分钟；退出登录使当前令牌失效。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** 用户状态：正常 / 停用 */
    private static final int STATUS_ENABLED = 0;
    private static final int STATUS_DISABLED = 1;

    /** 成员状态：正常 */
    private static final int MEMBER_ENABLED = 0;

    /** 手机验证码 Redis 前缀与有效期 */
    private static final String MOBILE_CODE_PREFIX = "owner:mall:sms:mobile:";
    private static final Duration MOBILE_CODE_TTL = Duration.ofMinutes(5);

    /** 账号或密码错误的统一提示，不透露账号是否存在 */
    private static final String BAD_CREDENTIALS = "账号或密码不正确";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SysUserMapper sysUserMapper;
    private final ShopMapper shopMapper;
    private final ShopMemberMapper shopMemberMapper;
    private final PasswordService passwordService;
    private final JwtTokenService jwtTokenService;
    private final LoginAttemptService loginAttemptService;
    private final TokenBlacklistService tokenBlacklistService;
    private final StringRedisTemplate redisTemplate;

    @Value("${owner.mall.sms.expose-code:true}")
    private boolean exposeCode;

    // ------------------------------------------------------------------
    // 注册 / 登录 / 退出
    // ------------------------------------------------------------------

    /** 店面注册：注册成功即登录，身份是顾客 */
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(RegisterDTO dto) {
        String mobile = dto.mobile().trim();
        if (findByAccount(mobile) != null) {
            throw new ServiceException(ResultCode.DATA_EXISTS, "该手机号已注册，请直接登录");
        }
        SysUser user = new SysUser();
        user.setUsername(mobile);
        user.setMobile(mobile);
        user.setNickname(StringUtils.isNotBlank(dto.nickname())
                ? dto.nickname().trim()
                : "用户" + mobile.substring(mobile.length() - 4));
        user.setPasswordHash(passwordService.encode(dto.password()));
        user.setPlatformAdmin(0);
        user.setStatus(STATUS_ENABLED);
        user.setVersion(0);
        sysUserMapper.insert(user);

        SysUser saved = sysUserMapper.selectById(user.getId());
        log.info("[MallAuth] 注册成功 userId={}, mobile={}", saved.getId(), mobile);
        return buildLogin(saved);
    }

    /** 统一登录入口 */
    public LoginVO login(LoginDTO dto) {
        String account = dto.account().trim();
        loginAttemptService.assertNotLocked(account);

        SysUser user = findByAccount(account);
        if (user == null || !passwordService.matches(dto.password(), user.getPasswordHash())) {
            loginAttemptService.onFailure(account);
            log.warn("[MallAuth] 登录失败 account={} 原因={}", account,
                    user == null ? "账号不存在" : "密码错误");
            throw new ServiceException(ResultCode.UNAUTHORIZED, BAD_CREDENTIALS);
        }
        if (user.getStatus() != null && user.getStatus() == STATUS_DISABLED) {
            log.warn("[MallAuth] 停用账号尝试登录 userId={}", user.getId());
            throw new ServiceException(ResultCode.FORBIDDEN, "账号已停用，请联系管理员");
        }

        loginAttemptService.onSuccess(account);
        log.info("[MallAuth] 登录成功 userId={}, account={}", user.getId(), account);
        return buildLogin(user);
    }

    /** 退出登录：把当前令牌拉黑至其自然过期 */
    public void logout() {
        MallCurrentUser.Principal principal = MallCurrentUser.getOrNull();
        if (principal == null) {
            return;
        }
        long remaining = principal.expiresAtEpochSecond() - System.currentTimeMillis() / 1000L;
        tokenBlacklistService.revoke(principal.jti(), remaining);
        log.info("[MallAuth] 退出登录 userId={}", principal.userId());
    }

    // ------------------------------------------------------------------
    // 个人资料
    // ------------------------------------------------------------------

    /** 当前登录用户 */
    public AuthUserVO me() {
        SysUser user = requireUser(MallCurrentUser.requireUserId());
        return toAuthUser(user);
    }

    /** 改昵称、头像、邮箱 */
    @Transactional(rollbackFor = Exception.class)
    public AuthUserVO updateProfile(UpdateProfileDTO dto) {
        SysUser user = requireUser(MallCurrentUser.requireUserId());
        if (dto.nickname() != null && !dto.nickname().isBlank()) {
            user.setNickname(dto.nickname().trim());
        }
        if (dto.email() != null) {
            user.setEmail(StringUtils.trimToNull(dto.email()));
        }
        if (dto.avatarUrl() != null) {
            user.setAvatarUrl(StringUtils.trimToNull(dto.avatarUrl()));
        }
        updateUser(user);
        return toAuthUser(sysUserMapper.selectById(user.getId()));
    }

    /** 改密码，必须填当前密码 */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordDTO dto) {
        SysUser user = requireUser(MallCurrentUser.requireUserId());
        if (!passwordService.matches(dto.currentPassword(), user.getPasswordHash())) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "当前密码不正确");
        }
        if (passwordService.matches(dto.newPassword(), user.getPasswordHash())) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "新密码不能与当前密码相同");
        }
        user.setPasswordHash(passwordService.encode(dto.newPassword()));
        updateUser(user);
        log.info("[MallAuth] 修改密码成功 userId={}", user.getId());
    }

    /** 发送手机验证码（未接短信通道时把验证码写入日志，便于本地联调） */
    public MobileCodeVO sendMobileCode(String mobile) {
        if (StringUtils.isBlank(mobile)) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "手机号不能为空");
        }
        String trimmed = mobile.trim();
        if (!trimmed.matches("^1[3-9]\\d{9}$")) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "手机号格式不正确");
        }
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        redisTemplate.opsForValue().set(MOBILE_CODE_PREFIX + trimmed, code, MOBILE_CODE_TTL);
        log.info("[MallAuth] 手机验证码 mobile={}, code={}（未接短信通道，仅记录日志）", trimmed, code);
        return new MobileCodeVO(trimmed, MOBILE_CODE_TTL.toSeconds(), exposeCode ? code : null);
    }

    /** 校验验证码后改手机号 */
    @Transactional(rollbackFor = Exception.class)
    public void changeMobile(ChangeMobileDTO dto) {
        Long userId = MallCurrentUser.requireUserId();
        String key = MOBILE_CODE_PREFIX + dto.mobile();
        String expected = redisTemplate.opsForValue().get(key);
        if (expected == null || !expected.equals(dto.code())) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "验证码不正确或已过期");
        }
        SysUser exists = findByAccount(dto.mobile().trim());
        if (exists != null && !exists.getId().equals(userId)) {
            throw new ServiceException(ResultCode.DATA_EXISTS, "该手机号已被其他账号使用");
        }
        SysUser user = requireUser(userId);
        String oldMobile = user.getMobile();
        user.setMobile(dto.mobile().trim());
        // 店面注册的账号用手机号当登录名，这里同步更新，避免登录名与手机号不一致
        if (oldMobile != null && oldMobile.equals(user.getUsername())) {
            user.setUsername(dto.mobile().trim());
        }
        updateUser(user);
        redisTemplate.delete(key);
        log.info("[MallAuth] 修改手机号成功 userId={}", userId);
    }

    // ------------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------------

    private LoginVO buildLogin(SysUser user) {
        String token = jwtTokenService.createToken(user.getId(), user.getUsername(), isAdmin(user));
        return new LoginVO(token, "Bearer", jwtTokenService.getExpireSeconds(),
                toAuthUser(user), myShops(user.getId()));
    }

    private List<ShopBriefVO> myShops(Long userId) {
        List<ShopMember> members = shopMemberMapper.selectList(Wrappers.<ShopMember>lambdaQuery()
                .eq(ShopMember::getUserId, userId)
                .eq(ShopMember::getStatus, MEMBER_ENABLED));
        if (members.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> shopIds = members.stream().map(ShopMember::getShopId).distinct().toList();
        List<Shop> shops = shopMapper.selectList(Wrappers.<Shop>lambdaQuery().in(Shop::getId, shopIds));
        List<ShopBriefVO> result = new ArrayList<>(members.size());
        for (ShopMember member : members) {
            shops.stream()
                    .filter(shop -> shop.getId().equals(member.getShopId()))
                    .findFirst()
                    .ifPresent(shop -> {
                        ShopRole role = ShopRole.of(member.getRole());
                        result.add(new ShopBriefVO(shop.getId(), shop.getName(), member.getRole(),
                                role == null ? member.getRole() : role.getLabel()));
                    });
        }
        return result;
    }

    private SysUser findByAccount(String account) {
        return sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .and(w -> w.eq(SysUser::getUsername, account).or().eq(SysUser::getMobile, account))
                .last("LIMIT 1"));
    }

    private SysUser requireUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(ResultCode.UNAUTHORIZED, "账号不存在或已删除");
        }
        return user;
    }

    private void updateUser(SysUser user) {
        if (sysUserMapper.updateById(user) == 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "数据已被他人修改，请刷新后重试");
        }
    }

    private boolean isAdmin(SysUser user) {
        return user.getPlatformAdmin() != null && user.getPlatformAdmin() == 1;
    }

    private AuthUserVO toAuthUser(SysUser user) {
        return new AuthUserVO(user.getId(), user.getUsername(), user.getNickname(),
                user.getMobile(), user.getEmail(), user.getAvatarUrl(), isAdmin(user));
    }
}
