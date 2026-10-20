import { useState } from 'react';
import { Button, Form, Input } from 'antd';
import {
  CheckOutlined,
  GiftOutlined,
  SafetyCertificateOutlined,
  ShoppingOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { authApi } from '@/api/mall';
import AvatarUploader from '@/components/AvatarUploader';
import { isMerchant, useAuthStore } from '@/store/auth';
import { toast } from '@/utils/feedback';

interface ProfileForm {
  nickname: string;
  email?: string;
  avatarUrl?: string;
}

/** 手机号打星，个人资料页只做展示 */
function maskMobile(mobile?: string) {
  if (!mobile || mobile.length < 7) return mobile ?? '';
  return `${mobile.slice(0, 3)} **** ${mobile.slice(-4)}`;
}

/**
 * 注册后的资料完善页。
 *
 * <p>不是必填墙：昵称注册时就有了，这里主要是头像和邮箱，「稍后再说」直接进店。
 * 之后随时可以在「我的账号 → 个人资料」里改。</p>
 */
export default function Welcome() {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const setUser = useAuthStore((state) => state.setUser);
  const [form] = Form.useForm<ProfileForm>();
  const [saving, setSaving] = useState(false);

  const watchAvatar = Form.useWatch<string | undefined>('avatarUrl', form);
  const watchNickname = Form.useWatch<string | undefined>('nickname', form);

  const nickname = watchNickname?.trim() || user?.nickname || user?.username || '新朋友';
  const avatar = watchAvatar ?? user?.avatarUrl;
  const merchant = isMerchant(user);

  const save = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      const me = await authApi.updateProfile({
        nickname: values.nickname.trim(),
        email: values.email?.trim() || undefined,
        avatarUrl: values.avatarUrl ?? '',
      });
      setUser(me);
      toast.success('资料已保存，欢迎光临');
      navigate('/', { replace: true });
    } catch {
      /* 拦截器已提示 */
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="welcome">
      <section className="welcome__story">
        <div className="welcome__brand">
          <span className="welcome__logo">日</span>
          <span>
            日用铺
            <small>DAILY GOODS STORE</small>
          </span>
        </div>

        <div className="welcome__card">
          <div className="welcome__avatar">
            {avatar ? (
              <img src={avatar} alt="" />
            ) : (
              <span>{nickname.slice(0, 1).toUpperCase()}</span>
            )}
          </div>
          <div className="welcome__name">{nickname}</div>
          <div className="welcome__mobile">{maskMobile(user?.mobile)}</div>
          <span className="welcome__chip">{merchant ? '店主' : '顾客'}</span>
        </div>

        <ul className="welcome__points">
          <li>
            <ShoppingOutlined />
            <div>
              <b>头像会用在评价里</b>
              <span>晒单时比一串手机号亲切得多</span>
            </div>
          </li>
          <li>
            <GiftOutlined />
            <div>
              <b>邮箱用来找回账号</b>
              <span>忘了密码时，店主能按邮箱找到你</span>
            </div>
          </li>
          <li>
            <SafetyCertificateOutlined />
            <div>
              <b>手机号只用于发货联系</b>
              <span>不会出现在评价、也不会对外展示</span>
            </div>
          </li>
        </ul>
      </section>

      <section className="welcome__panel">
        <header className="welcome__head">
          <span className="welcome__step">第 2 步 / 共 2 步</span>
          <h1>把资料补齐</h1>
          <p>传个头像、留个称呼，我们好在订单和评价里认出你。之后点右上角头像 → 个人资料就能改。</p>
        </header>

        <Form
          form={form}
          layout="vertical"
          size="large"
          initialValues={{
            nickname: user?.nickname ?? '',
            email: user?.email ?? '',
            avatarUrl: user?.avatarUrl,
          }}
          onFinish={() => void save()}
        >
          <Form.Item name="avatarUrl" label="头像">
            <AvatarUploader size={92} tip="方形图最好，会裁成圆形" />
          </Form.Item>

          <Form.Item
            name="nickname"
            label="昵称"
            rules={[
              { required: true, message: '请输入昵称' },
              { max: 20, message: '最多 20 个字' },
            ]}
          >
            <Input placeholder="怎么称呼你" maxLength={20} />
          </Form.Item>

          <Form.Item
            name="email"
            label="邮箱（选填）"
            rules={[{ type: 'email', message: '邮箱格式不正确' }]}
          >
            <Input placeholder="用于找回账号，不对外展示" maxLength={64} />
          </Form.Item>

          <div className="welcome__actions">
            <Button type="primary" htmlType="submit" block loading={saving} icon={<CheckOutlined />}>
              保存并开始逛
            </Button>
            <Button type="text" block onClick={() => navigate('/', { replace: true })}>
              稍后再说，先逛逛
            </Button>
          </div>
        </Form>
      </section>
    </div>
  );
}
