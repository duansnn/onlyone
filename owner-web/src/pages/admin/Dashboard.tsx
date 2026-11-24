import { useEffect, useRef, useState } from 'react';
import { Button, Card, Skeleton, Space, Statistic } from 'antd';
import {
  AppstoreOutlined,
  ArrowDownOutlined,
  ArrowUpOutlined,
  AuditOutlined,
  DollarOutlined,
  InboxOutlined,
  ShoppingOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import dayjs from 'dayjs';
import * as echarts from 'echarts';
import { shopApi } from '@/api/admin';
import { useAuthStore } from '@/store/auth';
import { money } from '@/utils/format';
import type { ShopAnalytics, ShopDashboard } from '@/types/mall';

/** 环比涨跌标签：昨天有数、今天没数算「降」，反之算「升」；都没数显示持平 */
function delta(before: number, after: number) {
  if (after === before) return { cls: 'is-flat', icon: null, text: '持平' };
  if (after > before) {
    const diff = before === 0 ? null : `${Math.round(((after - before) / before) * 100)}%`;
    return { cls: 'is-up', icon: <ArrowUpOutlined />, text: diff ? `+${diff}` : '新增' };
  }
  const diff = before === 0 ? null : `${Math.round(((before - after) / before) * 100)}%`;
  return { cls: 'is-down', icon: <ArrowDownOutlined />, text: diff ? `-${diff}` : '归零' };
}

/** 复用同一套图表的主题色，跟店面暖橙主色对齐 */
const CHART_COLORS = ['#ff5a1f', '#f0a202', '#2f9e6b', '#3b82c4', '#8b5cf6', '#f36d8f'];

/** 金额（分）转元，保留 2 位 */
function yuan(fen: number): string {
  return (fen / 100).toFixed(2);
}

function useChart() {
  const ref = useRef<HTMLDivElement | null>(null);
  const chartRef = useRef<echarts.ECharts | null>(null);

  const render = (option: echarts.EChartsOption) => {
    const el = ref.current;
    if (!el) return;
    if (!chartRef.current) {
      chartRef.current = echarts.init(el);
    }
    chartRef.current.setOption(option, true);
  };

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const ro = new ResizeObserver(() => chartRef.current?.resize());
    ro.observe(el);
    return () => {
      ro.disconnect();
      chartRef.current?.dispose();
      chartRef.current = null;
    };
  }, []);

  return { ref, render };
}

export default function Dashboard() {
  const navigate = useNavigate();
  const shopId = useAuthStore((state) => state.currentShopId) ?? '';
  const [data, setData] = useState<ShopDashboard | null>(null);
  const [analytics, setAnalytics] = useState<ShopAnalytics | null>(null);
  const [loading, setLoading] = useState(true);

  const trendChart = useChart();
  const topChart = useChart();
  const categoryChart = useChart();

  useEffect(() => {
    if (!shopId) return;
    let alive = true;
    setLoading(true);
    Promise.all([shopApi.dashboard(shopId), shopApi.analytics(shopId)])
      .then(([d, a]) => {
        if (!alive) return;
        setData(d);
        setAnalytics(a);
      })
      .catch(() => undefined)
      .finally(() => alive && setLoading(false));
    return () => {
      alive = false;
    };
  }, [shopId]);

  useEffect(() => {
    if (!analytics) return;
    const days = analytics.trend.map((t) => t.day.slice(5)); // MM-DD
    trendChart.render({
      tooltip: { trigger: 'axis' },
      legend: { data: ['销售额', '订单数'], top: 0, right: 0, textStyle: { color: '#4a443c' } },
      grid: { left: 52, right: 52, top: 36, bottom: 32 },
      xAxis: {
        type: 'category',
        data: days,
        boundaryGap: false,
        axisLabel: { color: '#8a8278', interval: 4, hideOverlap: true },
        axisTick: { alignWithLabel: true },
      },
      yAxis: [
        {
          type: 'value',
          name: '销售额(元)',
          nameTextStyle: { color: '#8a8278' },
          axisLabel: { color: '#8a8278' },
          splitLine: { lineStyle: { color: '#eee9e2' } },
        },
        {
          type: 'value',
          name: '订单数',
          nameTextStyle: { color: '#8a8278' },
          axisLabel: { color: '#8a8278' },
          minInterval: 1,
          splitLine: { show: false },
        },
      ],
      series: [
        {
          name: '销售额',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 5,
          data: analytics.trend.map((t) => Number(yuan(t.paidAmount))),
          itemStyle: { color: CHART_COLORS[0] },
          areaStyle: { color: 'rgba(255,90,31,0.08)' },
        },
        {
          name: '订单数',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 5,
          yAxisIndex: 1,
          data: analytics.trend.map((t) => t.orderCount),
          itemStyle: { color: CHART_COLORS[3] },
        },
      ],
    });

    const top = analytics.topProducts.slice(0, 10);
    topChart.render({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: 8, right: 16, top: 12, bottom: 8, containLabel: true },
      xAxis: {
        type: 'value',
        axisLabel: { color: '#8a8278' },
        splitLine: { lineStyle: { color: '#eee9e2' } },
      },
      yAxis: {
        type: 'category',
        inverse: true,
        data: top.map((t) => t.productName),
        axisLabel: { color: '#4a443c', width: 96, overflow: 'truncate' },
      },
      series: [
        {
          name: '销量',
          type: 'bar',
          data: top.map((t) => t.quantity),
          itemStyle: { color: CHART_COLORS[0], borderRadius: [0, 4, 4, 0] },
          barMaxWidth: 16,
        },
      ],
    });

    categoryChart.render({
      tooltip: { trigger: 'item', formatter: '{b}: {c} 元 ({d}%)' },
      legend: { bottom: 0, textStyle: { color: '#4a443c' } },
      series: [
        {
          name: '类目销售',
          type: 'pie',
          radius: ['42%', '68%'],
          center: ['50%', '45%'],
          data: analytics.categorySales.map((c) => ({
            name: c.categoryName,
            value: Number(yuan(c.amount)),
          })),
          color: CHART_COLORS,
          label: { color: '#4a443c' },
        },
      ],
    });
  }, [analytics, trendChart, topChart, categoryChart]);

  if (loading) return <Skeleton active />;

  const cards = [
    {
      title: '今日订单',
      value: data?.todayOrderCount ?? 0,
      suffix: '单',
      hint: '今天 0 点后创建的订单',
      icon: <ShoppingOutlined />,
      tone: '',
      delta: delta(data?.yesterdayOrderCount ?? 0, data?.todayOrderCount ?? 0),
      to: `/admin/orders?from=${dayjs().format('YYYY-MM-DD')}&to=${dayjs().format('YYYY-MM-DD')}`,
    },
    {
      title: '今日收款',
      value: money(data?.todayPaidAmount ?? 0),
      hint: '今天确认收款的金额',
      icon: <DollarOutlined />,
      tone: 'is-blue',
      delta: delta(data?.yesterdayPaidAmount ?? 0, data?.todayPaidAmount ?? 0),
      to: `/admin/orders?from=${dayjs().format('YYYY-MM-DD')}&to=${dayjs().format('YYYY-MM-DD')}`,
    },
    {
      title: '待发货',
      value: data?.pendingShipmentCount ?? 0,
      suffix: '单',
      hint: '已收款但还没填运单',
      icon: <InboxOutlined />,
      tone: 'is-amber',
      delta: null,
      to: '/admin/orders?status=PENDING_SHIPMENT',
    },
    {
      title: '待处理售后',
      value: data?.pendingRefundCount ?? 0,
      suffix: '单',
      hint: '待审批 / 已同意 / 待退货',
      icon: <AuditOutlined />,
      tone: 'is-violet',
      delta: null,
      to: '/admin/refunds?status=OPEN',
    },
    {
      title: '在售商品',
      value: data?.onSaleProductCount ?? 0,
      suffix: '个',
      hint: '状态为在售的商品',
      icon: <AppstoreOutlined />,
      tone: 'is-green',
      delta: null,
      to: '/admin/products',
    },
  ];

  return (
    <div>
      <h2 className="mall-section-title">店铺看板</h2>

      <div className="admin-stat-grid">
        {cards.map((c) => (
          <Card
            key={c.title}
            hoverable
            className="admin-stat-card"
            onClick={() => navigate(c.to)}
            styles={{ body: { padding: '20px 22px' } }}
          >
            <span className={`admin-stat-card__icon ${c.tone}`}>{c.icon}</span>
            <div style={{ marginTop: 12, fontSize: 13, color: '#4a443c' }}>{c.title}</div>
            <div className="admin-stat-card__value-row">
              <Statistic value={c.value} suffix={c.suffix} valueStyle={{ fontSize: 28, fontWeight: 600 }} />
              {c.delta && (
                <span className={`admin-stat-card__delta ${c.delta.cls}`}>
                  {c.delta.icon}
                  {c.delta.text}
                </span>
              )}
            </div>
            {c.hint && (
              <div
                className="mall-card__meta"
                style={{ marginTop: 6, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}
              >
                {c.hint}
              </div>
            )}
          </Card>
        ))}
      </div>

      <Card title="近 30 天销售趋势" style={{ marginTop: 20 }}>
        <div ref={trendChart.ref} style={{ height: 280 }} />
      </Card>

      <div style={{ display: 'flex', gap: 20, marginTop: 20, flexWrap: 'wrap' }}>
        <Card title="热销商品 Top10" style={{ flex: '1 1 340px' }}>
          <div ref={topChart.ref} style={{ height: 300 }} />
        </Card>
        <Card title="类目销售占比" style={{ flex: '1 1 340px' }}>
          <div ref={categoryChart.ref} style={{ height: 300 }} />
        </Card>
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
