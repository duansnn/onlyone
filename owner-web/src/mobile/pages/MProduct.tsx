import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { cartApi, storeApi } from '@/api/mall';
import SafeImage from '@/components/SafeImage';
import { useAuthStore } from '@/store/auth';
import { useShopStore } from '@/store/shop';
import { dt, money, num, plainText } from '@/utils/format';
import { toast } from '@/utils/feedback';
import type { ProductDetail, Review } from '@/types/mall';

export default function MProduct() {
  const { productId } = useParams<{ productId: string }>();
  const navigate = useNavigate();
  const shopId = useShopStore((state) => state.shopId);
  const token = useAuthStore((state) => state.token);
  const shops = useAuthStore((state) => state.shops);

  const [detail, setDetail] = useState<ProductDetail | null>(null);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [skuId, setSkuId] = useState('');
  const [quantity, setQuantity] = useState(1);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!shopId || !productId) return;
    let alive = true;
    setLoading(true);
    void Promise.all([
      storeApi.detail(shopId, productId),
      storeApi.reviews(shopId, productId, { pageNum: 1, pageSize: 5 }).catch(() => null),
    ])
      .then(([d, r]) => {
        if (!alive) return;
        setDetail(d);
        const first = d.skus?.find((s) => s.status === 0 && s.availableStock > 0) ?? d.skus?.[0];
        setSkuId(first ? String(first.id) : '');
        setReviews(r?.rows ?? []);
      })
      .catch(() => undefined)
      .finally(() => alive && setLoading(false));
    return () => {
      alive = false;
    };
  }, [shopId, productId]);

  const sku = detail?.skus.find((s) => String(s.id) === skuId);
  const sellable = Boolean(sku && sku.status === 0 && sku.availableStock > 0);
  const maxQty = Math.max(1, sku?.availableStock ?? 1);

  /** 本店店主/店员不能给自己店铺下单（后端 submit 会做同样的拦截） */
  const myShopRole = shops.find((s) => String(s.shopId) === String(shopId))?.role;
  const selfPurchaseBlocked = Boolean(token && myShopRole);

  const requireLogin = () => {
    if (token) return false;
    toast.warning('请先登录');
    navigate('/m/login', { state: { from: `/m/product/${productId}` } });
    return true;
  };

  const addToCart = async () => {
    if (requireLogin()) return;
    if (selfPurchaseBlocked) {
      toast.warning(myShopRole === 'OWNER' ? '店主账号不能在自己店铺下单' : '店员账号不能在自己店铺下单');
      return;
    }
    if (!sku || !sellable) {
      toast.warning('请选择有货的规格');
      return;
    }
    setBusy(true);
    try {
      await cartApi.add({ skuId: String(sku.id), quantity });
      toast.success('已加入购物车');
    } catch {
      /* 拦截器已提示 */
    } finally {
      setBusy(false);
    }
  };

  const buyNow = () => {
    if (requireLogin()) return;
    if (selfPurchaseBlocked) {
      toast.warning(myShopRole === 'OWNER' ? '店主账号不能在自己店铺下单' : '店员账号不能在自己店铺下单');
      return;
    }
    if (!detail || !sku || !sellable) {
      toast.warning('请选择有货的规格');
      return;
    }
    sessionStorage.setItem(
      'checkout:payload',
      JSON.stringify({
        shopId: detail.shopId,
        items: [{ skuId: String(sku.id), quantity, price: num(sku.price) }],
      }),
    );
    navigate('/m/checkout');
  };

  if (loading) {
    return (
      <div>
        <div style={{ width: '100%', aspectRatio: '1 / 1', background: '#f0f0f0' }} />
        <div className="m-skeleton" />
      </div>
    );
  }

  if (!detail) {
    return (
      <div className="m-empty">
        商品不存在或已下架
        <div style={{ marginTop: 16 }}>
          <button className="m-btn m-btn--sm" onClick={() => navigate('/m/list')}>
            去逛逛别的
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="m-body--nobar" style={{ paddingBottom: 96 }}>
      <div className="m-gallery">
        {(detail.images?.length ? detail.images : ['']).map((img, index) => (
          <SafeImage key={`${img}-${index}`} src={img} alt={detail.name} />
        ))}
      </div>

      <div style={{ background: '#fff' }}>
        <div className="m-detail__pricebar">
          <span className="m-detail__price">{money(sku ? sku.price : detail.minPrice)}</span>
          <span style={{ marginLeft: 'auto' }} className="m-mute">
            {detail.totalAvailable > 0 ? `可售 ${detail.totalAvailable} 件` : '暂时缺货'}
          </span>
        </div>
        <div className="m-detail__name">{detail.name}</div>
        {detail.sellingPoint && <div className="m-detail__point">{detail.sellingPoint}</div>}
      </div>

      <div className="m-card">
        <div className="m-sub" style={{ marginBottom: 10 }}>选择规格</div>
        <div className="m-specs">
          {(detail.skus ?? []).map((item) => {
            const ok = item.status === 0 && item.availableStock > 0;
            const active = String(item.id) === skuId;
            return (
              <button
                key={item.id}
                className={`m-spec${active ? ' is-active' : ''}`}
                disabled={!ok}
                onClick={() => {
                  setSkuId(String(item.id));
                  setQuantity(1);
                }}
              >
                {item.specName}
              </button>
            );
          })}
        </div>

        <div className="m-divider" />

        <div className="m-row m-row--between">
          <span className="m-sub">数量</span>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <span className="m-mute">库存 {sku?.availableStock ?? 0}</span>
            <div className="m-stepper">
              <button disabled={quantity <= 1} onClick={() => setQuantity((v) => Math.max(1, v - 1))}>
                −
              </button>
              <span>{quantity}</span>
              <button disabled={quantity >= maxQty} onClick={() => setQuantity((v) => Math.min(maxQty, v + 1))}>
                +
              </button>
            </div>
          </div>
        </div>
      </div>

      <div className="m-card">
        <div className="m-sub" style={{ marginBottom: 8 }}>商品详情</div>
        {detail.detail ? (
          <div style={{ whiteSpace: 'pre-wrap', color: '#4a443c', fontSize: 13, lineHeight: 1.8 }}>
            {plainText(detail.detail)}
          </div>
        ) : (
          <div className="m-mute">商家未填写图文详情</div>
        )}
      </div>

      <div className="m-card">
        <div className="m-row m-row--between" style={{ marginBottom: 10 }}>
          <span className="m-sub">顾客评价（{detail.reviewCount}）</span>
        </div>
        {reviews.length === 0 ? (
          <div className="m-mute">还没有评价</div>
        ) : (
          reviews.map((review) => (
            <div key={review.id} style={{ padding: '10px 0', borderTop: '1px solid #f2f2f2' }}>
              <div className="m-row" style={{ gap: 8 }}>
                <strong style={{ fontSize: 13 }}>{review.nickname}</strong>
                <span style={{ color: '#f5a623', fontSize: 12 }}>{'★'.repeat(review.score)}</span>
                <span className="m-mute" style={{ marginLeft: 'auto' }}>{dt(review.createTime)}</span>
              </div>
              {review.content && (
                <div style={{ marginTop: 6, fontSize: 13, color: '#4a443c', lineHeight: 1.6 }}>{review.content}</div>
              )}
              {review.images?.length > 0 && (
                <div style={{ display: 'flex', gap: 6, marginTop: 8 }}>
                  {review.images.map((img) => (
                    <SafeImage key={img} src={img} alt="" style={{ width: 64, height: 64, borderRadius: 8, objectFit: 'cover' }} />
                  ))}
                </div>
              )}
            </div>
          ))
        )}
      </div>

      {selfPurchaseBlocked && (
        <div className="m-selfbuy-tip">
          你是本店{myShopRole === 'OWNER' ? '店主' : '店员'}，不能用这个账号在自己店铺下单。
        </div>
      )}

      <div className="m-detailbar">
        <button
          className="m-btn m-btn--block"
          disabled={busy || !sellable || selfPurchaseBlocked}
          onClick={() => void addToCart()}
        >
          加入购物车
        </button>
        <button
          className="m-btn m-btn--primary m-btn--block"
          disabled={!sellable || selfPurchaseBlocked}
          onClick={buyNow}
        >
          立即购买
        </button>
      </div>
    </div>
  );
}
