import { useEffect, useState } from 'react';
import { Button, Form, Input, Tabs } from 'antd';
import { useLocation, useNavigate } from 'react-router-dom';
import { authApi } from '@/api/mall';
import { BizError } from '@/api/request';
import type { LoginResult } from '@/types/mall';
import { useAuthStore } from '@/store/auth';
import { useShopStore } from '@/store/shop';
import { toast } from '@/utils/feedback';

interface LoginForm {
  account: string;
  password: string;
}

interface RegisterForm {
  mobile: string;
  nickname: string;
  password: string;
  confirm: string;
}

export default function Login() {
  const [tab, setTab] = useState('login');
  const [loading, setLoading] = useState(false);
  const [loginForm] = Form.useForm<LoginForm>();
  const navigate = useNavigate();
  const location = useLocation();
  const signIn = useAuthStore((state) => state.signIn);
  const { shopName } = useShopStore();

  const from = (location.state as { from?: string } | null)?.from ?? '/';

  useEffect(() => {
    const mode = new URLSearchParams(location.search).get('mode');
    if (mode === 'register') setTab('register');
  }, [location.search]);

  const afterAuth = (result: LoginResult) => {
    // 登录/注册响应里已经带了当前用户，直接用，不再调 /auth/me ——
    // 那时 token 还没写进 store，请求拦截器取不到，必 401「未认证或登录已过期」
    const me = result.user;
    signIn(result, me);
    toast.success(`欢迎回来，${me.nickname || me.username}`);
    // 主动登录（没有 from）时按身份分流：平台管理员进平台后台，店主/店员进店铺后台，顾客留在店面
    const landing =
      from !== '/'
        ? from
        : me.platformAdmin
          ? '/admin/shops'
          : result.shops.length
            ? '/admin'
            : '/';
    navigate(landing, { replace: true });
  };

  const handleLogin = async (values: LoginForm) => {
    setLoading(true);
    try {
      const result = await authApi.login({ account: values.account.trim(), password: values.password });
      afterAuth(result);
    } catch (e) {
      // 拦截器已对业务/HTTP 错误弹过提示；这里只兜底代码层异常，
      // 否则「登录成功但后续处理抛错」会表现为点了完全没反应
      if (!(e instanceof BizError)) {
        toast.error(e instanceof Error ? `登录失败：${e.message}` : '登录失败');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (values: RegisterForm) => {
    setLoading(true);
    try {
      const result = await authApi.register({
        mobile: values.mobile.trim(),
        password: values.password,
        nickname: values.nickname.trim(),
      });
      afterAuth(result);
    } catch (e) {
      // 拦截器已对业务/HTTP 错误弹过提示；这里只兜底代码层异常，
      // 否则「登录成功但后续处理抛错」会表现为点了完全没反应
      if (!(e instanceof BizError)) {
        toast.error(e instanceof Error ? `登录失败：${e.message}` : '登录失败');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="owner-login">
      <section className="owner-login__story">
        <p className="shop-kicker">{shopName || '日用铺'}</p>
        <h1>生活里的小东西</h1>
        <p>清洁、厨房、收纳、个护。少一点货架，多一点留白。</p>
        <p style={{ marginTop: 24, fontSize: 13, color: '#737373' }}>
          一个账号两种身份：顾客可以下单，店主管店。
        </p>
      </section>

      <section className="owner-login__panel">
        <h2>{tab === 'login' ? '进入日用铺' : '注册新账号'}</h2>
        <Tabs
          activeKey={tab}
          onChange={setTab}
          items={[
            {
              key: 'login',
              label: '登录',
              children: (
                <Form
                  form={loginForm}
                  layout="vertical"
                  size="large"
                  onFinish={handleLogin}
                  autoComplete="off"
                >
                  <Form.Item
                    name="account"
                    label="手机号 / 账号"
                    rules={[{ required: true, message: '请输入手机号或账号' }]}
                  >
                    <Input placeholder="13800000001 或 shopadmin" autoComplete="username" />
                  </Form.Item>
                  <Form.Item
                    name="password"
                    label="密码"
                    rules={[{ required: true, message: '请输入密码' }]}
                  >
                    <Input.Password placeholder="密码" autoComplete="current-password" />
                  </Form.Item>
                  <Button type="primary" htmlType="submit" block loading={loading}>
                    登录
                  </Button>
                </Form>
              ),
            },
            {
              key: 'register',
              label: '注册',
              children: (
                <Form layout="vertical" size="large" onFinish={handleRegister} autoComplete="off">
                  <Form.Item
                    name="mobile"
                    label="手机号"
                    rules={[
                      { required: true, message: '请输入手机号' },
                      { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' },
                    ]}
                  >
                    <Input placeholder="11 位手机号" maxLength={11} />
                  </Form.Item>
                  <Form.Item
                    name="nickname"
                    label="昵称"
                    rules={[{ required: true, message: '请输入昵称' }]}
                  >
                    <Input placeholder="怎么称呼你" maxLength={20} />
                  </Form.Item>
                  <Form.Item
                    name="password"
                    label="密码"
                    rules={[
                      { required: true, message: '请输入密码' },
                      { min: 6, message: '密码至少 6 位' },
                    ]}
                  >
                    <Input.Password placeholder="至少 6 位" autoComplete="new-password" />
                  </Form.Item>
                  <Form.Item
                    name="confirm"
                    label="确认密码"
                    dependencies={['password']}
                    rules={[
                      { required: true, message: '请再输入一次密码' },
                      ({ getFieldValue }) => ({
                        validator: (_, value) =>
                          !value || getFieldValue('password') === value
                            ? Promise.resolve()
                            : Promise.reject(new Error('两次输入的密码不一致')),
                      }),
                    ]}
                  >
                    <Input.Password placeholder="再输入一次" autoComplete="new-password" />
                  </Form.Item>
                  <Button type="primary" htmlType="submit" block loading={loading}>
                    注册并进入
                  </Button>
                </Form>
              ),
            },
          ]}
        />
        <div className="owner-login__accounts">
          <span>示例账号（点一下自动填入）</span>
          <button
            type="button"
            onClick={() => {
              setTab('login');
              loginForm.setFieldsValue({ account: 'koko', password: 'koko@2026' });
            }}
          >
            平台超管 koko / koko@2026
          </button>
          <button
            type="button"
            onClick={() => {
              setTab('login');
              loginForm.setFieldsValue({ account: 'shopadmin', password: 'shop123' });
            }}
          >
            店主 shopadmin / shop123
          </button>
          <button type="button" onClick={() => navigate('/')}>
            先逛逛店面 →
          </button>
        </div>
      </section>
    </div>
  );
}
