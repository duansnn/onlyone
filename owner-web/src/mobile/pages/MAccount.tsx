import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '@/api/mall';
import { isMerchant, shopRoleLabel, useAuthStore } from '@/store/auth';
import { toast } from '@/utils/feedback';

/** 订单状态入口图标（线条风格，与底栏图标保持一致） */
function StatusIcon({ name }: { name: string }) {
  const common = {
    width: 20,
    height: 20,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.7,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
  };
  switch (name) {
    case 'clock':
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="8.5" />
          <path d="M12 7.5V12l3 1.8" />
        </svg>
      );
    case 'box':
      return (
        <svg {...common}>
          <path d="M12 3.2 20 7.6v8.8L12 20.8 4 16.4V7.6z" />
          <path d="M4 7.6l8 4.4 8-4.4" />
          <path d="M12 12v8.8" />
        </svg>
      );
    case 'truck':
      return (
        <svg {...common}>
          <path d="M3 6.5h10.5v9H3z" />
          <path d="M13.5 10h3.4l3.1 3.2v2.3h-6.5z" />
          <circle cx="7" cy="18" r="1.6" />
          <circle cx="17" cy="18" r="1.6" />
        </svg>
      );
    default:
      return (
        <svg {...common}>
          <path d="M8 6.5h12M8 12h12M8 17.5h12" />
          <circle cx="4.2" cy="6.5" r="1.1" />
          <circle cx="4.2" cy="12" r="1.1" />
          <circle cx="4.2" cy="17.5" r="1.1" />
        </svg>
      );
  }
}

export default function MAccount() {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const shops = useAuthStore((state) => state.shops);
  const currentShopId = useAuthStore((state) => state.currentShopId);
  const signOut = useAuthStore((state) => state.signOut);
  // 店主、店员都是 MERCHANT，具体身份看当前店铺里的 role
  const roleLabel = shopRoleLabel(shops, currentShopId);

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
            {roleLabel && ` · ${roleLabel}`}
          </div>
        </div>
      </div>

      <div className="m-stat">
        {[
          { label: '待确认', status: 'PENDING_PAYMENT', icon: 'clock' },
          { label: '待发货', status: 'PENDING_SHIPMENT', icon: 'box' },
          { label: '待收货', status: 'PENDING_RECEIPT', icon: 'truck' },
          { label: '全部订单', status: '', icon: 'list' },
        ].map((item) => (
          <button
            key={item.label}
            className="m-stat__item"
            onClick={() => navigate(item.status ? `/m/orders?status=${item.status}` : '/m/orders')}
          >
            <span className="m-stat__icon">
              <StatusIcon name={item.icon} />
            </span>
            {item.label}
          </button>
        ))}
      </div>

      <div className="m-menu">
        <Link className="m-menu__item" to="/m/profile">
          个人资料
        </Link>
        <Link className="m-menu__item" to="/m/orders">
          我的订单
        </Link>
        <Link className="m-menu__item" to="/m/address">
          收货地址
        </Link>
        <Link className="m-menu__item" to="/m/cart">
          购物车
        </Link>
        {isMerchant(user) && (
          <Link className="m-menu__item" to="/admin">
            店铺后台
          </Link>
        )}
      </div>

      <div className="m-menu">
        <button className="m-menu__item m-menu__item--danger m-menu__item--plain" onClick={() => void logout()}>
          退出登录
        </button>
      </div>

      <div style={{ padding: '4px 16px 28px', textAlign: 'center', color: '#b8afa4', fontSize: 11 }}>
        Owner Mall · 日用铺
      </div>
    </div>
  );
}
