import { useEffect, useMemo, useState } from 'react';
import { Avatar, Button, Drawer, Layout, Menu, Select, Space, Tag } from 'antd';
import {
  AppstoreOutlined,
  AuditOutlined,
  BarChartOutlined,
  CommentOutlined,
  GiftOutlined,
  InboxOutlined,
  MenuOutlined,
  ShopOutlined,
  ShoppingOutlined,
  TeamOutlined,
  UserSwitchOutlined,
} from '@ant-design/icons';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { authApi } from '@/api/mall';
import { useAuthStore } from '@/store/auth';
import { useShopStore } from '@/store/shop';

/** 窄屏（手机）判定：用 matchMedia 同步取初值，避免首屏闪一下桌面侧栏 */
function useIsMobile(breakpoint = 768) {
  const query = `(max-width: ${breakpoint - 1}px)`;
  const [isMobile, setIsMobile] = useState(
    () => typeof window !== 'undefined' && window.matchMedia(query).matches,
  );

  useEffect(() => {
    const mql = window.matchMedia(query);
    const onChange = (event: MediaQueryListEvent) => setIsMobile(event.matches);
    setIsMobile(mql.matches);
    mql.addEventListener('change', onChange);
    return () => mql.removeEventListener('change', onChange);
  }, [query]);

  return isMobile;
}

export default function AdminLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, shops, currentShopId, setCurrentShop, signOut } = useAuthStore();
  const { setShop } = useShopStore();
  const [collapsed, setCollapsed] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const isMobile = useIsMobile();

  const shopOptions = useMemo(
    () => shops.map((s) => ({ value: s.shopId, label: `${s.shopName}（${s.roleLabel}）` })),
    [shops],
  );

  useEffect(() => {
    if (currentShopId) setShop(currentShopId);
  }, [currentShopId, setShop]);

  // 切页即收起手机端抽屉
  useEffect(() => {
    setMenuOpen(false);
  }, [location.pathname]);

  // 纯平台管理员没有店铺身份，店铺看板对他无意义，直接落到平台·店铺管理
  useEffect(() => {
    if (!shops.length && user?.platformAdmin && location.pathname === '/admin') {
      navigate('/admin/shops', { replace: true });
    }
  }, [shops.length, user?.platformAdmin, location.pathname, navigate]);

  const logout = async () => {
    try {
      await authApi.logout();
    } catch {
      /* ignore */
    }
    signOut();
    navigate('/');
  };

  // 纯平台管理员（没有任何店铺身份）不显示店铺侧菜单，避免点进去都是 403
  const shopItems = shops.length
    ? [
        { key: '/admin', icon: <BarChartOutlined />, label: '店铺看板' },
        { key: '/admin/orders', icon: <ShoppingOutlined />, label: '订单与发货' },
        { key: '/admin/refunds', icon: <AuditOutlined />, label: '退款/售后' },
        { key: '/admin/products', icon: <AppstoreOutlined />, label: '商品管理' },
        { key: '/admin/stock', icon: <InboxOutlined />, label: '库存管理' },
        { key: '/admin/coupons', icon: <GiftOutlined />, label: '优惠券' },
        { key: '/admin/reviews', icon: <CommentOutlined />, label: '顾客评价' },
        { key: '/admin/shop', icon: <ShopOutlined />, label: '店铺与分类' },
      ]
    : [];

  const items = [
    ...shopItems,
    ...(user?.platformAdmin
      ? [
          ...(shopItems.length ? [{ type: 'divider' as const }] : []),
          { key: '/admin/shops', icon: <TeamOutlined />, label: '平台 · 店铺管理' },
          { key: '/admin/users', icon: <UserSwitchOutlined />, label: '平台 · 用户管理' },
        ]
      : []),
  ];

  const selected = (items as { key?: string }[])
    .map((i) => i.key)
    .filter((k): k is string => typeof k === 'string')
    .filter((k) => location.pathname === k || location.pathname.startsWith(`${k}/`))
    .sort((a, b) => b.length - a.length)[0];

  const menu = (
    <Menu
      mode="inline"
      items={items}
      selectedKeys={selected ? [selected] : []}
      onClick={({ key }) => {
        navigate(key);
        setMenuOpen(false);
      }}
      style={isMobile ? { borderInlineEnd: 'none' } : undefined}
    />
  );

  const brand = <div className="admin-brand">日用铺后台</div>;

  return (
    <Layout className="admin-shell">
      {isMobile ? (
        <Drawer
          open={menuOpen}
          onClose={() => setMenuOpen(false)}
          placement="left"
          width={248}
          closable={false}
          styles={{ body: { padding: 0 } }}
        >
          {brand}
          {menu}
        </Drawer>
      ) : (
        <Layout.Sider
          theme="light"
          collapsible
          collapsed={collapsed}
          onCollapse={setCollapsed}
          width={216}
          className="admin-sider"
        >
          <div className="admin-brand">{collapsed ? '店' : '日用铺后台'}</div>
          {menu}
        </Layout.Sider>
      )}

      <Layout>
        <Layout.Header className="admin-header">
          <div className="admin-header__left">
            {isMobile && (
              <Button
                type="text"
                className="admin-header__menu"
                icon={<MenuOutlined />}
                onClick={() => setMenuOpen(true)}
                aria-label="打开菜单"
              />
            )}
            {shopOptions.length > 1 ? (
              <Select
                value={currentShopId ?? undefined}
                options={shopOptions}
                style={{ width: isMobile ? 148 : 220 }}
                onChange={(value) => setCurrentShop(value)}
              />
            ) : shops.length ? (
              <Tag color="blue">{shops[0]?.shopName ?? '未绑定店铺'}</Tag>
            ) : (
              <Tag color="gold">平台视角</Tag>
            )}
            <Button size="small" onClick={() => navigate(isMobile ? '/m' : '/')}>
              {isMobile ? '手机版' : '回到店面'}
            </Button>
          </div>
          <Space size={isMobile ? 8 : 12}>
            <Avatar size="small" style={{ background: '#111' }}>
              {(user?.nickname || user?.username || '?').slice(0, 1)}
            </Avatar>
            {!isMobile && <span>{user?.nickname || user?.username}</span>}
            {!isMobile && user?.platformAdmin && <Tag color="gold">平台管理员</Tag>}
            <Button size="small" onClick={() => void logout()}>
              退出
            </Button>
          </Space>
        </Layout.Header>
        <Layout.Content className="admin-content">
          <Outlet />
        </Layout.Content>
      </Layout>
    </Layout>
  );
}
