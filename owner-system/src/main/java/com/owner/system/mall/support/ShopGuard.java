package com.owner.system.mall.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import com.owner.system.mall.domain.entity.Shop;
import com.owner.system.mall.domain.entity.ShopMember;
import com.owner.system.mall.domain.enums.ShopRole;
import com.owner.system.mall.mapper.ShopMapper;
import com.owner.system.mall.mapper.ShopMemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 店铺权限核对：每个店铺接口都从登录态取用户，再查 {@code shop_member}。
 *
 * <p>规则（PRD §14）：</p>
 * <ul>
 *   <li>店主：可改店资料、商品、订单、店员、优惠券、售后；</li>
 *   <li>店员：可改库存、上下架、发货、处理售后；不能改店资料、不能增删店员、不能建券；</li>
 *   <li>平台超管 koko 不挂在店员名单里，调用改库存、发货一律无权限。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShopGuard {

    /** 店铺状态：停用 */
    private static final int SHOP_DISABLED = 1;

    /** 成员状态：停用 */
    private static final int MEMBER_DISABLED = 1;

    private final ShopMapper shopMapper;
    private final ShopMemberMapper shopMemberMapper;

    /** 取店铺，不存在抛 602 */
    public Shop loadShop(Long shopId) {
        if (shopId == null) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "缺少店铺编号");
        }
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "店铺不存在：" + shopId);
        }
        return shop;
    }

    /** 取营业中的店铺：店面访问、加购、下单走这个 */
    public Shop loadActiveShop(Long shopId) {
        Shop shop = loadShop(shopId);
        if (shop.getStatus() != null && shop.getStatus() == SHOP_DISABLED) {
            throw new ServiceException(ResultCode.BUSINESS_ERROR, "店铺已停用");
        }
        return shop;
    }

    /** 查成员，不存在返回 null */
    public ShopMember memberOf(Long shopId, Long userId) {
        if (shopId == null || userId == null) {
            return null;
        }
        return shopMemberMapper.selectOne(Wrappers.<ShopMember>lambdaQuery()
                .eq(ShopMember::getShopId, shopId)
                .eq(ShopMember::getUserId, userId)
                .last("LIMIT 1"));
    }

    /** 当前用户必须是本店正常成员（店主或店员） */
    public ShopMember requireMember(Long shopId) {
        Long userId = MallCurrentUser.requireUserId();
        ShopMember member = memberOf(shopId, userId);
        if (member == null) {
            log.warn("[ShopGuard] userId={} 不是店铺 {} 的成员", userId, shopId);
            throw new ServiceException(ResultCode.FORBIDDEN, "你不是这家店的成员");
        }
        if (member.getStatus() != null && member.getStatus() == MEMBER_DISABLED) {
            throw new ServiceException(ResultCode.FORBIDDEN, "你在本店的成员身份已停用");
        }
        return member;
    }

    /** 当前用户必须是本店店主 */
    public ShopMember requireOwner(Long shopId) {
        ShopMember member = requireMember(shopId);
        if (!ShopRole.OWNER.name().equals(member.getRole())) {
            log.warn("[ShopGuard] userId={} 以 {} 身份尝试执行店主操作，shopId={}",
                    member.getUserId(), member.getRole(), shopId);
            throw new ServiceException(ResultCode.FORBIDDEN, "仅店主可操作");
        }
        return member;
    }

    /** 当前用户必须是平台超管 */
    public void requirePlatformAdmin() {
        MallCurrentUser.requirePlatformAdmin();
    }
}
