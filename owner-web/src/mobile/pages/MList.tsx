import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { storeApi } from '@/api/mall';
import { useShopStore } from '@/store/shop';
import type { ProductCard } from '@/types/mall';
import MProductCard from '../components/MProductCard';

const SORTS = [
  { value: '', label: '最新' },
  { value: 'priceAsc', label: '价格 ↑' },
  { value: 'priceDesc', label: '价格 ↓' },
];

const PAGE_SIZE = 10;

export default function MList() {
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const shopId = useShopStore((state) => state.shopId);
  const categories = useShopStore((state) => state.home?.categories ?? []);

  const keyword = params.get('keyword') ?? '';
  const categoryId = params.get('categoryId') ?? '';
  const [sort, setSort] = useState('');
  const [draft, setDraft] = useState(keyword);
  const [items, setItems] = useState<ProductCard[]>([]);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState(false);
  const seq = useRef(0);

  useEffect(() => setDraft(keyword), [keyword]);

  const fetchPage = useCallback(
    async (page: number, replace: boolean) => {
      if (!shopId) return;
      const mySeq = ++seq.current;
      setLoading(true);
      try {
        const result = keyword
          ? await storeApi.search(shopId, { keyword, sort, pageNum: page, pageSize: PAGE_SIZE })
          : await storeApi.products(shopId, {
              categoryId: categoryId || undefined,
              sort: sort || undefined,
              pageNum: page,
              pageSize: PAGE_SIZE,
            });
        // 快速切换筛选时丢弃过期响应
        if (mySeq !== seq.current) return;
        setItems((prev) => (replace ? result.rows : [...prev, ...result.rows]));
        setTotal(result.total);
        setPageNum(page);
        setDone(page * PAGE_SIZE >= result.total);
      } catch {
        /* 拦截器已提示 */
      } finally {
        if (mySeq === seq.current) setLoading(false);
      }
    },
    [shopId, keyword, categoryId, sort],
  );

  useEffect(() => {
    if (!shopId) return;
    setItems([]);
    setDone(false);
    void fetchPage(1, true);
  }, [shopId, fetchPage]);

  const doSearch = () => {
    const next = new URLSearchParams(params);
    if (draft.trim()) next.set('keyword', draft.trim());
    else next.delete('keyword');
    next.delete('categoryId');
    setParams(next);
  };

  return (
    <div>
      <div style={{ display: 'flex', gap: 8, padding: '10px 12px' }}>
        <div
          style={{
            flex: 1,
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            height: 38,
            padding: '0 12px',
            background: '#fff',
            borderRadius: 999,
            border: '1px solid #ededed',
          }}
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#a3a3a3" strokeWidth="2" strokeLinecap="round">
            <circle cx="11" cy="11" r="7" />
            <path d="M20 20l-3.2-3.2" />
          </svg>
          <input
            className="m-input"
            style={{ fontSize: 13 }}
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter') doSearch();
            }}
            placeholder="搜索商品"
            enterKeyHint="search"
          />
          {draft && (
            <button
              style={{ border: 0, background: 'none', color: '#a3a3a3', padding: 0, fontSize: 14 }}
              onClick={() => {
                setDraft('');
                const next = new URLSearchParams(params);
                next.delete('keyword');
                setParams(next);
              }}
            >
              ×
            </button>
          )}
        </div>
        <button className="m-btn m-btn--sm" onClick={doSearch}>
          搜索
        </button>
      </div>

      {!keyword && categories.length > 0 && (
        <div className="m-chips" style={{ paddingTop: 0 }}>
          <button
            className={`m-chip${!categoryId ? ' is-active' : ''}`}
            onClick={() => {
              const next = new URLSearchParams(params);
              next.delete('categoryId');
              setParams(next);
            }}
          >
            全部
          </button>
          {categories.map((category) => (
            <button
              key={category.id}
              className={`m-chip${categoryId === String(category.id) ? ' is-active' : ''}`}
              onClick={() => setParams({ categoryId: String(category.id) })}
            >
              {category.name}
            </button>
          ))}
        </div>
      )}

      <div className="m-tabs" style={{ top: 0, position: 'static' }}>
        {SORTS.map((option) => (
          <button
            key={option.value}
            className={`m-tab${sort === option.value ? ' is-active' : ''}`}
            onClick={() => setSort(option.value)}
          >
            {option.label}
          </button>
        ))}
        <span className="m-tab" style={{ color: '#a3a3a3', cursor: 'default' }}>
          {total} 件
        </span>
      </div>

      {keyword && (
        <div style={{ padding: '10px 12px 0', color: '#737373', fontSize: 12 }}>
          「{keyword}」的搜索结果，共 {total} 件
          <button
            onClick={() => navigate('/m/list')}
            style={{ border: 0, background: 'none', color: '#2563eb', fontSize: 12, padding: '0 6px' }}
          >
            清除
          </button>
        </div>
      )}

      {items.length === 0 && !loading ? (
        <div className="m-empty">{keyword ? '没有找到相关商品' : '暂无商品'}</div>
      ) : (
        <div className="m-grid" style={{ paddingTop: 10 }}>
          {items.map((product) => (
            <MProductCard key={product.id} product={product} />
          ))}
        </div>
      )}

      {loading && (
        <>
          <div className="m-skeleton" style={{ height: 180 }} />
        </>
      )}

      {!loading && !done && items.length > 0 && (
        <div style={{ padding: '4px 12px 24px', textAlign: 'center' }}>
          <button className="m-btn m-btn--sm m-btn--ghost" onClick={() => void fetchPage(pageNum + 1, false)}>
            加载更多
          </button>
        </div>
      )}

      {!loading && done && items.length > 0 && (
        <div style={{ padding: '4px 0 24px', textAlign: 'center' }} className="m-mute">
          没有更多了
        </div>
      )}

      <div style={{ padding: '0 12px 24px', textAlign: 'center' }}>
        <Link to="/m" className="m-btn m-btn--sm m-btn--ghost">
          回首页
        </Link>
      </div>
    </div>
  );
}
