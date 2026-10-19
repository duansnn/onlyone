import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Divider,
  Empty,
  Input,
  Modal,
  Radio,
  Result,
  Skeleton,
  Space,
  Tag,
} from 'antd';
import { useNavigate } from 'react-router-dom';
import { addressApi, orderApi } from '@/api/mall';
import SafeImage from '@/components/SafeImage';
import { toast } from '@/utils/feedback';
import { dt, money, num } from '@/utils/format';
import type { Address, OrderPreview, OrderSubmitResult } from '@/types/mall';

interface Payload {
  shopId: string;
  items: { skuId: string; quantity: number; price: number }[];
}

export default function Checkout() {
  const navigate = useNavigate();
  const [payload] = useState<Payload | null>(() => {
    const raw = sessionStorage.getItem('checkout:payload');
    return raw ? (JSON.parse(raw) as Payload) : null;
  });
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [addressId, setAddressId] = useState<string>('');
  const [addressPickerOpen, setAddressPickerOpen] = useState(false);
  /** 弹窗里的临时选择，点「确定」才写回 addressId */
  const [pickedId, setPickedId] = useState<string>('');
  const [preview, setPreview] = useState<OrderPreview | null>(null);
  const [couponUserId, setCouponUserId] = useState<string | null>(null);
  const [remark, setRemark] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [created, setCreated] = useState<OrderSubmitResult | null>(null);

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

  const currentAddress = addresses.find((a) => String(a.id) === addressId);

  const openAddressPicker = () => {
    setPickedId(addressId);
    setAddressPickerOpen(true);
  };

  const confirmAddress = () => {
    if (!pickedId) {
      toast.warning('请选择一个收货地址');
      return;
    }
    setAddressId(pickedId);
    setAddressPickerOpen(false);
  };

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

  if (!payload) return null;

  if (created) {
    return (
      <Card>
        <Result
          status="success"
          title="订单已提交"
          subTitle={`订单号 ${created.orderNo} · 应付 ${money(created.payAmount)} · 商家确认收款后安排发货`}
          extra={[
            <Button key="orders" onClick={() => navigate('/orders')}>
              查看我的订单
            </Button>,
          ]}
        />

        <Divider />

        <div style={{ maxWidth: 420, margin: '0 auto' }}>
          <Alert
            type="info"
            showIcon
            message="暂未接入在线支付"
            description="系统当前不提供微信支付 / 支付宝收款，你无需在页面上付款。商家确认收款后会安排发货，进度可在「我的订单」里查看。"
          />
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
        <Card
          title="收货地址"
          style={{ marginBottom: 16 }}
          extra={
            <Button size="small" onClick={openAddressPicker}>
              切换地址
            </Button>
          }
        >
          {currentAddress ? (
            <div className="mall-address-current">
              <Space size={8} wrap>
                <strong>{currentAddress.receiver}</strong>
                <span>{currentAddress.mobile}</span>
                {currentAddress.isDefault === 1 && <Tag color="blue">默认</Tag>}
              </Space>
              <div className="mall-card__meta" style={{ marginTop: 4 }}>
                {currentAddress.fullAddress}
              </div>
            </div>
          ) : (
            <div className="mall-card__meta">尚未选择收货地址</div>
          )}
        </Card>
      )}

      <Modal
        open={addressPickerOpen}
        title="选择收货地址"
        onCancel={() => setAddressPickerOpen(false)}
        onOk={confirmAddress}
        okText="使用该地址"
        width={560}
        destroyOnHidden
      >
        <Radio.Group
          style={{ width: '100%', maxHeight: 380, overflowY: 'auto', paddingRight: 4 }}
          value={pickedId}
          onChange={(e) => setPickedId(e.target.value)}
        >
          <Space direction="vertical" size={4} style={{ width: '100%' }}>
            {addresses.map((a) => (
              <div
                key={a.id}
                className={`mall-address-option${String(a.id) === pickedId ? ' is-active' : ''}`}
                onClick={() => setPickedId(String(a.id))}
              >
                <Radio value={String(a.id)}>
                  <Space size={8} wrap>
                    <strong>{a.receiver}</strong>
                    <span>{a.mobile}</span>
                    {a.isDefault === 1 && <Tag color="blue">默认</Tag>}
                  </Space>
                  <div className="mall-card__meta" style={{ marginTop: 2 }}>
                    {a.fullAddress}
                  </div>
                </Radio>
              </div>
            ))}
          </Space>
        </Radio.Group>

        <Button
          type="link"
          style={{ paddingLeft: 0, marginTop: 8 }}
          onClick={() => navigate('/account?tab=address')}
        >
          + 新增 / 管理收货地址
        </Button>
      </Modal>

      <Card title={preview?.shopName ?? '商品清单'} style={{ marginBottom: 16 }}>
        {(preview?.items ?? []).map((item) => (
          <div key={item.skuId} style={{ display: 'flex', gap: 12, alignItems: 'center', padding: '8px 0' }}>
            <SafeImage
              src={item.imageUrl}
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
