import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useShopStore } from '@/store/shop';
import MProductCard from '../components/MProductCard';

export default function MHome() {
  const navigate = useNavigate();
  const home = useShopStore((state) => state.home);
  const loading = useShopStore((state) => state.loading);
  const [keyword, setKeyword] = useState('');

  const search = () => {
    const k = keyword.trim();
    navigate(k ? `/m/list?keyword=${encodeURIComponent(k)}` : '/m/list');
  };

  if (!home) {
    return (
      <div>
        <div className="m-hero">
          <div className="m-hero__name">日用铺</div>
          <div className="m-hero__intro">{loading ? '正在加载店铺…' : '店铺暂时无法访问'}</div>
        </div>
        {loading && (
          <>
            <div className="m-skeleton" />
            <div className="m-skeleton" />
          </>
        )}
      </div>
    );
  }

  return (
    <div>
      <div className="m-hero">
        <div className="m-hero__brand">Daily Goods · 日用铺</div>
        <div className="m-hero__name">{home.shopName}</div>
        {home.intro && <div className="m-hero__intro">{home.intro}</div>}
        {home.notice && (
          <div className="m-hero__notice">
            <span>公告</span>
            <span>{home.notice}</span>
          </div>
        )}
        <div className="m-searchbar">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <circle cx="11" cy="11" r="7" />
            <path d="M20 20l-3.2-3.2" />
          </svg>
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter') search();
            }}
            placeholder="搜一搜店里的东西"
            enterKeyHint="search"
            inputMode="search"
          />
          <button onClick={search} className="m-searchbar__go">
            搜索
          </button>
        </div>
      </div>

      {(home.categories ?? []).length > 0 && (
        <div className="m-chips">
          <Link to="/m/list" className="m-chip">
            全部
          </Link>
          {home.categories.map((category) => (
            <Link key={category.id} to={`/m/list?categoryId=${category.id}`} className="m-chip">
              {category.name}
            </Link>
          ))}
        </div>
      )}

      <div className="m-section-title">
        <span>全部商品</span>
        <Link to="/m/list" style={{ color: '#6f675e', fontSize: 12, textDecoration: 'none' }}>
          共 {home.productTotal} 件 ›
        </Link>
      </div>

      {home.products.length === 0 ? (
        <div className="m-empty">店里还没有上架商品</div>
      ) : (
        <div className="m-grid">
          {home.products.map((product) => (
            <MProductCard key={product.id} product={product} />
          ))}
        </div>
      )}

      <div style={{ padding: '4px 0 20px', textAlign: 'center' }}>
        <Link to="/m/list" className="m-btn m-btn--sm m-btn--ghost">
          查看全部商品
        </Link>
      </div>
    </div>
  );
}
