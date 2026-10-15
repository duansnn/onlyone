import { useMemo } from 'react';
import { App as AntdApp, Dropdown, Typography } from 'antd';
import type { MenuProps } from 'antd';
import { Outlet, useLocation, useNavigate, useParams } from 'react-router-dom';
import {
  AppstoreOutlined,
  BarChartOutlined,
  BookOutlined,
  ClockCircleOutlined,
  EditOutlined,
  LogoutOutlined,
  RobotOutlined,
} from '@ant-design/icons';
import { useKanbanAuthStore } from '@/store/kanbanAuth';

const { Text } = Typography;

/** 看板顶部导航栏，独立于门户 MainLayout */
export default function KanbanLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { projectId } = useParams();
  const { modal } = AntdApp.useApp();
  const { username, signOut } = useKanbanAuthStore();

  const isBoard = location.pathname.endsWith('/board');
  const isStats = location.pathname.endsWith('/stats');
  const isNotes = location.pathname.startsWith('/kanban/notes');
  const isFocus = location.pathname.startsWith('/kanban/focus');
  const isBlog = location.pathname.startsWith('/kanban/blog');

  const activeKey = useMemo(() => {
    if (isBoard) return 'board';
    if (isStats) return 'stats';
    if (isNotes) return 'notes';
    if (isFocus) return 'focus';
    if (isBlog) return 'blog';
    return 'home';
  }, [isBoard, isStats, isNotes, isFocus, isBlog]);

  const handleSignOut = () => {
    modal.confirm({
      title: '确认退出登录？',
      content: '退出后需要重新登录看板。',
      okText: '退出',
      cancelText: '取消',
      onOk: () => {
        signOut();
        navigate('/kanban/login', { replace: true });
      },
    });
  };

  const userMenu: MenuProps['items'] = [
    { key: 'profile', label: `账号：${username ?? '-'}`, disabled: true },
    { type: 'divider' },
    { key: 'logout', icon: <LogoutOutlined />, label: '退出登录', danger: true },
  ];

  return (
    <div className="kb-shell">
      <header className="kb-topbar">
        <div className="kb-topbar__left">
          <span className="kb-logo">看板</span>
          <nav className="kb-nav">
            <button
              type="button"
              className={`kb-nav__item${activeKey === 'home' ? ' is-active' : ''}`}
              onClick={() => navigate('/kanban/projects')}
            >
              <AppstoreOutlined />
              <span>项目</span>
            </button>
            <button
              type="button"
              className={`kb-nav__item${activeKey === 'notes' ? ' is-active' : ''}`}
              onClick={() => navigate('/kanban/notes')}
            >
              <BookOutlined />
              <span>知识库</span>
            </button>
            <button
              type="button"
              className={`kb-nav__item${activeKey === 'focus' ? ' is-active' : ''}`}
              onClick={() => navigate('/kanban/focus')}
            >
              <ClockCircleOutlined />
              <span>番茄钟</span>
            </button>
            <button
              type="button"
              className={`kb-nav__item${activeKey === 'blog' ? ' is-active' : ''}`}
              onClick={() => navigate('/kanban/blog')}
            >
              <EditOutlined />
              <span>博客</span>
            </button>
            {projectId && (
              <>
                <button
                  type="button"
                  className={`kb-nav__item${activeKey === 'board' ? ' is-active' : ''}`}
                  onClick={() => navigate(`/kanban/projects/${projectId}/board`)}
                >
                  <RobotOutlined />
                  <span>看板</span>
                </button>
                <button
                  type="button"
                  className={`kb-nav__item${activeKey === 'stats' ? ' is-active' : ''}`}
                  onClick={() => navigate(`/kanban/projects/${projectId}/stats`)}
                >
                  <BarChartOutlined />
                  <span>统计</span>
                </button>
              </>
            )}
          </nav>
        </div>

        <Dropdown menu={{ items: userMenu, onClick: ({ key }) => key === 'logout' && handleSignOut() }}>
          <div className="kb-user">
            <span className="kb-user__avatar">{username?.slice(0, 1) ?? '?'}</span>
            <Text>{username ?? '未登录'}</Text>
          </div>
        </Dropdown>
      </header>

      <main className="kb-main">
        <Outlet />
      </main>
    </div>
  );
}
