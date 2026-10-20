import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '@/api/mall';
import AvatarUploader from '@/components/AvatarUploader';
import { useAuthStore } from '@/store/auth';
import { toast } from '@/utils/feedback';

/**
 * 手机端个人资料。
 *
 * <p>桌面端在 /account?tab=profile 里改资料，手机端单独一页，省得在窄屏里
 * 塞进桌面那套双栏布局。换手机号要短信验证码，仍留给网页版。</p>
 */
export default function MProfile() {
  const navigate = useNavigate();
  const { user, setUser } = useAuthStore();

  const [avatarUrl, setAvatarUrl] = useState<string | undefined>(user?.avatarUrl);
  const [nickname, setNickname] = useState(user?.nickname ?? '');
  const [email, setEmail] = useState(user?.email ?? '');
  const [savingProfile, setSavingProfile] = useState(false);

  const [pwdOpen, setPwdOpen] = useState(false);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [savingPwd, setSavingPwd] = useState(false);

  const saveProfile = async () => {
    if (!nickname.trim()) {
      toast.warning('请输入昵称');
      return;
    }
    setSavingProfile(true);
    try {
      // 空串代表「清掉头像」，后端 trimToNull 落 null
      const me = await authApi.updateProfile({
        nickname: nickname.trim(),
        email: email.trim(),
        avatarUrl: avatarUrl ?? '',
      });
      setUser(me);
      toast.success('资料已更新');
      navigate('/m/account', { replace: true });
    } catch {
      /* 拦截器已提示 */
    } finally {
      setSavingProfile(false);
    }
  };

  const savePassword = async () => {
    if (!currentPassword) {
      toast.warning('请输入当前密码');
      return;
    }
    if (newPassword.length < 6) {
      toast.warning('新密码至少 6 位');
      return;
    }
    setSavingPwd(true);
    try {
      await authApi.changePassword({ currentPassword, newPassword });
      toast.success('密码已修改');
      setPwdOpen(false);
      setCurrentPassword('');
      setNewPassword('');
    } catch {
      /* 拦截器已提示 */
    } finally {
      setSavingPwd(false);
    }
  };

  return (
    <div className="m-pf">
      <div className="m-card">
        <div className="m-pf__avatar">
          <AvatarUploader
            value={avatarUrl}
            onChange={setAvatarUrl}
            size={84}
            tip="支持 jpg / png，不超过 5 MB"
          />
        </div>
        <div className="m-field">
          <span className="m-field__label">昵称</span>
          <input
            className="m-input"
            value={nickname}
            maxLength={20}
            placeholder="怎么称呼你"
            onChange={(event) => setNickname(event.target.value)}
          />
        </div>
        <div className="m-field">
          <span className="m-field__label">邮箱</span>
          <input
            className="m-input"
            value={email}
            maxLength={64}
            placeholder="选填，用于找回账号"
            onChange={(event) => setEmail(event.target.value)}
          />
        </div>
        <div className="m-field">
          <span className="m-field__label">手机号</span>
          <input className="m-input" value={user?.mobile ?? user?.username ?? '-'} readOnly />
        </div>
        <button
          className="m-btn m-btn--primary m-btn--block"
          style={{ marginTop: 14 }}
          disabled={savingProfile}
          onClick={() => void saveProfile()}
        >
          {savingProfile ? '保存中…' : '保存资料'}
        </button>
      </div>

      <div className="m-card">
        <button className="m-pf__toggle" onClick={() => setPwdOpen((open) => !open)}>
          修改密码
          <span>{pwdOpen ? '收起' : '展开'}</span>
        </button>
        {pwdOpen && (
          <>
            <div className="m-field">
              <span className="m-field__label">当前密码</span>
              <input
                className="m-input"
                type="password"
                value={currentPassword}
                placeholder="请输入当前密码"
                onChange={(event) => setCurrentPassword(event.target.value)}
              />
            </div>
            <div className="m-field">
              <span className="m-field__label">新密码</span>
              <input
                className="m-input"
                type="password"
                value={newPassword}
                placeholder="至少 6 位"
                onChange={(event) => setNewPassword(event.target.value)}
              />
            </div>
            <button
              className="m-btn m-btn--primary m-btn--block"
              style={{ marginTop: 14 }}
              disabled={savingPwd}
              onClick={() => void savePassword()}
            >
              {savingPwd ? '提交中…' : '确认修改'}
            </button>
          </>
        )}
      </div>

      <div className="m-pf__hint">换手机号需要短信验证码，请用电脑打开「我的账号 → 个人资料」操作。</div>
    </div>
  );
}
