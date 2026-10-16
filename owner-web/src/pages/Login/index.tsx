import { useState } from 'react';
import { Button, Form, Input } from 'antd';
import { useLocation, useNavigate } from 'react-router-dom';
import { login } from '@/api/auth';
import { useAuthStore } from '@/store/auth';
import { toast } from '@/utils/feedback';

interface LoginForm {
  username: string;
  password: string;
}

const DEMO_ACCOUNTS = [{ username: 'koko', nickname: '超级管理员' }];

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
      <section className="owner-login__story">
        <p className="shop-kicker">日用铺</p>
        <h1>生活里的小东西</h1>
        <p>清洁、厨房、收纳、个护。少一点货架，多一点留白。</p>
      </section>

      <section className="owner-login__panel">
        <h2>进入店铺</h2>
        <p className="owner-login__lead">使用门户账号登录</p>
        <Form form={form} layout="vertical" size="large" onFinish={handleSubmit}>
          <Form.Item name="username" label="账号" rules={[{ required: true, message: '请输入账号' }]}>
            <Input placeholder="账号" autoComplete="username" />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password placeholder="密码" autoComplete="current-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={loading}>
            登录
          </Button>
        </Form>
        <div className="owner-login__accounts">
          <span>示例</span>
          {DEMO_ACCOUNTS.map((account) => (
            <button
              key={account.username}
              type="button"
              onClick={() => form.setFieldsValue({ username: account.username, password: '123456' })}
            >
              {account.nickname}
            </button>
          ))}
        </div>
      </section>
    </div>
  );
}
