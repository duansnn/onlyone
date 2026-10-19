import { useCallback, useEffect, useState } from 'react';
import { Button, Card, Empty, Pagination, Radio, Skeleton, Space, Tag } from 'antd';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { storeApi } from '@/api/mall';
import SafeImage from '@/components/SafeImage';
import { useShopStore } from '@/store/shop';
import { money } from '@/utils/format';
import type { Category, ProductCard } from '@/types/mall';

const SORTS = [
  { value: '', label: '最新上架' },
  { value: 'priceAsc', label: '价格从低到高' },
  { value: 'priceDesc', label: '价格从高到低' },
];

export default function ProductList() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const shopId = useShopStore((state) => state.shopId);
  const [rows, setRows] = useState<ProductCard[]>([]);
  const [total, setTotal] = useState(0);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);

  const categoryId = searchParams.get('categoryId') ?? '';
  const keyword = searchParams.get('keyword') ?? '';
  const sort = searchParams.get('sort') ?? '';
  const pageNum = Number(searchParams.get('pageNum') ?? 1);

  const load = useCallback(async () => {
    if (!shopId) return;
    setLoading(true);
    try {
      const pageSize = 12;
      const result = keyword
        ? await storeApi.search(shopId, { keyword, sort: sort || undefined, pageNum, pageSize })
        : await storeApi.products(shopId, {
            categoryId: categoryId || undefined,
            sort: sort || undefined,
            pageNum,
            pageSize,
          });
      setRows(result.rows ?? []);
      setTotal(result.total ?? 0);
    } catch {
      setRows([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  }, [shopId, keyword, categoryId, sort, pageNum]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (!shopId) return;
    void storeApi
      .home(shopId)
      .then((home) => setCategories(home.categories ?? []))
      .catch(() => undefined);
  }, [shopId]);

  const patch = (next: Record<string, string | number | undefined>) => {
    const params = new URLSearchParams(searchParams);
    Object.entries(next).forEach(([k, v]) => {
      if (v === undefined || v === '' || v === null) params.delete(k);
      else params.set(k, String(v));
    });
    if (!('pageNum' in next)) params.set('pageNum', '1');
    setSearchParams(params);
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div>
          <h2 className="mall-section-title" style={{ marginBottom: 4 }}>
            {keyword ? `搜索“${keyword}”` : '全部商品'}
          </h2>
          <span className="mall-card__meta">共 {total} 件</span>
        </div>
        <Radio.Group
          value={sort}
          options={SORTS}
          optionType="button"
          buttonStyle="solid"
          onChange={(e) => patch({ sort: e.target.value })}
        />
      </div>

      <Space wrap style={{ marginBottom: 16 }}>
        <Tag
          color={!categoryId ? 'blue' : undefined}
          style={{ cursor: 'pointer', padding: '4px 12px' }}
          onClick={() => patch({ categoryId: '', keyword: '' })}
        >
          全部
        </Tag>
        {categories.map((c) => (
          <Tag
            key={c.id}
            color={categoryId === c.id ? 'blue' : undefined}
            style={{ cursor: 'pointer', padding: '4px 12px' }}
            onClick={() => patch({ categoryId: c.id, keyword: '' })}
          >
            {c.name}
          </Tag>
        ))}
      </Space>

      {loading ? (
        <Skeleton active />
      ) : rows.length === 0 ? (
        <Card>
          <Empty description={keyword ? '没有找到相关商品' : '该类目下暂无在售商品'} />
        </Card>
      ) : (
        <>
          <div className="mall-grid">
            {rows.map((p) => (
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
                  <span className="mall-card__meta">可售 {p.totalAvailable} 件</span>
                </div>
              </div>
            ))}
          </div>

          <div style={{ textAlign: 'center', marginTop: 24 }}>
            <Pagination
              current={pageNum}
              total={total}
              pageSize={12}
              showSizeChanger={false}
              onChange={(page) => patch({ pageNum: page })}
            />
          </div>
        </>
      )}

      <div style={{ marginTop: 24 }}>
        <Button onClick={() => navigate('/')}>返回首页</Button>
      </div>
    </div>
  );
}
