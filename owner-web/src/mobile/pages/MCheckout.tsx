import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { addressApi, orderApi } from '@/api/mall';
import SafeImage from '@/components/SafeImage';
import { dt, money, num } from '@/utils/format';
import { toast } from '@/utils/feedback';
import type { Address, OrderPreview, OrderSubmitResult } from '@/types/mall';

interface Payload {
  shopId: string;
  items: { skuId: string; quantity: number; price: number }[];
}

export default function MCheckout() {
  const navigate = useNavigate();
  const [payload] = useState<Payload | null>(() => {
    const raw = sessionStorage.getItem('checkout:payload');
    return raw ? (JSON.parse(raw) as Payload) : null;
  });

  const [addresses, setAddresses] = useState<Address[]>([]);
  const [addressId, setAddressId] = useState('');
  const [pickedId, setPickedId] = useState('');
  const [addressSheet, setAddressSheet] = useState(false);
  const [couponSheet, setCouponSheet] = useState(false);
  const [preview, setPreview] = useState<OrderPreview | null>(null);
  const [couponUserId, setCouponUserId] = useState<string | null>(null);
  const [remark, setRemark] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [created, setCreated] = useState<OrderSubmitResult | null>(null);

  const loadPreview = useCallback(
    async (coupon?: string | null) => {
      if (!payload) return;
      const data = await orderApi.preview({
        shopId: payload.shopId,
        items: payload.items,
        couponUserId: coupon ?? null,
      });
      setPreview(data);
    },
    [payload],
  );

  useEffect(() => {
    if (!payload) {
      toast.warning('结算信息已失效，请重新选择商品');
      navigate('/m/cart', { replace: true });
    }
  }, [payload, navigate]);

  useEffect(() => {
    if (!payload) return;
    void (async () => {
      try {
        const list = await addressApi.list();
        setAddresses(list);
        const preferred = list.find((item) => item.isDefault === 1) ?? list[0];
        if (preferred) setAddressId(String(preferred.id));
        await loadPreview(null);
      } catch {
        /* 拦截器已提示 */
      } finally {
        setLoading(false);
      }
    })();
  }, [payload, loadPreview]);

  const current = addresses.find((item) => String(item.id) === addressId);

  const selectAddress = (id: string) => {
    setAddressId(id);
    setAddressSheet(false);
  };

  const selectCoupon = async (id: string | null) => {
    setCouponUserId(id);
    setCouponSheet(false);
    try {
      await loadPreview(id);
    } catch {
      /* 拦截器已提示 */
    }
  };

  const submit = async () => {
    if (!payload) return;
    if (!addressId) {
      toast.warning('请先选择收货地址');
      return;
    }
    if ((preview?.invalidItems ?? []).length > 0) {
      toast.warning('有商品已失效，请返回购物车处理');
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
      <div style={{ padding: '40px 20px' }}>
        <div style={{ textAlign: 'center' }}>
          <div
            style={{
              width: 64,
              height: 64,
              margin: '0 auto 16px',
              borderRadius: '50%',
              background: '#e8f7ee',
              color: '#16a34a',
              display: 'grid',
              placeItems: 'center',
              fontSize: 30,
            }}
          >
            ✓
          </div>
          <div style={{ fontSize: 18, fontWeight: 600 }}>订单已提交</div>
          <div className="m-sub" style={{ marginTop: 8, lineHeight: 1.7 }}>
            订单号 {created.orderNo}
            <br />
            应付 {money(created.payAmount)}
          </div>
        </div>

        <div className="m-card" style={{ marginTop: 20 }}>
          <div className="m-sub" style={{ lineHeight: 1.8 }}>
            商家确认收款后会安排发货，进度可在订单详情里查看。
            <br />
            本系统暂未接入在线支付，无需在手机上付款。
          </div>
        </div>

        <div style={{ marginTop: 20, display: 'flex', gap: 10 }}>
          <Link to={`/m/orders/${created.orderId}`} className="m-btn m-btn--primary m-btn--block">
            查看订单详情
          </Link>
          <Link to="/m" className="m-btn m-btn--block">
            继续逛逛
          </Link>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div>
        <div className="m-skeleton" />
        <div className="m-skeleton" />
      </div>
    );
  }

  return (
    <div style={{ paddingBottom: 84 }}>
      <button className="m-address" style={{ width: 'calc(100% - 24px)', border: 0, textAlign: 'left' }} onClick={() => {
        if (addresses.length === 0) {
          navigate('/m/address?from=checkout');
          return;
        }
        setPickedId(addressId);
        setAddressSheet(true);
      }}>
        {current ? (
          <>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#17130f" strokeWidth="1.7" style={{ marginTop: 2 }}>
              <path d="M12 21s7-5.4 7-11a7 7 0 1 0-14 0c0 5.6 7 11 7 11z" />
              <circle cx="12" cy="10" r="2.5" />
            </svg>
            <div className="m-address__main">
              <div className="m-address__who">
                <span>{current.receiver}</span>
                <span style={{ fontWeight: 400, color: '#6f675e' }}>{current.mobile}</span>
                {current.isDefault === 1 && <span className="m-tag">默认</span>}
              </div>
              <div className="m-address__text">{current.fullAddress}</div>
            </div>
          </>
        ) : (
          <div className="m-address__main">
            <div className="m-address__who">还没有收货地址</div>
            <div className="m-address__text">点这里添加一个收货地址</div>
          </div>
        )}
        <span style={{ color: '#b8afa4', fontSize: 18, alignSelf: 'center' }}>›</span>
      </button>

      <div className="m-card">
        <div className="m-sub" style={{ marginBottom: 4 }}>{preview?.shopName ?? '商品清单'}</div>
        {(preview?.items ?? []).map((item) => (
          <div key={item.skuId} className="m-cart-item" style={{ paddingLeft: 0, paddingRight: 0 }}>
            <SafeImage src={item.imageUrl} alt={item.productName} className="m-cart-item__img" style={{ width: 64, height: 64 }} />
            <div className="m-cart-item__body">
              <div className="m-cart-item__name m-clamp2">{item.productName}</div>
              <div className="m-cart-item__spec">{item.specName} × {item.quantity}</div>
              <div className="m-cart-item__foot">
                <span className="m-price">{money(num(item.price) * item.quantity)}</span>
              </div>
            </div>
          </div>
        ))}
        {(preview?.invalidItems ?? []).length > 0 && (
          <div style={{ marginTop: 10, color: '#dc2626', fontSize: 12, lineHeight: 1.6 }}>
            以下商品已失效：{(preview?.invalidItems ?? []).map((item) => item.productName).join('、')}
          </div>
        )}
      </div>

      <div className="m-card" style={{ padding: '4px 14px' }}>
        <button
          className="m-menu__item"
          style={{ paddingLeft: 0, paddingRight: 0 }}
          onClick={() => setCouponSheet(true)}
        >
          <span>优惠券</span>
          <span style={{ marginLeft: 'auto', color: '#6f675e', fontSize: 13, marginRight: 4 }}>
            {preview?.couponName
              ? preview.couponName
              : (preview?.usableCoupons ?? []).length > 0
                ? `${(preview?.usableCoupons ?? []).length} 张可用`
                : '暂无可用'}
          </span>
        </button>
        <div className="m-field" style={{ borderBottom: 0 }}>
          <span className="m-field__label">买家留言</span>
          <input
            className="m-input"
            value={remark}
            maxLength={200}
            onChange={(event) => setRemark(event.target.value)}
            placeholder="选填，想告诉店主的"
          />
        </div>
      </div>

      <div className="m-card">
        <div className="m-row m-row--between" style={{ fontSize: 13, color: '#4a443c' }}>
          <span>商品金额</span>
          <span>{money(preview?.goodsAmount ?? 0)}</span>
        </div>
        <div className="m-row m-row--between" style={{ fontSize: 13, color: '#4a443c', marginTop: 8 }}>
          <span>运费</span>
          <span>{money(preview?.freightAmount ?? 0)}</span>
        </div>
        <div className="m-row m-row--between" style={{ fontSize: 13, color: '#4a443c', marginTop: 8 }}>
          <span>优惠</span>
          <span>
            -{money(preview?.discountAmount ?? 0)}
            {preview?.couponName ? `（${preview.couponName}）` : ''}
          </span>
        </div>
        <div className="m-divider" />
        <div className="m-row m-row--between">
          <span>应付</span>
          <span className="m-price" style={{ fontSize: 22 }}>{money(preview?.payAmount ?? 0)}</span>
        </div>
        {preview?.message && <div className="m-mute" style={{ marginTop: 6 }}>{preview.message}</div>}
      </div>

      <div className="m-detailbar">
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
          <span className="m-mute">应付</span>
          <span className="m-price" style={{ fontSize: 20 }}>{money(preview?.payAmount ?? 0)}</span>
        </div>
        <button
          className="m-btn m-btn--primary"
          style={{ minWidth: 132 }}
          disabled={submitting || !addressId || (preview?.invalidItems ?? []).length > 0}
          onClick={() => void submit()}
        >
          提交订单
        </button>
      </div>

      {addressSheet && (
        <>
          <div className="m-mask" onClick={() => setAddressSheet(false)} />
          <div className="m-sheet">
            <button className="m-sheet__close" onClick={() => setAddressSheet(false)}>×</button>
            <div className="m-sheet__title">选择收货地址</div>
            {addresses.map((item) => (
              <div
                key={item.id}
                className={`m-picker-item${String(item.id) === pickedId ? ' is-active' : ''}`}
                onClick={() => setPickedId(String(item.id))}
              >
                <div className="m-address__main">
                  <div className="m-address__who">
                    <span>{item.receiver}</span>
                    <span style={{ fontWeight: 400, color: '#6f675e', fontSize: 13 }}>{item.mobile}</span>
                    {item.isDefault === 1 && <span className="m-tag">默认</span>}
                  </div>
                  <div className="m-address__text">{item.fullAddress}</div>
                </div>
              </div>
            ))}
            <div style={{ display: 'flex', gap: 10, marginTop: 12 }}>
              <Link to="/m/address?from=checkout" className="m-btn m-btn--block">
                管理地址
              </Link>
              <button className="m-btn m-btn--primary m-btn--block" onClick={() => selectAddress(pickedId)}>
                使用该地址
              </button>
            </div>
          </div>
        </>
      )}

      {couponSheet && (
        <>
          <div className="m-mask" onClick={() => setCouponSheet(false)} />
          <div className="m-sheet">
            <button className="m-sheet__close" onClick={() => setCouponSheet(false)}>×</button>
            <div className="m-sheet__title">选择优惠券</div>
            <div
              className={`m-picker-item${couponUserId === null ? ' is-active' : ''}`}
              onClick={() => void selectCoupon(null)}
            >
              不使用优惠券
            </div>
            {(preview?.usableCoupons ?? []).map((coupon) => (
              <div
                key={coupon.id}
                className={`m-picker-item${couponUserId === String(coupon.id) ? ' is-active' : ''}`}
                onClick={() => void selectCoupon(String(coupon.id))}
              >
                <div>
                  <div style={{ fontSize: 14, fontWeight: 600 }}>{coupon.name}</div>
                  <div className="m-mute" style={{ marginTop: 4 }}>
                    满 {money(coupon.thresholdAmount)} 减 {money(coupon.reduceAmount)}
                  </div>
                </div>
              </div>
            ))}
            {(preview?.usableCoupons ?? []).length === 0 && <div className="m-empty" style={{ padding: 24 }}>本单没有可用的优惠券</div>}
          </div>
        </>
      )}
    </div>
  );
}
