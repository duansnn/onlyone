package com.owner.system.mall.domain.enums;

import lombok.Getter;

/**
 * 全站角色。只有两种，因为本站是「一个店主 + 很多顾客」的单商家模式：
 * <ul>
 *   <li>{@link #MERCHANT} 店主（商家）：本站唯一经营者，能进店铺后台，
 *       能看到商品、订单、顾客与店铺资料；</li>
 *   <li>{@link #CUSTOMER} 顾客：只能逛店面、下单、看自己的订单与地址。</li>
 * </ul>
 *
 * <p>判定依据是 {@code sys_user.platform_admin = 1}（koko 为 1），
 * 或在该店 {@code shop_member} 里有成员身份。前者是常态，后者是为了兼容
 * 早期「平台超管 + 店铺成员」两套身份并存的数据。</p>
 */
@Getter
public enum MallRole {

    /** 店主（商家） */
    MERCHANT("店主"),
    /** 顾客 */
    CUSTOMER("顾客");

    private final String label;

    MallRole(String label) {
        this.label = label;
    }

    /** 是否是经营者身份（能进后台） */
    public boolean isMerchant() {
        return this == MERCHANT;
    }
}
