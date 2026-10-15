import { useState } from 'react';
import { Button, Card, Col, Form, Input, Row, Space, Tag, Typography } from 'antd';
import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { useLocation, useNavigate } from 'react-router-dom';
import { kanbanAuthApi } from '@/api/kanban';
import { useKanbanAuthStore } from '@/store/kanbanAuth';
import { toast } from '@/utils/feedback';

const { Title, Text, Paragraph } = Typography;

interface AuthForm {
  username: string;
  password: string;
  /** 注册时的二次确认密码（仅前端校验） */
  confirm?: string;
}

/** 看板登录/注册页（复用看板独立 JWT 登录态） */
export default function KanbanLogin() {
  const [form] = Form.useForm<AuthForm>();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const signIn = useKanbanAuthStore((state) => state.signIn);

  const from = (location.state as { from?: string } | null)?.from ?? '/kanban/projects';

  const handleSubmit = async (values: AuthForm) => {
    setLoading(true);
    try {
      if (mode === 'register') {
        await kanbanAuthApi.register({ username: values.username, password: values.password });
        toast.success('注册成功，请登录');
        setMode('login');
        form.setFieldsValue({ password: '', confirm: '' });
        return;
      }

      const result = await kanbanAuthApi.login({ username: values.username, password: values.password });
      signIn(result.token, result.userId, result.username, result.expireAt);
      toast.success(`欢迎回来，${result.username}`);
      navigate(from, { replace: true });
    } catch (error) {
      const message = error instanceof Error ? error.message : '操作失败';
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };

  const switchMode = (next: 'login' | 'register') => {
    setMode(next);
    form.resetFields();
  };

  return (
    <div className="kb-login">
      <Card className="kb-login__card" styles={{ body: { padding: 0 } }}>
        <Row>
          <Col xs={0} md={11}>
            <div className="kb-login__brand">
              <Title level={2} style={{ color: '#ebe4d6', margin: 0 }}>
                AI 轻量化任务看板
              </Title>
              <Paragraph style={{ color: 'rgba(235, 228, 214, 0.78)', margin: 0 }}>
                三列看板 · 拖拽流转 · AI 任务拆解 · 数据统计
              </Paragraph>
              <Space wrap style={{ marginTop: 8 }}>
                <Tag className="owner-chip">Vue 同构 · React 19</Tag>
                <Tag className="owner-chip">ECharts 可视化</Tag>
                <Tag className="owner-chip">JWT 鉴权</Tag>
                <Tag className="owner-chip">AI 助手</Tag>
              </Space>
            </div>
          </Col>

          <Col xs={24} md={13}>
            <div className="kb-login__form">
              <Title level={3} style={{ marginBottom: 4 }}>
                {mode === 'login' ? '登录看板' : '注册账号'}
              </Title>
              <Text type="secondary">
                {mode === 'login' ? '输入账号密码进入你的任务看板' : '创建账号后即可管理项目与任务'}
              </Text>

              <Form form={form} layout="vertical" size="large" style={{ marginTop: 24 }} onFinish={handleSubmit}>
                <Form.Item
                  name="username"
                  label="用户名"
                  rules={[
                    { required: true, message: '请输入用户名' },
                    { min: 3, max: 50, message: '用户名长度须在 3~50 之间' },
                  ]}
                >
                  <Input prefix={<UserOutlined />} placeholder="3~50 位用户名" autoComplete="username" />
                </Form.Item>

                <Form.Item
                  name="password"
                  label="密码"
                  rules={[
                    { required: true, message: '请输入密码' },
                    { min: 6, max: 64, message: '密码长度须在 6~64 之间' },
                  ]}
                >
                  <Input.Password
                    prefix={<LockOutlined />}
                    placeholder="6~64 位密码"
                    autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                  />
                </Form.Item>

                {mode === 'register' && (
                  <Form.Item
                    name="confirm"
                    label="确认密码"
                    dependencies={['password']}
                    rules={[
                      { required: true, message: '请再次输入密码' },
                      ({ getFieldValue }) => ({
                        validator(_, value) {
                          if (!value || getFieldValue('password') === value) {
                            return Promise.resolve();
                          }
                          return Promise.reject(new Error('两次输入的密码不一致'));
                        },
                      }),
                    ]}
                  >
                    <Input.Password
                      prefix={<LockOutlined />}
                      placeholder="再次输入密码"
                      autoComplete="new-password"
                    />
                  </Form.Item>
                )}

                <Form.Item style={{ marginBottom: 12 }}>
                  <Button type="primary" htmlType="submit" block loading={loading}>
                    {mode === 'login' ? '登 录' : '注 册'}
                  </Button>
                </Form.Item>
              </Form>

              <div style={{ textAlign: 'center' }}>
                {mode === 'login' ? (
                  <Space size={4}>
                    <Text type="secondary">还没有账号？</Text>
                    <Button type="link" style={{ padding: 0 }} onClick={() => switchMode('register')}>
                      立即注册
                    </Button>
                  </Space>
                ) : (
                  <Space size={4}>
                    <Text type="secondary">已有账号？</Text>
                    <Button type="link" style={{ padding: 0 }} onClick={() => switchMode('login')}>
                      返回登录
                    </Button>
                  </Space>
                )}
              </div>
            </div>
          </Col>
        </Row>
      </Card>
    </div>
  );
}
