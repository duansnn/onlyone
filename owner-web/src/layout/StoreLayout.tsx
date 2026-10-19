import { useEffect, useState } from 'react';
import { Badge, Button, Dropdown, Input, Space, Tag } from 'antd';
import {
  AppstoreOutlined,
  GiftOutlined,
  ProfileOutlined,
  SearchOutlined,
  ShoppingCartOutlined,
  SolutionOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { authApi, cartApi, storeApi } from '@/api/mall';
import { useAuthStore } from '@/store/auth';
import { useShopStore } from '@/store/shop';
import { toast } from '@/utils/feedback';

export default function StoreLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { token, user, shops, signOut } = useAuthStore();
  const { shopId, shopName, setShop, loadHome } = useShopStore();
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
    if (!shopId) return;
    if (keyword.trim().length < 2) {
      toast.warning('搜索关键字至少两个字');
      return;
    }
    navigate(`/products?keyword=${encodeURIComponent(keyword.trim())}`);
  };

  const menuItems = [
    { key: 'orders', label: '我的订单', icon: <ProfileOutlined /> },
    { key: 'address', label: '我的地址', icon: <UserOutlined /> },
    { key: 'coupons', label: '我的优惠券', icon: <GiftOutlined /> },
    { key: 'refunds', label: '退款/售后', icon: <SolutionOutlined /> },
  ];

  /** 菜单项 → 路由。订单是独立页面，其余是账号页的 tab（原来一律跳 account?tab=key，导致「我的订单」落到不存在的 tab） */
  const MENU_PATHS: Record<string, string> = {
    orders: '/orders',
    address: '/account?tab=address',
    coupons: '/account?tab=coupons',
    refunds: '/account?tab=refunds',
  };

  return (
    <div className="mall-shell">
      <header className="mall-header">
        <div className="mall-header__inner">
          <Link to="/" className="mall-brand">
            <span className="mall-brand__dot" />
            {shopName || '日用铺'}
          </Link>

          <Input
            className="mall-search"
            allowClear
            placeholder="搜索商品（至少两个字）"
            prefix={<SearchOutlined />}
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onPressEnter={goSearch}
          />
          <Button type="primary" onClick={goSearch}>
            搜索
          </Button>

          <Space size={18} className="mall-header__nav">
            <Link to="/products">
              <AppstoreOutlined /> 全部商品
            </Link>
            <Link to="/cart">
              <Badge count={cartCount} size="small" offset={[4, -2]}>
                <ShoppingCartOutlined /> 购物车
              </Badge>
            </Link>
            {user ? (
              <Dropdown
                menu={{
                  items: [
                    ...menuItems,
                    { type: 'divider' as const },
                    ...(shops.length || user?.platformAdmin
                      ? [
                          {
                            key: 'admin',
                            label: shops.length ? '进入店铺后台' : '平台管理后台',
                          },
                        ]
                      : []),
                    { key: 'logout', label: '退出登录', danger: true },
                  ],
                  onClick: ({ key }) => {
                    if (key === 'logout') void logout();
                    else if (key === 'admin') navigate(shops.length ? '/admin' : '/admin/shops');
                    else navigate(MENU_PATHS[key] ?? '/');
                  },
                }}
              >
                <span className="mall-user">
                  {user.nickname || user.username}
                  {user.platformAdmin && <Tag color="gold">平台</Tag>}
                </span>
              </Dropdown>
            ) : (
              <Space size={8}>
                <Button size="small" onClick={() => navigate('/login')}>
                  登录
                </Button>
                <Button size="small" type="primary" onClick={() => navigate('/login?mode=register')}>
                  注册
                </Button>
              </Space>
            )}
          </Space>
        </div>
      </header>

      <main className="mall-main">
        <Outlet />
      </main>

      <footer className="mall-footer">
        <span>日用铺 · 清洁 / 厨房 / 收纳 / 个护</span>
        <span>金额单位：元 · 下单后 30 分钟内未支付将自动取消</span>
      </footer>
    </div>
  );
}
