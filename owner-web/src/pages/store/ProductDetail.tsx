import { useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Divider,
  Empty,
  InputNumber,
  Radio,
  Rate,
  Skeleton,
  Space,
  Tag,
} from 'antd';
import { useNavigate, useParams } from 'react-router-dom';
import { cartApi, storeApi } from '@/api/mall';
import SafeImage from '@/components/SafeImage';
import { useAuthStore } from '@/store/auth';
import { useShopStore } from '@/store/shop';
import { toast } from '@/utils/feedback';
import { dt, money, num } from '@/utils/format';
import type { ProductDetail as Detail, Review } from '@/types/mall';

export default function ProductDetail() {
  const { productId } = useParams<{ productId: string }>();
  const navigate = useNavigate();
  const shopId = useShopStore((state) => state.shopId);
  const token = useAuthStore((state) => state.token);
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

  if (loading) return <Skeleton active />;
  if (!detail) {
    return (
      <Card>
        <Empty description="商品不存在或已下架" />
        <div style={{ textAlign: 'center', marginTop: 16 }}>
          <Button onClick={() => navigate('/products')}>返回列表</Button>
        </div>
      </Card>
    );
  }

  const sku = detail.skus.find((s) => String(s.id) === activeSku);
  const sellable = Boolean(sku && sku.status === 0 && sku.availableStock > 0);

  const addToCart = async () => {
    if (!token) {
      toast.warning('请先登录');
      navigate('/login', { state: { from: `/product/${productId}` } });
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
    if (!sku || !sellable) {
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
    navigate('/checkout');
  };

  return (
    <div>
      <Button type="link" style={{ paddingLeft: 0 }} onClick={() => navigate(-1)}>
        ← 返回
      </Button>

      <Card style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', gap: 24, flexWrap: 'wrap' }}>
          <div style={{ width: 340 }}>
            <SafeImage
              src={activeImage}
              alt={detail.name}
              style={{
                width: '100%',
                aspectRatio: '1 / 1',
                objectFit: 'cover',
                borderRadius: 16,
                background: '#f5f5f5',
              }}
            />
            <div className="mall-thumbs" style={{ marginTop: 12 }}>
              {(detail.images ?? []).map((img) => (
                <SafeImage
                  key={img}
                  src={img}
                  alt=""
                  style={{ borderColor: img === activeImage ? '#111' : '#eee' }}
                  onClick={() => setActiveImage(img)}
                />
              ))}
            </div>
          </div>

          <div style={{ flex: 1, minWidth: 280 }}>
            <h1 style={{ fontSize: 22, fontWeight: 500, margin: '0 0 8px' }}>{detail.name}</h1>
            {detail.sellingPoint && <p style={{ color: '#525252', marginTop: 0 }}>{detail.sellingPoint}</p>}
            <Space size={8} wrap style={{ marginBottom: 12 }}>
              <Tag color={detail.totalAvailable > 0 ? 'green' : 'default'}>
                {detail.totalAvailable > 0 ? `可售 ${detail.totalAvailable} 件` : '暂时缺货'}
              </Tag>
              <Tag>{detail.reviewCount > 0 ? `${detail.reviewCount} 条评价` : '暂无评价'}</Tag>
            </Space>

            <div style={{ background: '#fafafa', borderRadius: 14, padding: '14px 16px', marginBottom: 16 }}>
              <span className="mall-price" style={{ fontSize: 26 }}>
                {money(sku ? sku.price : detail.minPrice)}
              </span>
              {sku?.listPrice && num(sku.listPrice) > num(sku.price) && (
                <span style={{ marginLeft: 10, color: '#a3a3a3', textDecoration: 'line-through' }}>
                  {money(sku.listPrice)}
                </span>
              )}
            </div>

            <div style={{ marginBottom: 16 }}>
              <div className="mall-card__meta" style={{ marginBottom: 8 }}>
                规格
              </div>
              <Radio.Group
                value={activeSku}
                onChange={(e) => {
                  setActiveSku(e.target.value);
                  setQuantity(1);
                }}
              >
                <Space wrap>
                  {(detail.skus ?? []).map((s) => {
                    const ok = s.status === 0 && s.availableStock > 0;
                    return (
                      <Radio.Button key={s.id} value={String(s.id)} disabled={!ok}>
                        {s.specName}
                        {!ok && '（缺货）'}
                      </Radio.Button>
                    );
                  })}
                </Space>
              </Radio.Group>
            </div>

            <div style={{ marginBottom: 20, display: 'flex', alignItems: 'center', gap: 12 }}>
              <span className="mall-card__meta">数量</span>
              <InputNumber
                min={1}
                max={Math.max(1, sku?.availableStock ?? 1)}
                value={quantity}
                onChange={(v) => setQuantity(Number(v) || 1)}
              />
              {sku && <span className="mall-card__meta">库存 {sku.availableStock} 件</span>}
            </div>

            <Space>
              <Button size="large" loading={busy} onClick={() => void addToCart()}>
                加入购物车
              </Button>
              <Button size="large" type="primary" onClick={buyNow}>
                立即购买
              </Button>
            </Space>
          </div>
        </div>
      </Card>

      <Card title="商品详情" style={{ marginBottom: 16 }}>
        {detail.detail ? (
          <div style={{ whiteSpace: 'pre-wrap', color: '#525252', lineHeight: 1.8 }}>{detail.detail}</div>
        ) : (
          <span className="mall-card__meta">商家未填写图文详情</span>
        )}
      </Card>

      <Card title={`顾客评价（${detail.reviewCount}）`}>
        {reviews.length === 0 ? (
          <Empty description="还没有评价" />
        ) : (
          reviews.map((r, index) => (
            <div key={r.id}>
              {index > 0 && <Divider style={{ margin: '14px 0' }} />}
              <Space size={10}>
                <strong>{r.nickname}</strong>
                <Rate disabled value={r.score} style={{ fontSize: 13 }} />
                <span className="mall-card__meta">{dt(r.createTime)}</span>
              </Space>
              {r.content && <p style={{ margin: '8px 0 0' }}>{r.content}</p>}
              {r.images?.length > 0 && (
                <div className="mall-thumbs" style={{ marginTop: 8 }}>
                  {r.images.map((img) => (
                    <SafeImage key={img} src={img} alt="" />
                  ))}
                </div>
              )}
            </div>
          ))
        )}
      </Card>

      <Alert
        style={{ marginTop: 16 }}
        type="info"
        showIcon
        message="售后说明"
        description="未发货可申请仅退款；已发货 7 天内可申请退货退款。售后按整单处理。"
      />
    </div>
  );
}
