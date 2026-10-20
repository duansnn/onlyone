import { useEffect, useMemo, useState } from 'react';
import { ConfigProvider, Dropdown } from 'antd';
import {
  AppstoreOutlined,
  CloseCircleFilled,
  EnvironmentOutlined,
  GiftOutlined,
  IdcardOutlined,
  ProfileOutlined,
  SearchOutlined,
  ShoppingCartOutlined,
  SolutionOutlined,
} from '@ant-design/icons';
import { Link, Outlet, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { authApi, cartApi, storeApi } from '@/api/mall';
import { isMerchant, shopRoleLabel, useAuthStore } from '@/store/auth';
import { useShopStore } from '@/store/shop';
import { toast } from '@/utils/feedback';

/** 店面色板：暖橙强调色，仅作用于 / 下的店面，不影响店铺后台 */
const STORE_THEME = {
  token: {
    colorPrimary: '#ff5a1f',
    colorPrimaryHover: '#ec4a10',
    colorPrimaryActive: '#d63f08',
    colorLink: '#ff5a1f',
    colorLinkHover: '#ec4a10',
    colorText: '#17130f',
    colorTextSecondary: '#3f3831',
    colorTextTertiary: '#857e75',
    colorBorder: '#eee7de',
    colorBorderSecondary: '#f3ede5',
    colorBgContainer: '#ffffff',
    borderRadius: 14,
    borderRadiusLG: 18,
    controlOutline: 'rgba(255, 90, 31, 0.12)',
  },
  components: {
    Button: { primaryShadow: 'none', borderRadius: 999 },
    Input: { activeBorderColor: '#ff5a1f', hoverBorderColor: '#ff7d45' },
    Pagination: { itemActiveBg: '#17130f' },
    Tabs: { itemSelectedColor: '#ff5a1f', inkBarColor: '#ff5a1f' },
  },
};

export default function StoreLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const { token, user, shops, currentShopId, signOut } = useAuthStore();
  // 店主、店员都是 MERCHANT，具体身份看当前店铺里的 role
  const roleLabel = shopRoleLabel(shops, currentShopId);
  const { shopId, shopName, home, setShop, loadHome } = useShopStore();
  const [cartCount, setCartCount] = useState(0);
  const [keyword, setKeyword] = useState('');

  // 未登录也要能逛：先从公开接口拿到店铺
  useEffect(() => {
    if (shopId) {
      void loadHome(shopId);
      return;
    }
    void (async () => {
      try {
        const list = await storeApi.shops();
        if (list.length) {
          setShop(list[0].shopId, list[0].shopName);
          await loadHome(list[0].shopId, true);
        }
      } catch {
        /* 后端未启动时静默，页面自己会提示 */
      }
    })();
  }, [shopId, setShop, loadHome]);

  useEffect(() => {
    if (!token) {
      setCartCount(0);
      return;
    }
    void cartApi
      .count()
      .then((count) => setCartCount(Number(count) || 0))
      .catch(() => setCartCount(0));
  }, [token, location.pathname]);

  // 搜索框跟随 URL 上的关键词，刷新后不丢
  useEffect(() => {
    setKeyword(searchParams.get('keyword') ?? '');
  }, [searchParams]);

  const categories = useMemo(() => home?.categories ?? [], [home]);
  const currentCategoryId = searchParams.get('categoryId') ?? '';

  const logout = async () => {
    try {
      await authApi.logout();
    } catch {
      /* 令牌过期也照常退出 */
    }
    signOut();
    toast.success('已退出登录');
    navigate('/');
  };

  const goSearch = () => {
    const value = keyword.trim();
    if (value.length < 2) {
      toast.warning('搜索关键字至少两个字');
      return;
    }
    navigate(`/products?keyword=${encodeURIComponent(value)}`);
  };

  const menuItems = [
    { key: 'profile', label: '个人资料', icon: <IdcardOutlined /> },
    { key: 'orders', label: '我的订单', icon: <ProfileOutlined /> },
    { key: 'address', label: '我的地址', icon: <EnvironmentOutlined /> },
    { key: 'coupons', label: '我的优惠券', icon: <GiftOutlined /> },
    { key: 'refunds', label: '退款/售后', icon: <SolutionOutlined /> },
  ];

  /** 菜单项 → 路由。订单是独立页面，其余是账号页的 tab */
  const MENU_PATHS: Record<string, string> = {
    profile: '/account?tab=profile',
    orders: '/orders',
    address: '/account?tab=address',
    coupons: '/account?tab=coupons',
    refunds: '/account?tab=refunds',
  };

  const isHome = location.pathname === '/';

  return (
    <ConfigProvider theme={STORE_THEME}>
      <div className="sf-shell">
        <header className="sf-header">
          <div className="sf-header__inner">
            <Link to="/" className="sf-brand" aria-label="返回首页">
              <img className="sf-brand__mark" src="/brand/logo-mark.svg" alt="" />
              <span className="sf-brand__text">
                <span className="sf-brand__name">{shopName || '日用铺'}</span>
                <span className="sf-brand__sub">Daily Goods Store</span>
              </span>
            </Link>

            <form
              className="sf-search"
              onSubmit={(event) => {
                event.preventDefault();
                goSearch();
              }}
            >
              <SearchOutlined className="sf-search__icon" />
              <input
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="搜索清洁、厨房、收纳好物…"
                aria-label="搜索商品"
              />
              {keyword && (
                <button
                  type="button"
                  className="sf-search__clear"
                  aria-label="清空搜索"
                  onClick={() => setKeyword('')}
                >
                  <CloseCircleFilled />
                </button>
              )}
              <button type="submit" className="sf-search__btn">
                搜索
              </button>
            </form>

            <nav className="sf-nav">
              <Link to="/products" className="sf-nav__link sf-nav__link--hide">
                <AppstoreOutlined /> 全部商品
              </Link>
              <Link to="/orders" className="sf-nav__link sf-nav__link--hide">
                <ProfileOutlined /> 我的订单
              </Link>
              <Link to="/cart" className="sf-cart" aria-label="购物车">
                <ShoppingCartOutlined />
                <span className="sf-cart__text">购物车</span>
                {cartCount > 0 && <em className="sf-cart__badge">{cartCount > 99 ? '99+' : cartCount}</em>}
              </Link>
              {user ? (
                <Dropdown
                  menu={{
                    items: [
                      ...menuItems,
                      { type: 'divider' as const },
                      // 只有店主能看到后台入口，顾客的菜单里不出现
                      ...(isMerchant(user) ? [{ key: 'admin', label: '进入店铺后台' }] : []),
                      { key: 'logout', label: '退出登录', danger: true },
                    ],
                    onClick: ({ key }) => {
                      if (key === 'logout') void logout();
                      else if (key === 'admin') navigate('/admin');
                      else navigate(MENU_PATHS[key] ?? '/');
                    },
                  }}
                >
                  <span className="sf-user">
                    <span className={`sf-user__avatar${user.avatarUrl ? ' sf-user__avatar--img' : ''}`}>
                      {user.avatarUrl ? (
                        <img src={user.avatarUrl} alt="" />
                      ) : (
                        (user.nickname || user.username || 'U').slice(0, 1).toUpperCase()
                      )}
                    </span>
                    {user.nickname || user.username}
                    {roleLabel && <span className="sf-user__role">{roleLabel}</span>}
                  </span>
                </Dropdown>
              ) : (
                <div className="sf-auth">
                  <Link to="/login" className="sf-auth__login">
                    登录
                  </Link>
                  <Link to="/login?mode=register" className="sf-auth__register">
                    免费注册
                  </Link>
                </div>
              )}
            </nav>
          </div>
        </header>

        <div className="sf-subnav">
          <div className="sf-subnav__inner">
            <Link to="/" className={`sf-subnav__item${isHome ? ' is-active' : ''}`}>
              首页
            </Link>
            {categories.map((category) => (
              <Link
                key={category.id}
                to={`/products?categoryId=${category.id}`}
                className={`sf-subnav__item${currentCategoryId === category.id ? ' is-active' : ''}`}
              >
                {category.name}
              </Link>
            ))}
            <Link to="/products" className="sf-subnav__item">
              全部商品
            </Link>
            <Link to="/account?tab=coupons" className="sf-subnav__item">
              领券中心
            </Link>
            <span className="sf-subnav__spacer" />
            <span className="sf-subnav__note">
              <i className="sf-dot" />
              {home?.businessHours ? `营业时间 ${home.businessHours}` : '今日正常营业'}
            </span>
          </div>
        </div>

        <main className="sf-main">
          <Outlet />
        </main>

        <footer className="sf-footer">
          <div className="sf-footer__inner">
            <div>
              <div className="sf-footer__brand">{shopName || '日用铺'}</div>
              <p className="sf-footer__intro">
                {home?.intro || '清洁、厨房、收纳、个护。把日常需要的小东西，认真挑好每一件。'}
              </p>
            </div>
            <div className="sf-footer__col">
              <h3>逛店</h3>
              <Link to="/products">全部商品</Link>
              {categories.slice(0, 3).map((category) => (
                <Link key={category.id} to={`/products?categoryId=${category.id}`}>
                  {category.name}
                </Link>
              ))}
            </div>
            <div className="sf-footer__col">
              <h3>服务</h3>
              <Link to="/cart">购物车</Link>
              <Link to="/orders">我的订单</Link>
              <Link to="/account?tab=coupons">领券中心</Link>
              <Link to="/account?tab=refunds">退款/售后</Link>
            </div>
            <div className="sf-footer__col">
              <h3>联系</h3>
              <p>{home?.businessHours ? `营业时间：${home.businessHours}` : '营业时间：每日 09:00 - 21:00'}</p>
              <p>下单后请等待店主确认收款</p>
              <p>售后按整单处理</p>
            </div>
          </div>
          <div className="sf-footer__bottom">
            <span>© {new Date().getFullYear()} {shopName || '日用铺'} · 日用铺电商系统</span>
            <span>金额单位：元 · 本店目前仅支持线下收款</span>
          </div>
        </footer>
      </div>
    </ConfigProvider>
  );
}
