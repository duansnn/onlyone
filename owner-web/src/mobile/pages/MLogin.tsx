import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { authApi } from '@/api/mall';
import { useAuthStore } from '@/store/auth';
import { toast } from '@/utils/feedback';

type Mode = 'login' | 'register';

export default function MLogin() {
  const navigate = useNavigate();
  const location = useLocation();
  const signIn = useAuthStore((state) => state.signIn);

  const [mode, setMode] = useState<Mode>('login');
  const [account, setAccount] = useState('');
  const [password, setPassword] = useState('');
  const [nickname, setNickname] = useState('');
  const [busy, setBusy] = useState(false);

  const from = (location.state as { from?: string } | null)?.from;

  const submit = async () => {
    const name = account.trim();
    if (!name) return toast.warning(mode === 'login' ? '请输入账号或手机号' : '请输入手机号');
    if (password.length < 6) return toast.warning('密码至少 6 位');
    if (mode === 'register' && !nickname.trim()) return toast.warning('请填写昵称');

    setBusy(true);
    try {
      const result =
        mode === 'login'
          ? await authApi.login({ account: name, password })
          : await authApi.register({ mobile: name, password, nickname: nickname.trim() });
      signIn(result, result.user);
      toast.success(mode === 'login' ? '登录成功' : '注册成功');
      navigate(from || '/m', { replace: true });
    } catch {
      /* 拦截器已提示 */
    } finally {
      setBusy(false);
    }
  };

  const fill = (value: string) => {
    setMode('login');
    setAccount(value);
    setPassword(value === 'koko' ? 'koko@2026' : 'shop123');
  };

  return (
    <div className="m-root">
      <div className="m-login">
        <div className="m-login__brand">日用铺</div>
        <div className="m-login__slogan">生活日用品，一件也送</div>

        <div className="m-login__form">
          <div className="m-field">
            <span className="m-field__label">{mode === 'login' ? '账号' : '手机号'}</span>
            <input
              className="m-input"
              value={account}
              onChange={(event) => setAccount(event.target.value)}
              placeholder={mode === 'login' ? '用户名或手机号' : '11 位手机号'}
              inputMode={mode === 'login' ? 'text' : 'numeric'}
              autoCapitalize="none"
              autoCorrect="off"
            />
          </div>
          {mode === 'register' && (
            <div className="m-field">
              <span className="m-field__label">昵称</span>
              <input
                className="m-input"
                value={nickname}
                onChange={(event) => setNickname(event.target.value)}
                placeholder="怎么称呼你"
              />
            </div>
          )}
          <div className="m-field">
            <span className="m-field__label">密码</span>
            <input
              className="m-input"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="至少 6 位"
              onKeyDown={(event) => {
                if (event.key === 'Enter') void submit();
              }}
            />
          </div>
        </div>

        <button
          className="m-btn m-btn--primary m-btn--block"
          style={{ marginTop: 18 }}
          disabled={busy}
          onClick={() => void submit()}
        >
          {busy ? '请稍候…' : mode === 'login' ? '登录' : '注册并登录'}
        </button>

        <div className="m-login__switch">
          {mode === 'login' ? '还没有账号？' : '已有账号？'}
          <button onClick={() => setMode(mode === 'login' ? 'register' : 'login')}>
            {mode === 'login' ? '注册一个' : '去登录'}
          </button>
        </div>

        <div className="m-login__demo">
          <div style={{ marginBottom: 6, color: '#111', fontWeight: 600 }}>演示账号（点一下自动填）</div>
          <div>
            平台管理员 / 店主：
            <button onClick={() => fill('koko')} style={{ border: 0, background: 'none', color: '#2563eb', padding: '0 4px' }}>
              koko / koko@2026
            </button>
          </div>
          <div>
            店主：
            <button onClick={() => fill('shopadmin')} style={{ border: 0, background: 'none', color: '#2563eb', padding: '0 4px' }}>
              shopadmin / shop123
            </button>
          </div>
          <div style={{ marginTop: 6 }}>顾客可直接注册（账号用手机号，不是邮箱）</div>
        </div>

        <div className="m-login__switch">
          <button onClick={() => navigate('/m')}>先随便逛逛 →</button>
        </div>
      </div>
    </div>
  );
}
