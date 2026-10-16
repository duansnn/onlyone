import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Divider,
  Empty,
  Input,
  Radio,
  Result,
  Skeleton,
  Space,
  Tag,
} from 'antd';
import { useNavigate } from 'react-router-dom';
import { addressApi, orderApi, payApi } from '@/api/mall';
import { toast } from '@/utils/feedback';
import { dt, money, num, PLACEHOLDER } from '@/utils/format';
import type { Address, OrderPreview, OrderSubmitResult, PaymentCreate } from '@/types/mall';

interface Payload {
  shopId: string;
  items: { skuId: string; quantity: number; price: number }[];
}

const CHANNELS = [
  { value: 'WECHAT', label: '微信支付' },
  { value: 'ALIPAY', label: '支付宝' },
];

export default function Checkout() {
  const navigate = useNavigate();
  const [payload] = useState<Payload | null>(() => {
    const raw = sessionStorage.getItem('checkout:payload');
    return raw ? (JSON.parse(raw) as Payload) : null;
  });
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [addressId, setAddressId] = useState<string>('');
  const [preview, setPreview] = useState<OrderPreview | null>(null);
  const [couponUserId, setCouponUserId] = useState<string | null>(null);
  const [remark, setRemark] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [channel, setChannel] = useState('WECHAT');
  const [created, setCreated] = useState<OrderSubmitResult | null>(null);
  const [payment, setPayment] = useState<PaymentCreate | null>(null);
  const [paying, setPaying] = useState(false);

  useEffect(() => {
    if (!payload) {
      toast.warning('结算信息已失效，请重新选择商品');
      navigate('/cart', { replace: true });
    }
  }, [payload, navigate]);

  const loadPreview = useCallback(
    async (coupon?: string | null) => {
      if (!payload) return;
      const data = await orderApi.preview({
        shopId: payload.shopId,
        items: payload.items,
        couponUserId: coupon ?? null,
      });
      setPreview(data);
      return data;
    },
    [payload],
  );

  useEffect(() => {
    if (!payload) return;
    void (async () => {
      try {
        const list = await addressApi.list();
        setAddresses(list);
        const preferred = list.find((a) => a.isDefault === 1) ?? list[0];
        if (preferred) setAddressId(String(preferred.id));
        await loadPreview(null);
      } catch {
        /* 拦截器已提示 */
      } finally {
        setLoading(false);
      }
    })();
  }, [payload, loadPreview]);

  const onSelectCoupon = async (value: string | null) => {
    setCouponUserId(value);
    try {
      await loadPreview(value);
    } catch {
      /* 拦截器已提示 */
    }
  };

  const submit = async () => {
    if (!payload) return;
    if (!addressId) {
      toast.warning('请选择收货地址');
      return;
    }
    if ((preview?.invalidItems ?? []).length > 0) {
      toast.warning('购物车里有失效商品，请返回处理');
      return;
    }
    setSubmitting(true);
    try {
      const result = await orderApi.submit({
        shopId: payload.shopId,
        items: payload.items,
        addressId,
        couponUserId,
        buyerRemark: remark || undefined,
      });
      setCreated(result);
      sessionStorage.removeItem('checkout:payload');
    } catch {
      /* 拦截器已提示 */
    } finally {
      setSubmitting(false);
    }
  };

  const doPay = async () => {
    if (!created) return;
    setPaying(true);
    try {
      const result = await payApi.create(String(created.orderId), channel);
      setPayment(result);
      if (!result.payable) {
        toast.warning(result.message || '支付通道未开通，订单已保留在待付款');
      }
    } catch {
      /* 拦截器已提示 */
    } finally {
      setPaying(false);
    }
  };

  /** 本地联调：等价于微信/支付宝支付成功后的异步回调 */
  const mockNotify = async () => {
    if (!created) return;
    try {
      await payApi.mockCallback(created.orderNo);
      toast.success('支付成功');
      navigate('/orders');
    } catch {
      /* 拦截器已提示 */
    }
  };

  if (!payload) return null;

  if (created) {
    return (
      <Card>
        <Result
          status="success"
          title="订单已提交"
          subTitle={`订单号 ${created.orderNo} · 应付 ${money(created.payAmount)} · ${dt(created.expireTime)} 前未支付将自动取消`}
          extra={[
            <Button key="orders" onClick={() => navigate('/orders')}>
              查看我的订单
            </Button>,
          ]}
        />

        <Divider />

        <div style={{ maxWidth: 420, margin: '0 auto' }}>
          <div className="mall-card__meta" style={{ marginBottom: 8 }}>
            支付方式
          </div>
          <Radio.Group
            value={channel}
            options={CHANNELS}
            optionType="button"
            buttonStyle="solid"
            onChange={(e) => setChannel(e.target.value)}
          />
          <div style={{ marginTop: 16 }}>
            <Button type="primary" block loading={paying} onClick={() => void doPay()}>
              发起支付（{money(created.payAmount)}）
            </Button>
          </div>

          {payment && (
            <Alert
              style={{ marginTop: 16 }}
              type={payment.payable ? 'success' : 'warning'}
              showIcon
              message={payment.payable ? `已创建支付单（${payment.channelLabel}）` : '支付通道未开通'}
              description={
                payment.payable
                  ? `商户单号 ${payment.outTradeNo ?? '-'}。真实环境请按通道返回的支付参数在客户端唤起收银台。`
                  : payment.message ||
                    '未配置商户号，订单会留在待付款状态。系统不会假装收款成功，也不提供“我已付款”按钮。'
              }
            />
          )}

          {payment?.payable && (
            <Alert
              style={{ marginTop: 12 }}
              type="info"
              showIcon
              message="本地联调"
              description="下面这个按钮直接调用后端的模拟回调接口，等价于真实通道支付成功后的通知，仅在开发环境建议使用。"
              action={
                <Button size="small" onClick={() => void mockNotify()}>
                  模拟支付成功
                </Button>
              }
            />
          )}
        </div>
      </Card>
    );
  }

  if (loading) return <Skeleton active />;

  return (
    <div>
      <h2 className="mall-section-title">确认订单</h2>

      {addresses.length === 0 ? (
        <Card style={{ marginBottom: 16 }}>
          <Empty description="还没有收货地址">
            <Button type="primary" onClick={() => navigate('/account?tab=address')}>
              去添加地址
            </Button>
          </Empty>
        </Card>
      ) : (
        <Card title="收货地址" style={{ marginBottom: 16 }}>
          <Radio.Group
            style={{ width: '100%' }}
            value={addressId}
            onChange={(e) => setAddressId(e.target.value)}
          >
            <Space direction="vertical" style={{ width: '100%' }}>
              {addresses.map((a) => (
                <Radio key={a.id} value={String(a.id)}>
                  <Space size={8}>
                    <strong>{a.receiver}</strong>
                    <span>{a.mobile}</span>
                    <span className="mall-card__meta">{a.fullAddress}</span>
                    {a.isDefault === 1 && <Tag color="blue">默认</Tag>}
                  </Space>
                </Radio>
              ))}
            </Space>
          </Radio.Group>
        </Card>
      )}

      <Card title={preview?.shopName ?? '商品清单'} style={{ marginBottom: 16 }}>
        {(preview?.items ?? []).map((item) => (
          <div key={item.skuId} style={{ display: 'flex', gap: 12, alignItems: 'center', padding: '8px 0' }}>
            <img
              src={item.imageUrl || PLACEHOLDER}
              alt=""
              style={{ width: 56, height: 56, borderRadius: 10, objectFit: 'cover', background: '#f5f5f5' }}
            />
            <div style={{ flex: 1 }}>
              <div>{item.productName}</div>
              <div className="mall-card__meta">{item.specName} × {item.quantity}</div>
            </div>
            <span className="mall-price">{money(num(item.price) * item.quantity)}</span>
          </div>
        ))}

        {(preview?.invalidItems ?? []).length > 0 && (
          <Alert
            style={{ marginTop: 12 }}
            type="error"
            showIcon
            message="以下商品已失效，请返回购物车移除后再提交"
            description={(preview?.invalidItems ?? [])
              .map((i) => `${i.productName}（${i.specName}）：${i.invalidReason}`)
              .join('；')}
          />
        )}
      </Card>

      <Card title="优惠券" style={{ marginBottom: 16 }}>
        {(preview?.usableCoupons ?? []).length === 0 ? (
          <span className="mall-card__meta">本单没有可用的优惠券</span>
        ) : (
          <Radio.Group value={couponUserId} onChange={(e) => void onSelectCoupon(e.target.value ?? null)}>
            <Space direction="vertical">
              <Radio value={null}>不使用优惠券</Radio>
              {(preview?.usableCoupons ?? []).map((c) => (
                <Radio key={c.id} value={String(c.id)}>
                  {c.name} · 满 {money(c.thresholdAmount)} 减 {money(c.reduceAmount)}
                </Radio>
              ))}
            </Space>
          </Radio.Group>
        )}
      </Card>

      <Card title="买家留言" style={{ marginBottom: 16 }}>
        <Input.TextArea
          rows={2}
          maxLength={200}
          value={remark}
          placeholder="有什么想告诉店主的？（选填）"
          onChange={(e) => setRemark(e.target.value)}
        />
      </Card>

      <Card>
        <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
          <Space direction="vertical" align="end" size={4}>
            <span className="mall-card__meta">商品金额 {money(preview?.goodsAmount ?? 0)}</span>
            <span className="mall-card__meta">运费 {money(preview?.freightAmount ?? 0)}</span>
            <span className="mall-card__meta">
              优惠 -{money(preview?.discountAmount ?? 0)}
              {preview?.couponName ? `（${preview.couponName}）` : ''}
            </span>
            <span style={{ fontSize: 16 }}>
              应付 <span className="mall-price" style={{ fontSize: 24 }}>{money(preview?.payAmount ?? 0)}</span>
            </span>
            {preview?.message && <span className="mall-card__meta">{preview.message}</span>}
            <Button
              type="primary"
              size="large"
              loading={submitting}
              disabled={!addressId || (preview?.invalidItems ?? []).length > 0}
              onClick={() => void submit()}
            >
              提交订单
            </Button>
          </Space>
        </div>
      </Card>
    </div>
  );
}
