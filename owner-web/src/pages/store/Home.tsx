import { useEffect, useMemo, useState } from 'react';
import {
  AppstoreOutlined,
  ArrowRightOutlined,
  CustomerServiceOutlined,
  GiftOutlined,
  ProfileOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  SearchOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { storeApi } from '@/api/mall';
import ProductTile from '@/components/ProductTile';
import SafeImage from '@/components/SafeImage';
import { useShopStore } from '@/store/shop';
import type { Category, ProductCard } from '@/types/mall';

type SortKey = 'default' | 'priceAsc' | 'priceDesc';

const SORTS: { key: SortKey; label: string }[] = [
  { key: 'default', label: '最新上架' },
  { key: 'priceAsc', label: '价格从低到高' },
  { key: 'priceDesc', label: '价格从高到低' },
];

const PROMISES = [
  { icon: <SafetyCertificateOutlined />, title: '品质保障', desc: '上架前人工挑选' },
  { icon: <ThunderboltOutlined />, title: '快速发货', desc: '付款确认后尽快寄出' },
  { icon: <ReloadOutlined />, title: '7 天售后', desc: '未发货可申请退款' },
  { icon: <CustomerServiceOutlined />, title: '售后无忧', desc: '整单处理，进度可查' },
];

/** 首屏与特色卡的内置配图：店铺没设横幅时用它兜底，避免一片灰「暂无图片」 */
const HERO_FALLBACK = '/images/hero-daily-goods.jpg';
const CATEGORY_SCENE = '/images/scene-categories.jpg';
const ORDER_SCENE = '/images/scene-orders.jpg';

export default function Home() {
  const navigate = useNavigate();
  const shopId = useShopStore((state) => state.shopId);
  const [products, setProducts] = useState<ProductCard[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [notice, setNotice] = useState('');
  const [intro, setIntro] = useState('');
  const [bannerUrl, setBannerUrl] = useState('');
  const [productTotal, setProductTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [sort, setSort] = useState<SortKey>('default');

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
        setBannerUrl(home.bannerUrl ?? '');
        setProductTotal(home.productTotal ?? (home.products ?? []).length);
      })
      .catch(() => undefined)
      .finally(() => alive && setLoading(false));
    return () => {
      alive = false;
    };
  }, [shopId]);

  const sorted = useMemo(() => {
    const list = [...products];
    if (sort === 'priceAsc') list.sort((a, b) => a.minPrice - b.minPrice);
    if (sort === 'priceDesc') list.sort((a, b) => b.minPrice - a.minPrice);
    return list;
  }, [products, sort]);

  /** 搜索热词：取一级与二级类目名，去重后前 6 个 */
  const hotWords = useMemo(() => {
    const words = categories.flatMap((category) => [
      category.name,
      ...(category.children ?? []).map((child) => child.name),
    ]);
    return Array.from(new Set(words.filter(Boolean))).slice(0, 6);
  }, [categories]);

  const heroImage = bannerUrl || HERO_FALLBACK;

  const submitSearch = () => {
    const value = keyword.trim();
    if (value.length < 2) {
      navigate('/products');
      return;
    }
    navigate(`/products?keyword=${encodeURIComponent(value)}`);
  };

  const availableTotal = products.reduce((sum, item) => sum + (item.totalAvailable ?? 0), 0);

  return (
    <div>
      {/* ---------------------------- 首屏 ---------------------------- */}
      <section className="sf-hero">
        <div className="sf-hero__bg">
          {heroImage ? <SafeImage src={heroImage} alt="" /> : null}
        </div>
        <div className="sf-hero__veil" />

        <p className="sf-hero__kicker">Daily Goods · 日用铺</p>
        <h1 className="sf-hero__title">
          把日子过得
          <br />
          <em>顺手一点</em>
        </h1>
        <p className="sf-hero__desc">
          {intro || '清洁、厨房、收纳、个护。少一点花哨，多一点好用——我们只上架经得起日常的东西。'}
        </p>

        <div className="sf-hero__stats">
          <div className="sf-hero__stat">
            <b>{productTotal}</b>
            <span>在售商品</span>
          </div>
          <div className="sf-hero__stat">
            <b>{categories.length}</b>
            <span>商品分类</span>
          </div>
          <div className="sf-hero__stat">
            <b>{availableTotal}</b>
            <span>可售件数</span>
          </div>
        </div>

        {notice && (
          <div className="sf-hero__notice">
            <strong>店铺公告</strong>
            <span>{notice}</span>
          </div>
        )}
      </section>

      {/* ------------------------- 悬浮搜索面板 ------------------------- */}
      <div className="sf-searchpanel">
        <div className="sf-searchpanel__form">
          <label className="sf-searchpanel__field">
            <SearchOutlined style={{ color: '#a89f95', marginRight: 10 }} />
            <input
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              onKeyDown={(event) => event.key === 'Enter' && submitSearch()}
              placeholder="找点什么？输入商品名或类目，如「抹布」「收纳」"
              aria-label="搜索商品"
            />
          </label>
          <button type="button" className="sf-btn" onClick={submitSearch}>
            开始搜索 <ArrowRightOutlined />
          </button>
        </div>
        <div className="sf-searchpanel__hot">
          <span>热搜</span>
          {hotWords.map((word) => (
            <button
              key={word}
              type="button"
              className="sf-hot"
              onClick={() => navigate(`/products?keyword=${encodeURIComponent(word)}`)}
            >
              {word}
            </button>
          ))}
          <button type="button" className="sf-hot" onClick={() => navigate('/products')}>
            全部商品
          </button>
        </div>
      </div>

      {/* ---------------------------- 三张特色卡 ---------------------------- */}
      <section className="sf-section">
        <div className="sf-feature-grid">
          <button
            type="button"
            className="sf-feature"
            style={{ border: 0, textAlign: 'left', cursor: 'pointer' }}
            onClick={() => navigate('/products')}
          >
            <div className="sf-feature__bg">
              <SafeImage src={CATEGORY_SCENE} alt="" />
            </div>
            <div className="sf-feature__veil" />
            <span className="sf-feature__icon">
              <AppstoreOutlined />
            </span>
            <h3 className="sf-feature__title">{categories.length ? '逛全部分类' : '逛全部商品'}</h3>
            <p className="sf-feature__desc">
              {categories.length
                ? categories.map((category) => category.name).join(' · ')
                : '清洁 / 厨房 / 收纳 / 个护'}
            </p>
            <span className="sf-feature__link">看看有什么</span>
          </button>

          <button
            type="button"
            className="sf-feature sf-feature--orange"
            style={{ border: 0, textAlign: 'left', cursor: 'pointer' }}
            onClick={() => navigate('/account?tab=coupons')}
          >
            <span className="sf-feature__icon">
              <GiftOutlined />
            </span>
            <h3 className="sf-feature__title">领券再下单</h3>
            <p className="sf-feature__desc">
              登录后可在领券中心领取本店优惠券，满减直接抵扣，结算时自动算出最优组合。
            </p>
            <span className="sf-feature__link">去领券中心</span>
          </button>

          <button
            type="button"
            className="sf-feature sf-feature--light"
            style={{ border: 0, textAlign: 'left', cursor: 'pointer' }}
            onClick={() => navigate('/orders')}
          >
            <div className="sf-feature__bg">
              <SafeImage src={ORDER_SCENE} alt="" />
            </div>
            <div className="sf-feature__veil" />
            <span className="sf-feature__icon">
              <ProfileOutlined />
            </span>
            <h3 className="sf-feature__title">订单随时可查</h3>
            <p className="sf-feature__desc">待确认、待发货、待收货、售后进度，一页看清每一单走到哪一步。</p>
            <span className="sf-feature__link">查看我的订单</span>
          </button>
        </div>
      </section>

      {/* ----------------------------- 分类宫格 ----------------------------- */}
      {categories.length > 0 && (
        <section className="sf-section">
          <div className="sf-section__head">
            <div>
              <span className="sf-section__kicker">Categories</span>
              <h2 className="sf-section__title">按日常场景，慢慢挑</h2>
            </div>
            <button type="button" className="sf-section__more" onClick={() => navigate('/products')}>
              全部商品 <ArrowRightOutlined />
            </button>
          </div>

          <div className="sf-cat-grid">
            {categories.slice(0, 5).map((category, index) => (
              <div
                key={category.id}
                className="sf-cat"
                role="button"
                tabIndex={0}
                onClick={() => navigate(`/products?categoryId=${category.id}`)}
                onKeyDown={(event) => event.key === 'Enter' && navigate(`/products?categoryId=${category.id}`)}
              >
                <span className="sf-cat__index">{String(index + 1).padStart(2, '0')}</span>
                <span className="sf-cat__name">{category.name}</span>
                <div className="sf-cat__children">
                  {(category.children ?? []).slice(0, 4).map((child) => (
                    <span
                      key={child.id}
                      role="button"
                      tabIndex={0}
                      onClick={(event) => {
                        event.stopPropagation();
                        navigate(`/products?categoryId=${child.id}`);
                      }}
                      onKeyDown={(event) => {
                        if (event.key !== 'Enter') return;
                        event.stopPropagation();
                        navigate(`/products?categoryId=${child.id}`);
                      }}
                    >
                      {child.name}
                    </span>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* ----------------------------- 商品列表 ----------------------------- */}
      <section className="sf-section">
        <div className="sf-section__head">
          <div>
            <span className="sf-section__kicker">Just Landed</span>
            <h2 className="sf-section__title">最近上架的好东西</h2>
          </div>
          <div className="sf-tabs">
            {SORTS.map((item) => (
              <button
                key={item.key}
                type="button"
                className={`sf-tabl${sort === item.key ? ' is-active' : ''}`}
                onClick={() => setSort(item.key)}
              >
                {item.label}
              </button>
            ))}
          </div>
        </div>

        {loading ? (
          <div className="sf-grid">
            {Array.from({ length: 8 }).map((_, index) => (
              <div key={index} className="sf-skeleton" style={{ height: 320 }} />
            ))}
          </div>
        ) : sorted.length === 0 ? (
          <div className="sf-empty">
            <AppstoreOutlined style={{ fontSize: 28, color: '#c9c0b6' }} />
            店铺还没上架商品，过一会儿再来看看
          </div>
        ) : (
          <div className="sf-grid">
            {sorted.map((product) => (
              <ProductTile key={product.id} product={product} />
            ))}
          </div>
        )}

        <div className="sf-promise">
          {PROMISES.map((item) => (
            <div key={item.title} className="sf-promise__item">
              <span className="sf-promise__icon">{item.icon}</span>
              <div>
                <div className="sf-promise__title">{item.title}</div>
                <div className="sf-promise__desc">{item.desc}</div>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
