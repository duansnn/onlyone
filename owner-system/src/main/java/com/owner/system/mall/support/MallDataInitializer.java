package com.owner.system.mall.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.owner.system.domain.entity.SysUser;
import com.owner.system.mapper.SysUserMapper;
import com.owner.system.mall.domain.entity.Category;
import com.owner.system.mall.domain.entity.Shop;
import com.owner.system.mall.domain.entity.ShopMember;
import com.owner.system.mall.domain.enums.ShopRole;
import com.owner.system.mall.mapper.CategoryMapper;
import com.owner.system.mall.mapper.ShopMapper;
import com.owner.system.mall.mapper.ShopMemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动引导（PRD §16 第 1 步）：可重复执行、只补不覆盖。
 *
 * <ol>
 *   <li>把 koko 标为平台超级管理员，并在其没有密码时写入初始口令；</li>
 *   <li>建出店主账号（默认 shopadmin）；</li>
 *   <li>建出第一家店「日用铺」，并把店主写入店铺成员；</li>
 *   <li>写入五组两级类目（家居清洁 / 厨房 / 收纳 / 个护 / 小百货）。</li>
 * </ol>
 *
 * <p>引导失败只记录日志，不阻断应用启动。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MallDataInitializer implements ApplicationRunner {

    /** 平台超管账号 */
    private static final String PLATFORM_ADMIN_USERNAME = "koko";

    /** 日用铺开店时写入的五组类目 */
    private static final String[][] CATEGORY_SEED = {
            {"家居清洁", "纸巾", "清洁剂", "垃圾袋"},
            {"厨房", "餐具", "保鲜盒", "厨具小件"},
            {"收纳", "收纳盒", "挂钩", "袋"},
            {"个护", "洗护", "口腔", "棉品"},
            {"小百货", "文具小件", "家用杂件", "一次性用品"}
    };

    private final SysUserMapper sysUserMapper;
    private final ShopMapper shopMapper;
    private final ShopMemberMapper shopMemberMapper;
    private final CategoryMapper categoryMapper;
    private final PasswordService passwordService;

    @Value("${owner.mall.bootstrap.enabled:true}")
    private boolean enabled;

    @Value("${owner.mall.bootstrap.admin-password:koko@2026}")
    private String adminPassword;

    @Value("${owner.mall.bootstrap.owner-username:shopadmin}")
    private String ownerUsername;

    @Value("${owner.mall.bootstrap.owner-password:shop123}")
    private String ownerPassword;

    @Value("${owner.mall.bootstrap.owner-mobile:13800000001}")
    private String ownerMobile;

    @Value("${owner.mall.bootstrap.shop-name:日用铺}")
    private String shopName;

    /** 发货地址：售后退货默认用它，店主应在店铺资料里改成真实地址 */
    @Value("${owner.mall.bootstrap.ship-address:广东省深圳市南山区科技园 1 号 101 室（示例地址，请店主改成真实发货地址）}")
    private String shipAddress;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("[MallBootstrap] owner.mall.bootstrap.enabled=false，跳过初始化");
            return;
        }
        try {
            ensurePlatformAdmin();
            SysUser admin = findByUsername(PLATFORM_ADMIN_USERNAME);
            SysUser owner = ensureShopOwner();
            Shop shop = ensureShop();
            ensureMember(shop.getId(), owner.getId());
            // 平台超管同时挂到默认店铺当店主：一个人就能跑通「开店 → 上架商品 → 下单」全流程。
            // 平台身份与店铺身份是两套，且店铺侧接口只认 shop_member，不加这条 koko 建不了商品。
            // 不需要时删掉这条 shop_member 记录即可，平台后台权限不受影响。
            if (admin != null && !admin.getId().equals(owner.getId())) {
                ensureMember(shop.getId(), admin.getId());
            }
            seedCategories(shop.getId());
            log.info("[MallBootstrap] 初始化完成：店铺「{}」(id={}) 店主={}", shop.getName(), shop.getId(), owner.getUsername());
            log.info("[MallBootstrap] 默认口令：平台超管 {} / {}，店主 {} / {}（请尽快修改）",
                    PLATFORM_ADMIN_USERNAME, adminPassword, owner.getUsername(), ownerPassword);
        } catch (Exception e) {
            log.error("[MallBootstrap] 初始化失败，不影响应用启动：{}", e.getMessage(), e);
        }
    }

    private void ensurePlatformAdmin() {
        SysUser koko = findByUsername(PLATFORM_ADMIN_USERNAME);
        if (koko == null) {
            log.warn("[MallBootstrap] 未找到平台超管账号 {}，跳过", PLATFORM_ADMIN_USERNAME);
            return;
        }
        boolean changed = false;
        // 老库可能没把 mobile 写进去，登录用手机号时需要
        if (koko.getPlatformAdmin() == null || koko.getPlatformAdmin() != 1) {
            koko.setPlatformAdmin(1);
            changed = true;
        }
        if (koko.getPasswordHash() == null || koko.getPasswordHash().isBlank()) {
            koko.setPasswordHash(passwordService.encode(adminPassword));
            changed = true;
            log.info("[MallBootstrap] 已为 {} 写入初始口令", PLATFORM_ADMIN_USERNAME);
        }
        if (changed) {
            sysUserMapper.updateById(koko);
        }
    }

    private SysUser ensureShopOwner() {
        SysUser owner = findByUsername(ownerUsername);
        if (owner != null) {
            return owner;
        }
        owner = new SysUser();
        owner.setUsername(ownerUsername);
        owner.setNickname("日用铺掌柜");
        owner.setMobile(ownerMobile);
        owner.setPasswordHash(passwordService.encode(ownerPassword));
        owner.setPlatformAdmin(0);
        owner.setStatus(0);
        owner.setVersion(0);
        owner.setRemark("开店引导创建的店主账号");
        sysUserMapper.insert(owner);
        log.info("[MallBootstrap] 已创建店主账号 {} (id={})", ownerUsername, owner.getId());
        return findByUsername(ownerUsername);
    }

    private Shop ensureShop() {
        List<Shop> shops = shopMapper.selectList(Wrappers.<Shop>lambdaQuery()
                .eq(Shop::getName, shopName)
                .last("LIMIT 1"));
        if (!shops.isEmpty()) {
            return shops.get(0);
        }
        Shop shop = new Shop();
        shop.setName(shopName);
        shop.setIntro("清洁、厨房、收纳、个护、小百货，日常用品一家买齐。");
        shop.setContactName("日用铺掌柜");
        shop.setContactMobile(ownerMobile);
        shop.setShipAddress(shipAddress);
        shop.setBusinessHours("每天 9:00 - 21:00");
        shop.setPayNote("支持微信支付、支付宝，在线支付原路退回。");
        shop.setStatus(0);
        shop.setVersion(0);
        shopMapper.insert(shop);
        log.info("[MallBootstrap] 已创建店铺「{}」id={}", shopName, shop.getId());
        return shop;
    }

    private void ensureMember(Long shopId, Long userId) {
        ShopMember existing = shopMemberMapper.selectOne(Wrappers.<ShopMember>lambdaQuery()
                .eq(ShopMember::getShopId, shopId)
                .eq(ShopMember::getUserId, userId)
                .last("LIMIT 1"));
        if (existing != null) {
            if (!ShopRole.OWNER.name().equals(existing.getRole())) {
                existing.setRole(ShopRole.OWNER.name());
                shopMemberMapper.updateById(existing);
            }
            return;
        }
        ShopMember member = new ShopMember();
        member.setShopId(shopId);
        member.setUserId(userId);
        member.setRole(ShopRole.OWNER.name());
        member.setStatus(0);
        member.setVersion(0);
        shopMemberMapper.insert(member);
        log.info("[MallBootstrap] 已把 userId={} 设为店铺 {} 的店主", userId, shopId);
    }

    private void seedCategories(Long shopId) {
        long exists = categoryMapper.selectCount(Wrappers.<Category>lambdaQuery()
                .eq(Category::getShopId, shopId));
        if (exists > 0) {
            return;
        }
        int parentSort = 0;
        for (String[] group : CATEGORY_SEED) {
            Category parent = new Category();
            parent.setShopId(shopId);
            parent.setParentId(0L);
            parent.setName(group[0]);
            parent.setSort(parentSort++);
            parent.setStatus(0);
            parent.setVersion(0);
            categoryMapper.insert(parent);

            int childSort = 0;
            for (int i = 1; i < group.length; i++) {
                Category child = new Category();
                child.setShopId(shopId);
                child.setParentId(parent.getId());
                child.setName(group[i]);
                child.setSort(childSort++);
                child.setStatus(0);
                child.setVersion(0);
                categoryMapper.insert(child);
            }
        }
        log.info("[MallBootstrap] 已为店铺 {} 写入 {} 组类目", shopId, CATEGORY_SEED.length);
    }

    private SysUser findByUsername(String username) {
        return sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username)
                .last("LIMIT 1"));
    }
}
