import { useEffect, useState } from 'react';
import { Button, Card, Skeleton, Space, Statistic } from 'antd';
import { useNavigate } from 'react-router-dom';
import { shopApi } from '@/api/admin';
import { useAuthStore } from '@/store/auth';
import { money } from '@/utils/format';
import type { ShopDashboard } from '@/types/mall';

export default function Dashboard() {
  const navigate = useNavigate();
  const shopId = useAuthStore((state) => state.currentShopId) ?? '';
  const [data, setData] = useState<ShopDashboard | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!shopId) return;
    let alive = true;
    void shopApi
      .dashboard(shopId)
      .then((d) => alive && setData(d))
      .catch(() => undefined)
      .finally(() => alive && setLoading(false));
    return () => {
      alive = false;
    };
  }, [shopId]);

  if (loading) return <Skeleton active />;

  const cards = [
    { title: '今日订单', value: data?.todayOrderCount ?? 0, suffix: '单', to: '/admin/orders' },
    { title: '今日收款', value: money(data?.todayPaidAmount ?? 0), to: '/admin/orders' },
    { title: '待发货', value: data?.pendingShipmentCount ?? 0, suffix: '单', to: '/admin/orders?status=PENDING_SHIPMENT' },
    { title: '待处理售后', value: data?.pendingRefundCount ?? 0, suffix: '单', to: '/admin/refunds' },
    { title: '在售商品', value: data?.onSaleProductCount ?? 0, suffix: '个', to: '/admin/products' },
  ];

  return (
    <div>
      <h2 className="mall-section-title">店铺看板</h2>

      <div className="admin-stat-grid">
        {cards.map((c) => (
          <Card key={c.title} hoverable onClick={() => navigate(c.to)}>
            <Statistic title={c.title} value={c.value} suffix={c.suffix} />
          </Card>
        ))}
      </div>

      <Card title="常用操作" style={{ marginTop: 20 }}>
        <Space wrap>
          <Button type="primary" onClick={() => navigate('/admin/products')}>
            新建商品
          </Button>
          <Button onClick={() => navigate('/admin/orders?status=PENDING_SHIPMENT')}>去发货</Button>
          <Button onClick={() => navigate('/admin/refunds')}>处理售后</Button>
          <Button onClick={() => navigate('/admin/stock')}>调库存</Button>
          <Button onClick={() => navigate('/admin/coupons')}>发优惠券</Button>
          <Button onClick={() => navigate('/admin/shop')}>店铺设置</Button>
        </Space>
      </Card>

      <Card title="经营提醒" style={{ marginTop: 20 }}>
        <ul style={{ margin: 0, paddingLeft: 18, color: '#4a443c', lineHeight: 2 }}>
          <li>商品只有在「至少 1 张主图 + 至少 1 个启用且可售的规格」时才允许上架，否则会被自动降级为草稿。</li>
          <li>有订单关联的商品不能删除，只能下架；库存用带条件的原子更新扣减，不会超卖。</li>
          <li>售后按整单处理：未发货可仅退款，已发货 7 天内可退货退款，拒绝时必须填写理由。</li>
          <li>系统没有接入在线支付：顾客下单后订单停在「待确认」，商家线下收到钱后在订单页点「确认收款」，才会转待发货。</li>
        </ul>
      </Card>
    </div>
  );
}
