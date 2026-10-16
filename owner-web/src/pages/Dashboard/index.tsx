import { useCallback, useEffect, useState } from 'react';
import { Button } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { fetchPortalOverview } from '@/api/portal';
import type { PortalOverview } from '@/types/api';

export default function Dashboard() {
  const [overview, setOverview] = useState<PortalOverview | null>(null);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setOverview(await fetchPortalOverview(14));
    } catch {
      // 拦截器已提示
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <div className="owner-page">
      <section className="shop-hero">
        <p className="shop-kicker">生活日用品</p>
        <h2>把日常用得干净一点</h2>
        <p>家居清洁、厨房、收纳、个护和小百货。先把店面气质定下来，商品和订单按规划往后加。</p>
      </section>

      <section className="owner-stat-grid">
        <article className="owner-tile">
          <span className="owner-tile__label">门户用户</span>
          <strong className="owner-tile__value">{overview?.portalUserCount ?? 0}</strong>
          <span className="owner-tile__note">当前账号</span>
        </article>
        <article className="owner-tile">
          <span className="owner-tile__label">在售商品</span>
          <strong className="owner-tile__value">0</strong>
          <span className="owner-tile__note">尚未建档</span>
        </article>
        <article className="owner-tile">
          <span className="owner-tile__label">待处理订单</span>
          <strong className="owner-tile__value">0</strong>
          <span className="owner-tile__note">尚未开单</span>
        </article>
      </section>

      <article className="owner-panel">
        <div className="owner-panel__head">
          <h2 className="owner-panel__title">这一季</h2>
          <Button icon={<ReloadOutlined />} loading={loading} onClick={() => void load()}>
            刷新
          </Button>
        </div>
        <ul className="shop-points">
          <li>类目收在生活日用，不做大而全的商城。</li>
          <li>一家店先服务自己的货，数据按租户分开。</li>
          <li>店主、店员、顾客分开，后面再接真实登录。</li>
        </ul>
      </article>
    </div>
  );
}
