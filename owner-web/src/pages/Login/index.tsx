import { useState } from 'react';
import { Alert, Button, Card, Col, Form, Input, Row, Space, Tag, Typography } from 'antd';
import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { useLocation, useNavigate } from 'react-router-dom';
import { login } from '@/api/auth';
import { useAuthStore } from '@/store/auth';
import { toast } from '@/utils/feedback';

const { Title, Text, Paragraph } = Typography;

interface LoginForm {
  username: string;
  password: string;
}

/** 数据库内置的示例账号，点击可直接填充 */
const DEMO_ACCOUNTS = [
  { username: 'admin', nickname: '超级管理员' },
  { username: 'zhangsan', nickname: '张三' },
  { username: 'lisi', nickname: '李四' },
];

export default function Login() {
  const [form] = Form.useForm<LoginForm>();
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const signIn = useAuthStore((state) => state.signIn);

  const from = (location.state as { from?: string } | null)?.from ?? '/dashboard';

  const handleSubmit = async (values: LoginForm) => {
    setLoading(true);
    try {
      const { user, token } = await login(values);
      signIn(user, token);
      toast.success(`欢迎回来，${user.nickname}`);
      navigate(from, { replace: true });
    } catch (error) {
      const message = error instanceof Error ? error.message : '登录失败';
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="owner-login">
      <Card className="owner-login__card" styles={{ body: { padding: 0 } }}>
        <Row>
          <Col xs={0} md={12}>
            <div className="owner-login__brand">
              <Title level={2} style={{ color: '#ebe4d6', margin: 0 }}>
                Owner IntelliPortal
              </Title>
              <Paragraph style={{ color: 'rgba(235, 228, 214, 0.78)', margin: 0 }}>
                企业级智能门户 · 后端 Spring Boot 4.1.0 / JDK 25 / Spring AI 2.0.0
              </Paragraph>
              <Space wrap style={{ marginTop: 8 }}>
                <Tag className="owner-chip">MySQL + Druid</Tag>
                <Tag className="owner-chip">Redis 缓存</Tag>
                <Tag className="owner-chip">Quartz 定时任务</Tag>
                <Tag className="owner-chip">Spring AI</Tag>
              </Space>
            </div>
          </Col>

          <Col xs={24} md={12}>
            <div className="owner-login__form">
              <Title level={3} style={{ marginBottom: 4 }}>
                登录门户
              </Title>
              <Text type="secondary">请使用门户账号登录（示例账号见下方）</Text>

              <Form form={form} layout="vertical" size="large" style={{ marginTop: 24 }} onFinish={handleSubmit}>
                <Form.Item
                  name="username"
                  label="登录账号"
                  rules={[{ required: true, message: '请输入登录账号' }]}
                >
                  <Input prefix={<UserOutlined />} placeholder="请输入登录账号" autoComplete="username" />
                </Form.Item>

                <Form.Item
                  name="password"
                  label="登录密码"
                  rules={[{ required: true, message: '请输入登录密码' }]}
                >
                  <Input.Password
                    prefix={<LockOutlined />}
                    placeholder="请输入登录密码"
                    autoComplete="current-password"
                  />
                </Form.Item>

                <Form.Item style={{ marginBottom: 12 }}>
                  <Button type="primary" htmlType="submit" block loading={loading}>
                    登 录
                  </Button>
                </Form.Item>
              </Form>

              <Alert
                type="info"
                showIcon
                message="当前后端尚未提供认证接口"
                description="登录通过查询 sys_user 校验账号是否存在与是否停用，密码暂不校验。接入真实认证后替换 src/api/auth.ts 即可。"
                style={{ marginBottom: 16 }}
              />

              <Space size={8} wrap>
                <Text type="secondary">快速填充：</Text>
                {DEMO_ACCOUNTS.map((account) => (
                  <Tag
                    key={account.username}
                    className="owner-account-chip owner-chip"
                    onClick={() => {
                      form.setFieldsValue({ username: account.username, password: '123456' });
                    }}
                  >
                    {account.username}
                  </Tag>
                ))}
              </Space>

              <div style={{ marginTop: 16 }}>
                <Space size={16} wrap>
                  <Button type="link" style={{ padding: 0 }} onClick={() => window.open('/doc.html', '_blank')}>
                    查看 Knife4j 接口文档 →
                  </Button>
                  <Button type="link" style={{ padding: 0 }} onClick={() => navigate('/kanban/login')}>
                    AI 任务看板入口 →
                  </Button>
                </Space>
              </div>
            </div>
          </Col>
        </Row>
      </Card>
    </div>
  );
}
