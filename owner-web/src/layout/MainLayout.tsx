import { useMemo, useState } from 'react';
import { App as AntdApp, Dropdown, Typography } from 'antd';
import type { MenuProps } from 'antd';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  ApiOutlined,
  BookOutlined,
  DashboardOutlined,
  DatabaseOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  RobotOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import { useAuthStore } from '@/store/auth';

const { Text } = Typography;

const MENU_KEY_TO_PATH: Record<string, string> = {
  dashboard: '/dashboard',
  users: '/users',
  websocket: '/websocket',
  ai: '/ai',
};

const PATH_META: Record<string, { title: string; hint: string }> = {
  '/dashboard': { title: '仪表盘', hint: '服务、连接与 AI 的当前状态' },
  '/users': { title: '用户管理', hint: '门户账号与状态' },
  '/websocket': { title: 'WebSocket 演示', hint: '连接、在线列表与消息收发' },
  '/ai': { title: 'AI 对话', hint: '基于 Spring AI 的门户助手' },
};

const PRIMARY_NAV = [
  { key: 'dashboard', icon: <DashboardOutlined />, label: '仪表盘' },
  { key: 'users', icon: <TeamOutlined />, label: '用户管理' },
  { key: 'websocket', icon: <ApiOutlined />, label: 'WebSocket 演示' },
  { key: 'ai', icon: <RobotOutlined />, label: 'AI 对话' },
];

const OPS_NAV = [
  { key: 'doc', icon: <BookOutlined />, label: '接口文档' },
  { key: 'druid', icon: <DatabaseOutlined />, label: 'Druid 监控' },
  { key: 'actuator', icon: <ApiOutlined />, label: '健康检查' },
];

export default function MainLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { modal } = AntdApp.useApp();
  const { nickname, username, signOut } = useAuthStore();

  const selectedKey = useMemo(() => {
    const hit = Object.entries(MENU_KEY_TO_PATH).find(([, path]) => location.pathname.startsWith(path));
    return hit?.[0] ?? 'dashboard';
  }, [location.pathname]);

  const page = PATH_META[location.pathname] ?? {
    title: 'Owner IntelliPortal',
    hint: '企业智能门户',
  };

  const openKey = (key: string) => {
    if (key in MENU_KEY_TO_PATH) {
      navigate(MENU_KEY_TO_PATH[key]);
      return;
    }
    const external: Record<string, string> = {
      doc: '/doc.html',
      druid: '/druid/login.html',
      actuator: '/actuator/health',
    };
    if (key in external) {
      window.open(external[key], '_blank', 'noopener');
    }
  };

  const handleSignOut = () => {
    modal.confirm({
      title: '确认退出登录？',
      content: '退出后需要重新登录门户。',
      okText: '退出',
      cancelText: '取消',
      onOk: () => {
        signOut();
        navigate('/login', { replace: true });
      },
    });
  };

  const userMenu: MenuProps['items'] = [
    { key: 'profile', label: `账号：${username ?? '-'}`, disabled: true },
    { type: 'divider' },
    { key: 'logout', icon: <LogoutOutlined />, label: '退出登录', danger: true },
  ];

  const displayName = nickname ?? username ?? '未登录';

  return (
    <div className={`owner-shell${collapsed ? ' is-collapsed' : ''}`}>
      <aside className="owner-sider">
        <div className="owner-brand">
          <span className="owner-brand__mark">O</span>
          <span className="owner-brand__text">
            <span className="owner-brand__name">IntelliPortal</span>
            <span className="owner-brand__sub">Owner</span>
          </span>
        </div>

        <nav className="owner-nav">
          {PRIMARY_NAV.map((item) => (
            <button
              key={item.key}
              type="button"
              className={`owner-nav__item${selectedKey === item.key ? ' is-active' : ''}`}
              title={item.label}
              onClick={() => openKey(item.key)}
            >
              <span className="owner-nav__icon">{item.icon}</span>
              <span className="owner-nav__text">{item.label}</span>
            </button>
          ))}

          <div className="owner-nav__label">运维与文档</div>
          {OPS_NAV.map((item) => (
            <button
              key={item.key}
              type="button"
              className="owner-nav__item"
              title={item.label}
              onClick={() => openKey(item.key)}
            >
              <span className="owner-nav__icon">{item.icon}</span>
              <span className="owner-nav__text">{item.label}</span>
            </button>
          ))}
        </nav>

        <button type="button" className="owner-nav__logout" title="退出登录" onClick={handleSignOut}>
          <LogoutOutlined />
          <span className="owner-nav__text">退出登录</span>
        </button>
      </aside>

      <div className="owner-main">
        <header className="owner-top">
          <div>
            <h1 className="owner-top__title">{page.title}</h1>
            <p className="owner-top__hint">{page.hint}</p>
          </div>
          <div className="owner-top__side">
            <button
              type="button"
              className="owner-fold"
              aria-label="切换侧边栏"
              onClick={() => setCollapsed((value) => !value)}
            >
              {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            </button>
            <Dropdown menu={{ items: userMenu, onClick: ({ key }) => key === 'logout' && handleSignOut() }}>
              <div className="owner-user">
                <span className="owner-avatar">{displayName.slice(0, 1)}</span>
                <Text>{displayName}</Text>
              </div>
            </Dropdown>
          </div>
        </header>

        <div className="owner-content">
          <Outlet />
        </div>

        <footer className="owner-footer">
          Owner IntelliPortal · 2026.03-SNAPSHOT · Spring Boot 4.1.0 / JDK 25 / React 19
        </footer>
      </div>
    </div>
  );
}
