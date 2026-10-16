import { useEffect, useMemo, useState } from 'react';
import { Avatar, Button, Layout, Menu, Select, Space, Tag } from 'antd';
import {
  AppstoreOutlined,
  AuditOutlined,
  BarChartOutlined,
  CommentOutlined,
  GiftOutlined,
  InboxOutlined,
  ShopOutlined,
  ShoppingOutlined,
  TeamOutlined,
  UserSwitchOutlined,
} from '@ant-design/icons';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { authApi } from '@/api/mall';
import { useAuthStore } from '@/store/auth';
import { useShopStore } from '@/store/shop';
import { toast } from '@/utils/feedback';

export default function AdminLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, shops, currentShopId, setCurrentShop, signOut } = useAuthStore();
  const { setShop } = useShopStore();
  const [collapsed, setCollapsed] = useState(false);

  const shopOptions = useMemo(
    () => shops.map((s) => ({ value: s.shopId, label: `${s.shopName}（${s.roleLabel}）` })),
    [shops],
  );

  useEffect(() => {
    if (currentShopId) setShop(currentShopId);
  }, [currentShopId, setShop]);

  const logout = async () => {
    try {
      await authApi.logout();
    } catch {
      /* ignore */
    }
    signOut();
    navigate('/');
  };

  const items = [
    { key: '/admin', icon: <BarChartOutlined />, label: '店铺看板' },
    { key: '/admin/orders', icon: <ShoppingOutlined />, label: '订单与发货' },
    { key: '/admin/refunds', icon: <AuditOutlined />, label: '退款/售后' },
    { key: '/admin/products', icon: <AppstoreOutlined />, label: '商品管理' },
    { key: '/admin/stock', icon: <InboxOutlined />, label: '库存管理' },
    { key: '/admin/coupons', icon: <GiftOutlined />, label: '优惠券' },
    { key: '/admin/reviews', icon: <CommentOutlined />, label: '顾客评价' },
    { key: '/admin/shop', icon: <ShopOutlined />, label: '店铺与分类' },
    ...(user?.platformAdmin
      ? [
          { type: 'divider' as const },
          { key: '/admin/shops', icon: <TeamOutlined />, label: '平台 · 店铺管理' },
          { key: '/admin/users', icon: <UserSwitchOutlined />, label: '平台 · 用户管理' },
        ]
      : []),
  ];

  const selected = items
    .filter((i) => 'key' in i && typeof i.key === 'string')
    .map((i) => i.key as string)
    .filter((k) => location.pathname === k || location.pathname.startsWith(`${k}/`))
    .sort((a, b) => b.length - a.length)[0];

  return (
    <Layout className="admin-shell">
      <Layout.Sider
        theme="light"
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        width={216}
        className="admin-sider"
      >
        <div className="admin-brand">{collapsed ? '店' : '日用铺后台'}</div>
        <Menu
          mode="inline"
          items={items}
          selectedKeys={selected ? [selected] : ['/admin']}
          onClick={({ key }) => navigate(key)}
        />
      </Layout.Sider>

      <Layout>
        <Layout.Header className="admin-header">
          <Space size={12}>
            {shopOptions.length > 1 ? (
              <Select
                value={currentShopId ?? undefined}
                options={shopOptions}
                style={{ width: 220 }}
                onChange={(value) => setCurrentShop(value)}
              />
            ) : (
              <Tag color="blue">{shops[0]?.shopName ?? '未绑定店铺'}</Tag>
            )}
            <Button size="small" onClick={() => navigate('/')}>
              回到店面
            </Button>
          </Space>
          <Space size={12}>
            <Avatar size="small" style={{ background: '#111' }}>
              {(user?.nickname || user?.username || '?').slice(0, 1)}
            </Avatar>
            <span>{user?.nickname || user?.username}</span>
            {user?.platformAdmin && <Tag color="gold">平台管理员</Tag>}
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
