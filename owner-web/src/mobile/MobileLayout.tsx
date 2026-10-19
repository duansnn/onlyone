import { useEffect, useState } from 'react';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { cartApi, storeApi } from '@/api/mall';
import { useAuthStore } from '@/store/auth';
import { useShopStore } from '@/store/shop';

/** 底部 Tab 定义：root 为 true 的页面不显示返回键 */
const TABS = [
  { key: 'home', label: '首页', path: '/m', root: true, icon: 'home' },
  { key: 'list', label: '商品', path: '/m/list', root: true, icon: 'grid' },
  { key: 'cart', label: '购物车', path: '/m/cart', root: true, icon: 'cart' },
  { key: 'account', label: '我的', path: '/m/account', root: true, icon: 'user' },
] as const;

const TITLES: { match: RegExp; title: string }[] = [
  { match: /^\/m\/?$/, title: '' },
  { match: /^\/m\/list/, title: '全部商品' },
  { match: /^\/m\/product\//, title: '商品详情' },
  { match: /^\/m\/cart/, title: '购物车' },
  { match: /^\/m\/checkout/, title: '确认订单' },
  { match: /^\/m\/orders\/[^/]+/, title: '订单详情' },
  { match: /^\/m\/orders/, title: '我的订单' },
  { match: /^\/m\/address/, title: '收货地址' },
  { match: /^\/m\/account/, title: '我的' },
];

function Icon({ name }: { name: string }) {
  const common = {
    width: 22,
    height: 22,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.7,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
  };
  switch (name) {
    case 'home':
      return (
        <svg {...common}>
          <path d="M3 10.5 12 3l9 7.5" />
          <path d="M5.5 9.5V21h13V9.5" />
        </svg>
      );
    case 'grid':
      return (
        <svg {...common}>
          <rect x="3.5" y="3.5" width="7" height="7" rx="1.4" />
          <rect x="13.5" y="3.5" width="7" height="7" rx="1.4" />
          <rect x="3.5" y="13.5" width="7" height="7" rx="1.4" />
          <rect x="13.5" y="13.5" width="7" height="7" rx="1.4" />
        </svg>
      );
    case 'cart':
      return (
        <svg {...common}>
          <path d="M3 4h2.2l2.3 11.2h10.2L20 7.5H6" />
          <circle cx="9.5" cy="19" r="1.3" />
          <circle cx="17" cy="19" r="1.3" />
        </svg>
      );
    case 'user':
      return (
        <svg {...common}>
          <circle cx="12" cy="8" r="3.6" />
          <path d="M4.5 20c1.3-3.6 4.1-5.4 7.5-5.4s6.2 1.8 7.5 5.4" />
        </svg>
      );
    default:
      return null;
  }
}

export default function MobileLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const token = useAuthStore((state) => state.token);
  const shopName = useShopStore((state) => state.shopName);
  const loadHome = useShopStore((state) => state.loadHome);
  const shopId = useShopStore((state) => state.shopId);
  const setShop = useShopStore((state) => state.setShop);
  const [cartCount, setCartCount] = useState(0);

  // 未登录也能逛：先拿公开接口里的营业店铺
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
        /* 后端未启动时页面自己提示 */
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

  const path = location.pathname;
  const title = TITLES.find((t) => t.match.test(path))?.title ?? '日用铺';
  const isRoot = TABS.some((t) => t.path === path);
  const activeTab = TABS.find((t) => t.path === path)?.key;

  return (
    <div className="m-root">
      <div className="m-app">
        <header className="m-header">
          {!isRoot && (
            <button className="m-header__back" onClick={() => navigate(-1)} aria-label="返回">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">
                <path d="M15 5l-7 7 7 7" />
              </svg>
            </button>
          )}
          <div className="m-header__title">{path === '/m' ? shopName ?? '日用铺' : title}</div>
          {!path.startsWith('/m/orders') && (
            <div className="m-header__extra">
              <Link to="/m/orders" style={{ color: 'inherit', textDecoration: 'none' }}>
                订单
              </Link>
            </div>
          )}
        </header>

        <div className={`m-body${isRoot ? '' : ' m-body--nobar'}`}>
          <Outlet />
        </div>

        {isRoot && (
          <nav className="m-tabbar">
            {TABS.map((tab) => (
              <Link
                key={tab.key}
                to={tab.path}
                className={`m-tabbar__item${activeTab === tab.key ? ' is-active' : ''}`}
              >
                <span className="m-tabbar__icon">
                  <Icon name={tab.icon} />
                  {tab.key === 'cart' && cartCount > 0 && (
                    <span className="m-badge">{cartCount > 99 ? '99+' : cartCount}</span>
                  )}
                </span>
                {tab.label}
              </Link>
            ))}
          </nav>
        )}
      </div>
    </div>
  );
}
