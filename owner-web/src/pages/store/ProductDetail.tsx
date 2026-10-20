import { useEffect, useMemo, useState } from 'react';
import { Rate } from 'antd';
import {
  CarOutlined,
  LeftOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  ShoppingCartOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { cartApi, storeApi } from '@/api/mall';
import SafeImage from '@/components/SafeImage';
import { useAuthStore } from '@/store/auth';
import { useShopStore } from '@/store/shop';
import { toast } from '@/utils/feedback';
import { dt, num, yuan } from '@/utils/format';
import type { ProductDetail as Detail, Review } from '@/types/mall';

export default function ProductDetail() {
  const { productId } = useParams<{ productId: string }>();
  const navigate = useNavigate();
  const shopId = useShopStore((state) => state.shopId);
  const token = useAuthStore((state) => state.token);
  const shops = useAuthStore((state) => state.shops);
  const [detail, setDetail] = useState<Detail | null>(null);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeSku, setActiveSku] = useState<string>('');
  const [quantity, setQuantity] = useState(1);
  const [activeImage, setActiveImage] = useState('');
  const [busy, setBusy] = useState(false);

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
        setActiveImage(d.images?.[0] ?? '');
        const firstOk = d.skus?.find((s) => s.availableStock > 0) ?? d.skus?.[0];
        setActiveSku(firstOk ? String(firstOk.id) : '');
        setReviews(r?.rows ?? []);
      })
      .catch(() => undefined)
      .finally(() => alive && setLoading(false));
    return () => {
      alive = false;
    };
  }, [shopId, productId]);

  const sku = useMemo(
    () => detail?.skus.find((item) => String(item.id) === activeSku),
    [detail, activeSku],
  );
  const sellable = Boolean(sku && sku.status === 0 && sku.availableStock > 0);

  /** 当前账号在这家店里的身份（跨店浏览时不能拿 shopRoleOf 的兜底，会误判） */
  const myShopRole = shops.find((s) => String(s.shopId) === String(shopId))?.role;
  const selfPurchaseBlocked = Boolean(token && myShopRole);
  const selfPurchaseTip =
    myShopRole === 'OWNER'
      ? '你是本店店主，不能用店主账号在自己店铺下单，想体验购买流程请注册一个顾客账号。'
      : '你是本店店员，不能用员工账号在自己店铺下单，想体验购买流程请注册一个顾客账号。';

  const addToCart = async () => {
    if (!token) {
      toast.warning('请先登录');
      navigate('/login', { state: { from: `/product/${productId}` } });
      return;
    }
    if (selfPurchaseBlocked) {
      toast.warning(selfPurchaseTip);
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
    if (!token) {
      toast.warning('请先登录');
      navigate('/login', { state: { from: `/product/${productId}` } });
      return;
    }
    if (selfPurchaseBlocked) {
      toast.warning(selfPurchaseTip);
      return;
    }
    if (!sku || !sellable) {
      toast.warning('请选择有货的规格');
      return;
    }
    sessionStorage.setItem(
      'checkout:payload',
      JSON.stringify({
        shopId: detail?.shopId,
        items: [{ skuId: String(sku.id), quantity, price: num(sku.price) }],
      }),
    );
    navigate('/checkout');
  };

  const changeQuantity = (next: number) => {
    const max = Math.max(1, sku?.availableStock ?? 1);
    setQuantity(Math.min(Math.max(1, next), max));
  };

  if (loading) {
    return (
      <div>
        <div className="sf-skeleton" style={{ height: 24, width: 200, marginBottom: 18 }} />
        <div className="sf-skeleton" style={{ height: 520 }} />
      </div>
    );
  }

  if (!detail) {
    return (
      <div className="sf-empty">
        <span style={{ fontSize: 16, color: '#3f3831', fontWeight: 700 }}>商品不存在或已下架</span>
        <div style={{ display: 'flex', gap: 10 }}>
          <button type="button" className="sf-btn sf-btn--ghost" onClick={() => navigate('/products')}>
            返回列表
          </button>
          <button type="button" className="sf-btn" onClick={() => navigate('/')}>
            回首页
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="sf-detail-page">
      <div className="sf-pagehead__crumb" style={{ marginBottom: 14 }}>
        <Link to="/">首页</Link>
        <span style={{ fontSize: 9 }}>›</span>
        <Link to="/products">全部商品</Link>
        <span style={{ fontSize: 9 }}>›</span>
        <span style={{ color: '#3f3831' }}>{detail.name}</span>
      </div>

      <div className="sf-detail">
        <div>
          <div className="sf-gallery__main">
            <SafeImage src={activeImage} alt={detail.name} />
          </div>
          {(detail.images ?? []).length > 1 && (
            <div className="sf-gallery__thumbs">
              {detail.images.map((image) => (
                <button
                  key={image}
                  type="button"
                  className={`sf-gallery__thumb${image === activeImage ? ' is-active' : ''}`}
                  onClick={() => setActiveImage(image)}
                  aria-label="切换商品图片"
                >
                  <SafeImage src={image} alt="" />
                </button>
              ))}
            </div>
          )}
        </div>

        <div>
          <div className="sf-pagehead__crumb">
            <span>{detail.totalAvailable > 0 ? '现货在售' : '暂时缺货'}</span>
            <span style={{ fontSize: 9 }}>›</span>
            <span>{detail.reviewCount > 0 ? `${detail.reviewCount} 条评价` : '暂无评价'}</span>
          </div>

          <h1 className="sf-detail__name">{detail.name}</h1>
          {detail.sellingPoint && <p className="sf-detail__point">{detail.sellingPoint}</p>}

          <div className="sf-pricebox">
            <div className="sf-pricebox__row">
              <span className="sf-pricebox__price">
                <small>¥</small>
                {yuan(sku ? sku.price : detail.minPrice)}
              </span>
            </div>
            <div className="sf-pricebox__meta">
              <span>{detail.totalAvailable > 0 ? `可售 ${detail.totalAvailable} 件` : '暂时缺货'}</span>
              <span>·</span>
              <span>{detail.reviewCount > 0 ? `${detail.reviewCount} 条评价` : '暂无评价'}</span>
              <span>·</span>
              <span>线下收款，店主确认后发货</span>
            </div>
          </div>

          <div className="sf-field">
            <div className="sf-field__label">
              规格
              <span>{sku ? `库存 ${sku.availableStock} 件` : '请选择'}</span>
            </div>
            <div className="sf-sku">
              {(detail.skus ?? []).map((item) => {
                const ok = item.status === 0 && item.availableStock > 0;
                return (
                  <button
                    key={item.id}
                    type="button"
                    disabled={!ok}
                    className={`sf-sku__item${String(item.id) === activeSku ? ' is-active' : ''}`}
                    onClick={() => {
                      setActiveSku(String(item.id));
                      setQuantity(1);
                    }}
                  >
                    {item.specName}
                    {!ok ? '（缺货）' : ''}
                  </button>
                );
              })}
            </div>
          </div>

          <div className="sf-field">
            <div className="sf-field__label">数量</div>
            <div className="sf-qty">
              <button type="button" onClick={() => changeQuantity(quantity - 1)} disabled={quantity <= 1}>
                −
              </button>
              <input
                value={quantity}
                aria-label="购买数量"
                onChange={(event) => changeQuantity(Number(event.target.value.replace(/\D/g, '')) || 1)}
              />
              <button
                type="button"
                onClick={() => changeQuantity(quantity + 1)}
                disabled={quantity >= Math.max(1, sku?.availableStock ?? 1)}
              >
                +
              </button>
            </div>
          </div>

          {selfPurchaseBlocked && (
            <div className="sf-notice" style={{ marginTop: 22 }}>
              <strong style={{ color: '#d9481a' }}>不能在本店下单</strong>
              <span>{selfPurchaseTip}</span>
            </div>
          )}

          <div className="sf-buy">
            <button
              type="button"
              className="sf-btn sf-btn--ghost sf-btn--lg"
              disabled={!sellable || busy || selfPurchaseBlocked}
              onClick={() => void addToCart()}
            >
              <ShoppingCartOutlined /> 加入购物车
            </button>
            <button
              type="button"
              className="sf-btn sf-btn--lg"
              disabled={!sellable || selfPurchaseBlocked}
              onClick={buyNow}
            >
              <ThunderboltOutlined /> 立即购买
            </button>
          </div>

          <div className="sf-services">
            <span>
              <i>✓</i>
              <SafetyCertificateOutlined /> 品质保障
            </span>
            <span>
              <i>✓</i>
              <CarOutlined /> 确认收款后发货
            </span>
            <span>
              <i>✓</i>
              <ReloadOutlined /> 未发货可退款
            </span>
          </div>
        </div>
      </div>

      <section className="sf-detail__section">
        <h2>商品详情</h2>
        {detail.detail ? (
          <div className="sf-detail__text">{detail.detail}</div>
        ) : (
          <span style={{ color: '#857e75', fontSize: 14 }}>商家未填写图文详情</span>
        )}
      </section>

      <section className="sf-detail__section">
        <h2>顾客评价（{detail.reviewCount}）</h2>
        {reviews.length === 0 ? (
          <span style={{ color: '#857e75', fontSize: 14 }}>还没有评价，等你来做第一个</span>
        ) : (
          reviews.map((review) => (
            <div key={review.id} className="sf-review">
              <div className="sf-review__head">
                <span className="sf-review__avatar">{(review.nickname || '匿').slice(0, 1)}</span>
                <span className="sf-review__name">{review.nickname}</span>
                <Rate disabled value={review.score} style={{ fontSize: 13 }} />
                <span className="sf-review__time">{dt(review.createTime)}</span>
              </div>
              {review.content && <p className="sf-review__content">{review.content}</p>}
              {review.images?.length > 0 && (
                <div className="sf-review__images">
                  {review.images.map((image) => (
                    <SafeImage key={image} src={image} alt="" />
                  ))}
                </div>
              )}
            </div>
          ))
        )}
      </section>

      <div className="sf-notice" style={{ marginTop: 22 }}>
        <strong style={{ color: '#d9481a' }}>售后说明</strong>
        <span>未发货可申请仅退款；已发货 7 天内可申请退货退款。售后按整单处理，进度可在「退款/售后」查看。</span>
      </div>

      {/* 手机端底部操作条 */}
      <div className="sf-buybar">
        <Link to="/cart" className="sf-buybar__icon">
          <i>
            <ShoppingCartOutlined />
          </i>
          购物车
        </Link>
        <button
          type="button"
          className="sf-btn sf-btn--ghost"
          disabled={!sellable || busy || selfPurchaseBlocked}
          onClick={() => void addToCart()}
        >
          加入购物车
        </button>
        <button
          type="button"
          className="sf-btn"
          disabled={!sellable || selfPurchaseBlocked}
          onClick={buyNow}
        >
          立即购买
        </button>
      </div>

      <div style={{ marginTop: 20 }}>
        <button type="button" className="sf-btn sf-btn--ghost" onClick={() => navigate(-1)}>
          <LeftOutlined /> 返回
        </button>
      </div>
    </div>
  );
}
