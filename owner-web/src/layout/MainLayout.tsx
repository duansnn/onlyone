import { useMemo, useState } from 'react';
import { App as AntdApp, Dropdown, Form, Input, Modal, Typography } from 'antd';
import type { MenuProps } from 'antd';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  ApiOutlined,
  DashboardOutlined,
  EditOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  RobotOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { getUserById, updateUser } from '@/api/user';
import { useAuthStore } from '@/store/auth';
import { toast } from '@/utils/feedback';

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

export default function MainLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { modal } = AntdApp.useApp();
  const { userId, nickname, username, signOut } = useAuthStore();

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

  // —— 个人信息修改 ——
  const [profileOpen, setProfileOpen] = useState(false);
  const [profileForm] = Form.useForm<{
    nickname: string;
    mobile?: string;
    email?: string;
    deptName?: string;
    remark?: string;
  }>();
  const [profileVersion, setProfileVersion] = useState<number | undefined>();
  const [profileSaving, setProfileSaving] = useState(false);
  const setNickname = useAuthStore((state) => state.setNickname);

  const openProfile = async () => {
    setProfileOpen(true);
    if (!userId) return;
    try {
      const detail = await getUserById(userId);
      setProfileVersion(detail.version);
      profileForm.setFieldsValue({
        nickname: detail.nickname,
        mobile: undefined, // 列表/详情中的手机号已脱敏，留空表示不修改
        email: detail.email,
        deptName: detail.deptName,
        remark: detail.remark,
      });
    } catch {
      // 拦截器已提示
    }
  };

  const handleProfileSave = async () => {
    if (!userId) return;
    const values = await profileForm.validateFields();
    setProfileSaving(true);
    try {
      await updateUser({
        id: userId,
        nickname: values.nickname,
        mobile: values.mobile,
        email: values.email,
        deptName: values.deptName,
        remark: values.remark,
        version: profileVersion,
      });
      if (values.nickname) setNickname(values.nickname);
      toast.success('个人信息已更新');
      setProfileOpen(false);
    } catch {
      // 拦截器已提示
    } finally {
      setProfileSaving(false);
    }
  };

  const userMenu: MenuProps['items'] = [
    { key: 'profile', label: `账号：${username ?? '-'}`, disabled: true },
    { type: 'divider' },
    { key: 'edit', icon: <EditOutlined />, label: '修改个人信息' },
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
            <Dropdown
              menu={{
                items: userMenu,
                onClick: ({ key }) => {
                  if (key === 'logout') handleSignOut();
                  else if (key === 'edit') void openProfile();
                },
              }}
            >
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

        {/* 修改个人信息 */}
        <Modal
          open={profileOpen}
          title="修改个人信息"
          okText="保存"
          cancelText="取消"
          confirmLoading={profileSaving}
          onOk={() => void handleProfileSave()}
          onCancel={() => setProfileOpen(false)}
          destroyOnClose
          width={520}
        >
          <Form form={profileForm} layout="vertical" style={{ marginTop: 16 }}>
            <Form.Item label="登录账号">
              <Input value={username ?? '-'} disabled />
            </Form.Item>
            <Form.Item
              name="nickname"
              label="昵称"
              rules={[{ required: true, message: '请输入昵称' }]}
            >
              <Input placeholder="昵称" maxLength={32} />
            </Form.Item>
            <Form.Item
              name="mobile"
              label="手机号"
              rules={[
                { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' },
              ]}
              extra="列表中的手机号已脱敏，留空表示不修改"
            >
              <Input placeholder="11 位手机号" maxLength={11} />
            </Form.Item>
            <Form.Item name="email" label="邮箱" rules={[{ type: 'email', message: '邮箱格式不正确' }]}>
              <Input placeholder="邮箱" maxLength={64} />
            </Form.Item>
            <Form.Item name="deptName" label="所属部门">
              <Input placeholder="所属部门" maxLength={64} />
            </Form.Item>
            <Form.Item name="remark" label="备注">
              <Input.TextArea rows={2} maxLength={255} placeholder="可选" />
            </Form.Item>
          </Form>
        </Modal>
      </div>
    </div>
  );
}
