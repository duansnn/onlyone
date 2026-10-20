import { useEffect, useState } from 'react';
import { Avatar, Button, ConfigProvider, Drawer, Layout, Menu, Space, Tag } from 'antd';
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
} from '@ant-design/icons';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { authApi } from '@/api/mall';
import { shopRoleLabel, shopRoleOf, useAuthStore } from '@/store/auth';
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

/** 后台色板：与店面同源的暖橙，但更紧凑（小圆角、按钮不用胶囊、表格紧凑） */
const ADMIN_THEME = {
  token: {
    colorPrimary: '#ff5a1f',
    colorPrimaryHover: '#ec4a10',
    colorPrimaryActive: '#d63f08',
    colorLink: '#ff5a1f',
    colorLinkHover: '#ec4a10',
    colorInfo: '#ff5a1f',
    colorText: '#17130f',
    colorTextSecondary: '#3f3831',
    colorTextTertiary: '#857e75',
    colorBorder: '#e7dfd4',
    colorBorderSecondary: '#f0eae1',
    colorSplit: '#f0eae1',
    colorBgLayout: '#f8f5f0',
    colorBgContainer: '#ffffff',
    colorFillAlter: '#faf6f1',
    colorFillSecondary: '#f5f0e9',
    borderRadius: 10,
    borderRadiusLG: 14,
    controlOutline: 'rgba(255, 90, 31, 0.12)',
  },
  components: {
    Button: { primaryShadow: 'none', borderRadius: 8 },
    Input: { activeBorderColor: '#ff5a1f', hoverBorderColor: '#ff7d45' },
    Select: { optionSelectedBg: '#fff2ec' },
    Table: {
      headerBg: '#f7f1e8',
      headerColor: '#4a443c',
      rowHoverBg: '#fdf8f4',
      borderColor: '#f0eae1',
    },
    Menu: {
      itemSelectedBg: '#fff2ec',
      itemSelectedColor: '#ff5a1f',
      itemHoverBg: '#faf6f1',
      itemBorderRadius: 8,
      itemHeight: 42,
    },
    Card: { borderRadiusLG: 14 },
    Tabs: { itemSelectedColor: '#ff5a1f', inkBarColor: '#ff5a1f' },
    Pagination: { itemActiveBg: '#17130f' },
    Tag: { defaultBg: '#faf6f1' },
  },
};

export default function AdminLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, shops, currentShopId, signOut } = useAuthStore();
  // 店主和店员都是 MERCHANT，头像是谁、身份要看 shop_member（shops[].role）
  const roleLabel = shopRoleLabel(shops, currentShopId);
  const roleTone = shopRoleOf(shops, currentShopId)?.role === 'OWNER' ? 'owner' : 'staff';
  const { setShop } = useShopStore();
  const [collapsed, setCollapsed] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const isMobile = useIsMobile();

  useEffect(() => {
    if (currentShopId) setShop(currentShopId);
  }, [currentShopId, setShop]);

  // 切页即收起手机端抽屉
  useEffect(() => {
    setMenuOpen(false);
  }, [location.pathname]);

  const logout = async () => {
    try {
      await authApi.logout();
    } catch {
      /* ignore */
    }
    signOut();
    navigate('/');
  };

  // 单商家模式：后台只有「店铺运营」和「顾客管理」两组，不再有平台/多店铺入口
  const items = [
    { key: '/admin', icon: <BarChartOutlined />, label: '店铺看板' },
    { key: '/admin/orders', icon: <ShoppingOutlined />, label: '订单与发货' },
    { key: '/admin/refunds', icon: <AuditOutlined />, label: '退款/售后' },
    { key: '/admin/products', icon: <AppstoreOutlined />, label: '商品管理' },
    { key: '/admin/stock', icon: <InboxOutlined />, label: '库存管理' },
    { key: '/admin/coupons', icon: <GiftOutlined />, label: '优惠券' },
    { key: '/admin/reviews', icon: <CommentOutlined />, label: '顾客评价' },
    { key: '/admin/customers', icon: <TeamOutlined />, label: '顾客管理' },
    { key: '/admin/shop', icon: <ShopOutlined />, label: '店铺与分类' },
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

  const brandMark = <img className="admin-brand__mark" src="/brand/logo-mark-sm.svg" alt="" />;
  const brandFull = (
    <div className="admin-brand">
      {brandMark}
      <span className="admin-brand__text">日用铺后台</span>
    </div>
  );
  const brandMini = <div className="admin-brand admin-brand--mini">{brandMark}</div>;

  return (
    <ConfigProvider theme={ADMIN_THEME}>
      <Layout className="admin-shell">
        {isMobile ? (
          <Drawer
            open={menuOpen}
            onClose={() => setMenuOpen(false)}
            placement="left"
            size={248}
            closable={false}
            styles={{ body: { padding: 0 } }}
          >
            {brandFull}
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
            {collapsed ? brandMini : brandFull}
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
              <Tag variant="filled" className="admin-tag">
                {shops[0]?.shopName ?? '我的店铺'}
              </Tag>
              <Button size="small" onClick={() => navigate(isMobile ? '/m' : '/')}>
                {isMobile ? '手机版' : '回到店面'}
              </Button>
            </div>
            <Space size={isMobile ? 8 : 12}>
              <span
                className="admin-me"
                role="button"
                tabIndex={0}
                title="个人资料"
                onClick={() => navigate('/account?tab=profile')}
                onKeyDown={(event) => {
                  if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                    navigate('/account?tab=profile');
                  }
                }}
              >
                <Avatar size="small" className="admin-avatar" src={user?.avatarUrl || undefined}>
                  {(user?.nickname || user?.username || '?').slice(0, 1)}
                </Avatar>
                {!isMobile && <span className="admin-user">{user?.nickname || user?.username}</span>}
              </span>
              {!isMobile && roleLabel && (
                <Tag variant="filled" className={`admin-tag admin-tag--${roleTone}`}>
                  {roleLabel}
                </Tag>
              )}
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
    </ConfigProvider>
  );
}
