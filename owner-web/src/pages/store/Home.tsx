import { useEffect, useState } from 'react';
import { Button, Card, Empty, Skeleton, Tag } from 'antd';
import { useNavigate } from 'react-router-dom';
import { storeApi } from '@/api/mall';
import SafeImage from '@/components/SafeImage';
import { useShopStore } from '@/store/shop';
import { money } from '@/utils/format';
import type { Category, ProductCard } from '@/types/mall';

export default function Home() {
  const navigate = useNavigate();
  const shopId = useShopStore((state) => state.shopId);
  const [products, setProducts] = useState<ProductCard[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [notice, setNotice] = useState('');
  const [intro, setIntro] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!shopId) return;
    let alive = true;
    setLoading(true);
    void storeApi
      .home(shopId)
      .then((home) => {
        if (!alive) return;
        setProducts(home.products ?? []);
        setCategories(home.categories ?? []);
        setNotice(home.notice ?? '');
        setIntro(home.intro ?? '');
      })
      .catch(() => undefined)
      .finally(() => alive && setLoading(false));
    return () => {
      alive = false;
    };
  }, [shopId]);

  return (
    <div>
      <section className="mall-hero">
        <h1>生活里的小东西</h1>
        <p>{intro || '清洁、厨房、收纳、个护。少一点货架，多一点留白。'}</p>
        {notice && (
          <p style={{ marginTop: 12 }}>
            <Tag color="blue">店铺公告</Tag>
            {notice}
          </p>
        )}
        <div style={{ marginTop: 18, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          {categories.map((c) => (
            <Button key={c.id} onClick={() => navigate(`/products?categoryId=${c.id}`)}>
              {c.name}
            </Button>
          ))}
          <Button type="primary" onClick={() => navigate('/products')}>
            全部商品
          </Button>
        </div>
      </section>

      <h2 className="mall-section-title">最新上架</h2>

      {loading ? (
        <Skeleton active />
      ) : products.length === 0 ? (
        <Card>
          <Empty description="店铺还没上架商品" />
        </Card>
      ) : (
        <div className="mall-grid">
          {products.map((p) => (
            <div
              key={p.id}
              className="mall-card"
              role="button"
              tabIndex={0}
              onClick={() => navigate(`/product/${p.id}`)}
              onKeyDown={(e) => e.key === 'Enter' && navigate(`/product/${p.id}`)}
            >
              <SafeImage className="mall-card__img" src={p.coverImage} alt={p.name} />
              <div className="mall-card__body">
                <span className="mall-card__name">{p.name}</span>
                <span className="mall-card__price">
                  {money(p.minPrice)}
                  {p.maxPrice > p.minPrice && <span style={{ fontSize: 13 }}> 起</span>}
                </span>
                <span className="mall-card__meta">
                  可售 {p.totalAvailable} 件{p.sellingPoint ? ` · ${p.sellingPoint}` : ''}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
