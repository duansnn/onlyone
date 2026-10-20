package com.owner.system.mall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.common.core.domain.PageQuery;
import com.owner.common.core.domain.PageResult;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.common.core.utils.StringUtils;
import com.owner.system.domain.entity.SysUser;
import com.owner.system.mall.domain.dto.MemberCreateDTO;
import com.owner.system.mall.domain.dto.MemberUpdateDTO;
import com.owner.system.mall.domain.dto.ShopCreateDTO;
import com.owner.system.mall.domain.dto.ShopUpdateDTO;
import com.owner.system.mall.domain.entity.OrderInfo;
import com.owner.system.mall.domain.entity.Refund;
import com.owner.system.mall.domain.entity.Shop;
import com.owner.system.mall.domain.entity.ShopMember;
import com.owner.system.mall.domain.enums.OrderStatus;
import com.owner.system.mall.domain.enums.RefundStatus;
import com.owner.system.mall.domain.enums.ShopRole;
import com.owner.system.mall.domain.vo.PlatformOverviewVO;
import com.owner.system.mall.domain.vo.PlatformShopVO;
import com.owner.system.mall.domain.vo.ShopDashboardVO;
import com.owner.system.mall.domain.vo.ShopMemberVO;
import com.owner.system.mall.domain.vo.ShopVO;
import com.owner.system.mall.mapper.OrderInfoMapper;
import com.owner.system.mall.mapper.ProductMapper;
import com.owner.system.mall.mapper.RefundMapper;
import com.owner.system.mall.mapper.ShopMapper;
import com.owner.system.mall.mapper.ShopMemberMapper;
import com.owner.system.mall.support.MallCurrentUser;
import com.owner.system.mall.support.PasswordService;
import com.owner.system.mall.support.ShopGuard;
import com.owner.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 店铺服务：平台建店 / 停店 / 概览，店主改资料与管成员，店铺工作台。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShopService {

    private static final int SHOP_ACTIVE = 0;
    private static final int SHOP_DISABLED = 1;
    private static final int MEMBER_ENABLED = 0;
    private static final int MEMBER_DISABLED = 1;

    private final ShopMapper shopMapper;
    private final ShopMemberMapper shopMemberMapper;
    private final SysUserMapper sysUserMapper;
    private final ProductMapper productMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final RefundMapper refundMapper;
    private final PasswordService passwordService;
    private final ShopGuard shopGuard;

    // ------------------------------------------------------------------
    // 平台侧
    // ------------------------------------------------------------------

    /** 平台工作台：店铺数、全部待发货数、今日支付金额 */
    public PlatformOverviewVO platformOverview() {
        MallCurrentUser.requirePlatformAdmin();
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        long shopCount = shopMapper.selectCount(null);
        long activeShopCount = shopMapper.selectCount(Wrappers.<Shop>lambdaQuery()
                .eq(Shop::getStatus, SHOP_ACTIVE));
        long pendingShipment = orderInfoMapper.selectCount(Wrappers.<OrderInfo>lambdaQuery()
                .eq(OrderInfo::getStatus, OrderStatus.PENDING_SHIPMENT.name()));
        long pendingRefund = countPendingRefund(null);
        long todayOrders = orderInfoMapper.selectCount(Wrappers.<OrderInfo>lambdaQuery()
                .ge(OrderInfo::getCreateTime, start)
                .lt(OrderInfo::getCreateTime, end));
        long todayPaid = orZero(orderInfoMapper.sumPaidAmountAll(start, end));

        return new PlatformOverviewVO(shopCount, activeShopCount, pendingShipment,
                pendingRefund, todayOrders, todayPaid);
    }

    /** 平台店铺列表（只读统计） */
    public PageResult<PlatformShopVO> platformShops(String keyword, PageQuery query) {
        MallCurrentUser.requirePlatformAdmin();
        Page<Shop> page = shopMapper.selectPage(
                new Page<>(query.getPageNumOrDefault(), query.getPageSizeOrDefault()),
                Wrappers.<Shop>lambdaQuery()
                        .like(StringUtils.isNotBlank(keyword), Shop::getName, keyword)
                        .orderByDesc(Shop::getCreateTime));

        LocalDateTime start = LocalDate.now().atStartOfDay();
        Map<Long, Long> onSale = toLongMap(productMapper.countOnSaleGroupByShop(), "cnt");
        Map<Long, Long> pending = toLongMap(orderInfoMapper.countPendingShipmentGroupByShop(), "cnt");
        Map<Long, Long> todayPaid = toLongMap(
                orderInfoMapper.sumPaidAmountGroupByShop(start, start.plusDays(1)), "amount");

        List<PlatformShopVO> rows = new ArrayList<>(page.getRecords().size());
        for (Shop shop : page.getRecords()) {
            rows.add(new PlatformShopVO(
                    shop.getId(), shop.getName(), shop.getContactName(),
                    StringUtils.maskMobile(shop.getContactMobile()),
                    shop.getStatus(), statusLabel(shop.getStatus()),
                    onSale.getOrDefault(shop.getId(), 0L),
                    pending.getOrDefault(shop.getId(), 0L),
                    todayPaid.getOrDefault(shop.getId(), 0L)));
        }
        return PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 平台新建店铺，并指定一名店主 */
    @Transactional(rollbackFor = Exception.class)
    public Long createShop(ShopCreateDTO dto) {
        MallCurrentUser.requirePlatformAdmin();
        SysUser owner = resolveOwner(dto);

        Shop shop = new Shop();
        shop.setName(dto.name().trim());
        shop.setIntro(StringUtils.trimToNull(dto.intro()));
        shop.setContactName(StringUtils.trimToNull(dto.contactName()));
        shop.setContactMobile(StringUtils.trimToNull(dto.contactMobile()));
        shop.setShipAddress(StringUtils.trimToNull(dto.shipAddress()));
        shop.setStatus(SHOP_ACTIVE);
        shop.setVersion(0);
        shopMapper.insert(shop);

        ShopMember member = new ShopMember();
        member.setShopId(shop.getId());
        member.setUserId(owner.getId());
        member.setRole(ShopRole.OWNER.name());
        member.setStatus(MEMBER_ENABLED);
        member.setVersion(0);
        shopMemberMapper.insert(member);

        log.info("[Shop] 平台建店成功 shopId={}, name={}, ownerUserId={}",
                shop.getId(), shop.getName(), owner.getId());
        return shop.getId();
    }

    /** 平台停用 / 启用店铺。停用后店面不可访问、不能下新单，未完成订单仍可发货与售后。 */
    @Transactional(rollbackFor = Exception.class)
    public void setShopStatus(Long shopId, int status) {
        MallCurrentUser.requirePlatformAdmin();
        if (status != SHOP_ACTIVE && status != SHOP_DISABLED) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "状态只能是 0 或 1");
        }
        Shop shop = shopGuard.loadShop(shopId);
        if (shop.getStatus() != null && shop.getStatus() == status) {
            return;
        }
        shop.setStatus(status);
        if (shopMapper.updateById(shop) == 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "数据已被他人修改，请刷新后重试");
        }
        log.info("[Shop] shopId={} 状态改为 {}", shopId, statusLabel(status));
    }

    // ------------------------------------------------------------------
    // 店铺侧
    // ------------------------------------------------------------------

    /** 店铺资料（成员可看） */
    public ShopVO shopProfile(Long shopId) {
        shopGuard.requireMember(shopId);
        return toShopVo(shopGuard.loadShop(shopId));
    }

    /** 店主改本店资料 */
    @Transactional(rollbackFor = Exception.class)
    public ShopVO updateShopProfile(Long shopId, ShopUpdateDTO dto) {
        shopGuard.requireOwner(shopId);
        Shop shop = shopGuard.loadShop(shopId);
        shop.setName(dto.name().trim());
        shop.setIntro(StringUtils.trimToNull(dto.intro()));
        shop.setNotice(StringUtils.trimToNull(dto.notice()));
        shop.setContactName(StringUtils.trimToNull(dto.contactName()));
        shop.setContactMobile(StringUtils.trimToNull(dto.contactMobile()));
        shop.setShipAddress(StringUtils.trimToNull(dto.shipAddress()));
        shop.setBannerUrl(StringUtils.trimToNull(dto.bannerUrl()));
        shop.setBusinessHours(StringUtils.trimToNull(dto.businessHours()));
        shop.setPayNote(StringUtils.trimToNull(dto.payNote()));
        if (dto.version() != null) {
            shop.setVersion(dto.version());
        }
        if (shopMapper.updateById(shop) == 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "数据已被他人修改，请刷新后重试");
        }
        log.info("[Shop] 店主更新店铺资料 shopId={}", shopId);
        return toShopVo(shopMapper.selectById(shopId));
    }

    /** 本店成员名单（koko 不属于任何店，不会出现在这里） */
    public List<ShopMemberVO> listMembers(Long shopId) {
        shopGuard.requireMember(shopId);
        List<ShopMember> members = shopMemberMapper.selectList(Wrappers.<ShopMember>lambdaQuery()
                .eq(ShopMember::getShopId, shopId)
                .orderByAsc(ShopMember::getRole)
                .orderByAsc(ShopMember::getCreateTime));
        if (members.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> userIds = members.stream().map(ShopMember::getUserId).distinct().toList();
        Map<Long, SysUser> userMap = new HashMap<>(userIds.size());
        for (SysUser user : sysUserMapper.selectList(Wrappers.<SysUser>lambdaQuery().in(SysUser::getId, userIds))) {
            userMap.put(user.getId(), user);
        }
        List<ShopMemberVO> rows = new ArrayList<>(members.size());
        for (ShopMember member : members) {
            SysUser user = userMap.get(member.getUserId());
            ShopRole role = ShopRole.of(member.getRole());
            rows.add(new ShopMemberVO(
                    member.getId(), member.getUserId(),
                    user == null ? null : user.getUsername(),
                    user == null ? null : user.getNickname(),
                    user == null ? null : user.getMobile(),
                    member.getRole(), role == null ? member.getRole() : role.getLabel(),
                    member.getStatus(), memberStatusLabel(member.getStatus()),
                    member.getCreateTime()));
        }
        return rows;
    }

    /** 店主添加店员（或把已有用户加进来） */
    @Transactional(rollbackFor = Exception.class)
    public Long addMember(Long shopId, MemberCreateDTO dto) {
        shopGuard.requireOwner(shopId);
        shopGuard.loadShop(shopId);

        String mobile = dto.mobile().trim();
        SysUser user = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getMobile, mobile)
                .last("LIMIT 1"));
        if (user == null) {
            if (StringUtils.isBlank(dto.password())) {
                throw new ServiceException(ResultCode.BAD_REQUEST, "该手机号尚未注册，请提供初始密码");
            }
            user = new SysUser();
            user.setUsername(mobile);
            user.setMobile(mobile);
            user.setNickname(StringUtils.isNotBlank(dto.nickname())
                    ? dto.nickname().trim()
                    : "店员" + mobile.substring(mobile.length() - 4));
            user.setPasswordHash(passwordService.encode(dto.password()));
            user.setPlatformAdmin(0);
            user.setStatus(MEMBER_ENABLED);
            user.setVersion(0);
            sysUserMapper.insert(user);
            user = sysUserMapper.selectById(user.getId());
            log.info("[Shop] 添加店员时新建用户 userId={}, mobile={}", user.getId(), mobile);
        } else if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            if (StringUtils.isBlank(dto.password())) {
                throw new ServiceException(ResultCode.BAD_REQUEST, "该用户尚未设置密码，请提供初始密码");
            }
            user.setPasswordHash(passwordService.encode(dto.password()));
            sysUserMapper.updateById(user);
        }

        ShopMember existing = shopGuard.memberOf(shopId, user.getId());
        if (existing != null) {
            if (existing.getStatus() != null && existing.getStatus() == MEMBER_DISABLED) {
                existing.setStatus(MEMBER_ENABLED);
                existing.setRole(dto.role());
                shopMemberMapper.updateById(existing);
                log.info("[Shop] 重新启用成员 memberId={}, role={}", existing.getId(), dto.role());
                return existing.getId();
            }
            throw new ServiceException(ResultCode.DATA_EXISTS, "该用户已是本店成员");
        }

        ShopMember member = new ShopMember();
        member.setShopId(shopId);
        member.setUserId(user.getId());
        member.setRole(dto.role());
        member.setStatus(MEMBER_ENABLED);
        member.setVersion(0);
        shopMemberMapper.insert(member);
        log.info("[Shop] 添加成员 shopId={}, userId={}, role={}", shopId, user.getId(), dto.role());
        return member.getId();
    }

    /** 店主变更角色 / 启用停用 / 改昵称手机号密码。一家店至少保留一名店主。 */
    @Transactional(rollbackFor = Exception.class)
    public void updateMember(Long shopId, Long memberId, MemberUpdateDTO dto) {
        shopGuard.requireOwner(shopId);
        ShopMember member = shopMemberMapper.selectById(memberId);
        if (member == null || !member.getShopId().equals(shopId)) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "成员不存在：" + memberId);
        }

        String newRole = StringUtils.isNotBlank(dto.role()) ? dto.role() : member.getRole();
        Integer newStatus = StringUtils.isNotBlank(dto.status())
                ? Integer.valueOf(dto.status())
                : member.getStatus();

        boolean wasActiveOwner = ShopRole.OWNER.name().equals(member.getRole())
                && (member.getStatus() == null || member.getStatus() == MEMBER_ENABLED);
        boolean stillActiveOwner = ShopRole.OWNER.name().equals(newRole)
                && newStatus != null && newStatus == MEMBER_ENABLED;
        if (wasActiveOwner && !stillActiveOwner && countActiveOwners(shopId) <= 1) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "一家店至少保留一名店主，不能移除最后一名店主");
        }

        member.setRole(newRole);
        member.setStatus(newStatus);
        if (shopMemberMapper.updateById(member) == 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "数据已被他人修改，请刷新后重试");
        }
        updateMemberAccount(member.getUserId(), dto);
        log.info("[Shop] 变更成员 memberId={}, role={}, status={}", memberId, newRole, newStatus);
    }

    /**
     * 改店员的账号资料。昵称、手机号、口令都在 {@code sys_user} 上。
     *
     * <p>店员是用手机号登录的（建号时 username 就是手机号），所以改手机号时
     * 登录账号一起跟着换，否则他改完就登不进来了；koko 这种用户名不是手机号的
     * 账号只改手机号、不动用户名。</p>
     */
    private void updateMemberAccount(Long userId, MemberUpdateDTO dto) {
        boolean touchNickname = StringUtils.isNotBlank(dto.nickname());
        boolean touchMobile = StringUtils.isNotBlank(dto.mobile());
        boolean touchPassword = StringUtils.isNotBlank(dto.password());
        if (!touchNickname && !touchMobile && !touchPassword) {
            return;
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "成员账号不存在：" + userId);
        }

        if (touchNickname) {
            user.setNickname(dto.nickname().trim());
        }
        if (touchMobile) {
            String mobile = dto.mobile().trim();
            if (!mobile.equals(user.getMobile())) {
                long mobileUsed = sysUserMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getMobile, mobile)
                        .ne(SysUser::getId, userId));
                if (mobileUsed > 0) {
                    throw new ServiceException(ResultCode.DATA_EXISTS, "手机号 " + mobile + " 已被其他账号使用");
                }
                if (Objects.equals(user.getUsername(), user.getMobile())) {
                    long nameUsed = sysUserMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getUsername, mobile)
                            .ne(SysUser::getId, userId));
                    if (nameUsed > 0) {
                        throw new ServiceException(ResultCode.DATA_EXISTS, "登录账号 " + mobile + " 已存在");
                    }
                    user.setUsername(mobile);
                }
                user.setMobile(mobile);
            }
        }
        if (touchPassword) {
            user.setPasswordHash(passwordService.encode(dto.password()));
        }

        if (sysUserMapper.updateById(user) == 0) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "账号资料已被他人修改，请刷新后重试");
        }
        log.info("[Shop] 更新成员账号 userId={}，改昵称={}，改手机号={}，重置密码={}",
                userId, touchNickname, touchMobile, touchPassword);
    }

    /** 店铺工作台 */
    public ShopDashboardVO dashboard(Long shopId) {
        shopGuard.requireMember(shopId);
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        long todayOrders = orderInfoMapper.selectCount(Wrappers.<OrderInfo>lambdaQuery()
                .eq(OrderInfo::getShopId, shopId)
                .ge(OrderInfo::getCreateTime, start)
                .lt(OrderInfo::getCreateTime, end));
        long todayPaid = orZero(orderInfoMapper.sumPaidAmountByShop(shopId, start, end));
        long pendingShipment = orderInfoMapper.selectCount(Wrappers.<OrderInfo>lambdaQuery()
                .eq(OrderInfo::getShopId, shopId)
                .eq(OrderInfo::getStatus, OrderStatus.PENDING_SHIPMENT.name()));
        long pendingRefund = countPendingRefund(shopId);
        long onSaleProduct = productMapper.selectCount(Wrappers.<com.owner.system.mall.domain.entity.Product>lambdaQuery()
                .eq(com.owner.system.mall.domain.entity.Product::getShopId, shopId)
                .eq(com.owner.system.mall.domain.entity.Product::getStatus,
                        com.owner.system.mall.domain.enums.ProductStatus.ON_SALE.name()));

        return new ShopDashboardVO(todayOrders, todayPaid, pendingShipment, pendingRefund, onSaleProduct);
    }

    // ------------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------------

    private long countPendingRefund(Long shopId) {
        // 与售后列表「待处理」筛选共用 RefundStatus.openCodes()，避免两处口径漂移
        return refundMapper.selectCount(Wrappers.<Refund>lambdaQuery()
                .eq(shopId != null, Refund::getShopId, shopId)
                .in(Refund::getStatus, RefundStatus.openCodes()));
    }

    private long countActiveOwners(Long shopId) {
        return shopMemberMapper.selectCount(Wrappers.<ShopMember>lambdaQuery()
                .eq(ShopMember::getShopId, shopId)
                .eq(ShopMember::getRole, ShopRole.OWNER.name())
                .eq(ShopMember::getStatus, MEMBER_ENABLED));
    }

    private SysUser resolveOwner(ShopCreateDTO dto) {
        if (dto.ownerUserId() != null) {
            SysUser user = sysUserMapper.selectById(dto.ownerUserId());
            if (user == null) {
                throw new ServiceException(ResultCode.DATA_NOT_FOUND, "店主用户不存在：" + dto.ownerUserId());
            }
            return user;
        }
        if (StringUtils.isBlank(dto.ownerMobile())) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "必须指定店主：ownerUserId 或 ownerMobile");
        }
        String mobile = dto.ownerMobile().trim();
        SysUser existing = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getMobile, mobile)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        if (StringUtils.isBlank(dto.ownerPassword())) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "按手机号新建店主时必须指定初始密码");
        }
        SysUser user = new SysUser();
        user.setUsername(mobile);
        user.setMobile(mobile);
        user.setNickname(StringUtils.isNotBlank(dto.ownerNickname())
                ? dto.ownerNickname().trim()
                : "店主" + mobile.substring(mobile.length() - 4));
        user.setPasswordHash(passwordService.encode(dto.ownerPassword()));
        user.setPlatformAdmin(0);
        user.setStatus(MEMBER_ENABLED);
        user.setVersion(0);
        sysUserMapper.insert(user);
        return sysUserMapper.selectById(user.getId());
    }

    private ShopVO toShopVo(Shop shop) {
        return new ShopVO(shop.getId(), shop.getName(), shop.getIntro(), shop.getNotice(),
                shop.getContactName(), shop.getContactMobile(), shop.getShipAddress(),
                shop.getBannerUrl(), shop.getBusinessHours(), shop.getPayNote(),
                shop.getStatus(), statusLabel(shop.getStatus()), shop.getVersion(),
                shop.getCreateTime(), shop.getUpdateTime());
    }

    private String statusLabel(Integer status) {
        if (status == null) {
            return "";
        }
        return status == SHOP_DISABLED ? "停用" : "营业";
    }

    private String memberStatusLabel(Integer status) {
        if (status == null) {
            return "";
        }
        return status == MEMBER_DISABLED ? "停用" : "正常";
    }

    private long orZero(Long value) {
        return value == null ? 0L : value;
    }

    /** 把 [ {shopId, <key>} ] 转成 Map<shopId, Long> */
    private Map<Long, Long> toLongMap(List<Map<String, Object>> rows, String key) {
        Map<Long, Long> result = new HashMap<>(rows.size() * 2);
        for (Map<String, Object> row : rows) {
            Object shopId = row.get("shopId");
            Object value = row.get(key);
            if (shopId instanceof Number id && value instanceof Number num) {
                result.put(id.longValue(), num.longValue());
            }
        }
        return result;
    }
}
