import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '@/api/mall';
import { useAuthStore } from '@/store/auth';
import { toast } from '@/utils/feedback';

export default function MAccount() {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const shops = useAuthStore((state) => state.shops);
  const signOut = useAuthStore((state) => state.signOut);

  const logout = async () => {
    try {
      await authApi.logout();
    } catch {
      /* 令牌过期也照常退出 */
    }
    signOut();
    toast.success('已退出登录');
    navigate('/m', { replace: true });
  };

  if (!user) {
    return (
      <div className="m-empty">
        还没有登录
        <div style={{ marginTop: 16 }}>
          <Link to="/m/login" className="m-btn m-btn--sm m-btn--primary">
            去登录
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div>
      <div className="m-profile">
        {user.avatarUrl ? (
          <img className="m-profile__avatar" src={user.avatarUrl} alt="" />
        ) : (
          <div className="m-profile__avatar">{(user.nickname || user.username).slice(0, 1)}</div>
        )}
        <div style={{ minWidth: 0 }}>
          <div className="m-profile__name m-ellipsis">{user.nickname || user.username}</div>
          <div className="m-profile__sub">
            {user.mobile || user.username}
            {shops.length > 0 && ' · 店主'}
            {user.platformAdmin && ' · 平台管理员'}
          </div>
        </div>
      </div>

      <div className="m-stat">
        {[
          { label: '待确认', status: 'PENDING_PAYMENT' },
          { label: '待发货', status: 'PENDING_SHIPMENT' },
          { label: '待收货', status: 'PENDING_RECEIPT' },
          { label: '全部订单', status: '' },
        ].map((item) => (
          <button
            key={item.label}
            className="m-stat__item"
            onClick={() => navigate(item.status ? `/m/orders?status=${item.status}` : '/m/orders')}
          >
            <span className="m-stat__num">›</span>
            {item.label}
          </button>
        ))}
      </div>

      <div className="m-menu">
        <Link className="m-menu__item" to="/m/orders">
          我的订单
        </Link>
        <Link className="m-menu__item" to="/m/address">
          收货地址
        </Link>
        <Link className="m-menu__item" to="/m/cart">
          购物车
        </Link>
        {(shops.length > 0 || user.platformAdmin) && (
          <Link className="m-menu__item" to="/admin">
            店铺后台
          </Link>
        )}
      </div>

      <div className="m-menu">
        <button className="m-menu__item" onClick={() => void logout()}>
          退出登录
        </button>
      </div>

      <div style={{ padding: '4px 16px 28px', textAlign: 'center', color: '#c4c4c4', fontSize: 11 }}>
        Owner Mall · 日用铺
      </div>
    </div>
  );
}
