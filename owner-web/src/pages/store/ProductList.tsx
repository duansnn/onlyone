import { useCallback, useEffect, useMemo, useState } from 'react';
import { Pagination } from 'antd';
import { AppstoreOutlined, HomeOutlined, RightOutlined } from '@ant-design/icons';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { storeApi } from '@/api/mall';
import ProductTile from '@/components/ProductTile';
import { useShopStore } from '@/store/shop';
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

  /** 当前选中的一级类目：categoryId 既可能是一级，也可能是某一级的子类 */
  const activeParent = useMemo(() => {
    if (!categoryId) return null;
    return (
      categories.find((category) => category.id === categoryId) ??
      categories.find((category) => (category.children ?? []).some((child) => child.id === categoryId)) ??
      null
    );
  }, [categories, categoryId]);

  const activeChildId = activeParent
    ? (activeParent.children ?? []).some((child) => child.id === categoryId)
      ? categoryId
      : ''
    : '';

  const title = keyword ? `搜索「${keyword}」` : (activeParent?.name ?? '全部商品');
  const hasFilter = Boolean(keyword || categoryId || sort);

  return (
    <div>
      <div className="sf-pagehead">
        <div>
          <div className="sf-pagehead__crumb">
            <Link to="/">
              <HomeOutlined /> 首页
            </Link>
            <RightOutlined style={{ fontSize: 9 }} />
            <span>{title}</span>
          </div>
          <h1 className="sf-pagehead__title">
            {title}
            <span className="sf-pagehead__count">共 {total} 件</span>
          </h1>
        </div>
        {hasFilter && (
          <button
            type="button"
            className="sf-btn sf-btn--ghost"
            onClick={() => setSearchParams(new URLSearchParams())}
          >
            清空筛选
          </button>
        )}
      </div>

      <div className="sf-filter">
        <div className="sf-filter__row">
          <span className="sf-filter__label">分类</span>
          <div className="sf-filter__chips">
            <button
              type="button"
              className={`sf-chip${!categoryId && !keyword ? ' is-active' : ''}`}
              onClick={() => patch({ categoryId: '', keyword: '' })}
            >
              全部
            </button>
            {categories.map((category) => (
              <button
                key={category.id}
                type="button"
                className={`sf-chip${activeParent?.id === category.id ? ' is-active' : ''}`}
                onClick={() => patch({ categoryId: category.id, keyword: '' })}
              >
                {category.name}
              </button>
            ))}
          </div>
        </div>

        {activeParent && (activeParent.children ?? []).length > 0 && (
          <div className="sf-filter__row">
            <span className="sf-filter__label">{activeParent.name}</span>
            <div className="sf-filter__chips">
              <button
                type="button"
                className={`sf-chip sf-chip--sub${!activeChildId ? ' is-active' : ''}`}
                onClick={() => patch({ categoryId: activeParent.id })}
              >
                全部{activeParent.name}
              </button>
              {(activeParent.children ?? []).map((child) => (
                <button
                  key={child.id}
                  type="button"
                  className={`sf-chip sf-chip--sub${activeChildId === child.id ? ' is-active' : ''}`}
                  onClick={() => patch({ categoryId: child.id })}
                >
                  {child.name}
                </button>
              ))}
            </div>
          </div>
        )}
      </div>

      <div className="sf-toolbar">
        <div className="sf-tabs">
          {SORTS.map((item) => (
            <button
              key={item.value || 'default'}
              type="button"
              className={`sf-tabl${sort === item.value ? ' is-active' : ''}`}
              onClick={() => patch({ sort: item.value })}
            >
              {item.label}
            </button>
          ))}
        </div>
        <span style={{ fontSize: 12, color: '#857e75' }}>按下单价格排序 · 单笔订单仅限本店商品</span>
      </div>

      {loading ? (
        <div className="sf-grid">
          {Array.from({ length: 8 }).map((_, index) => (
            <div key={index} className="sf-skeleton" style={{ height: 320 }} />
          ))}
        </div>
      ) : rows.length === 0 ? (
        <div className="sf-empty">
          <AppstoreOutlined style={{ fontSize: 28, color: '#c9c0b6' }} />
          {keyword ? '没有找到相关商品，换个关键词试试' : '该类目下暂无在售商品'}
          <div style={{ display: 'flex', gap: 10 }}>
            <button type="button" className="sf-btn sf-btn--ghost" onClick={() => setSearchParams(new URLSearchParams())}>
              看全部商品
            </button>
            <button type="button" className="sf-btn" onClick={() => navigate('/')}>
              回首页
            </button>
          </div>
        </div>
      ) : (
        <>
          <div className="sf-grid">
            {rows.map((product) => (
              <ProductTile key={product.id} product={product} />
            ))}
          </div>

          {total > 12 && (
            <div className="sf-pagination">
              <Pagination
                current={pageNum}
                total={total}
                pageSize={12}
                showSizeChanger={false}
                onChange={(page) => patch({ pageNum: page })}
              />
            </div>
          )}
        </>
      )}
    </div>
  );
}
