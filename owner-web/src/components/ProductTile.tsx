import { ArrowRightOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import SafeImage from '@/components/SafeImage';
import { yuan } from '@/utils/format';
import type { ProductCard } from '@/types/mall';

/**
 * 店面统一商品卡：暖橙价格、库存角标、悬停浮起。
 * 列表页与首页共用，避免两套样式走偏。
 */
export default function ProductTile({ product }: { product: ProductCard }) {
  const navigate = useNavigate();
  const go = () => navigate(`/product/${product.id}`);

  return (
    <article
      className="sf-card"
      role="button"
      tabIndex={0}
      onClick={go}
      onKeyDown={(event) => event.key === 'Enter' && go()}
    >
      <div className="sf-card__media">
        <SafeImage src={product.coverImage} alt={product.name} />
        {product.totalAvailable > 0 && product.totalAvailable <= 5 && (
          <span className="sf-card__flag sf-card__flag--warn">仅剩 {product.totalAvailable} 件</span>
        )}
        <span className="sf-card__go">
          <ArrowRightOutlined />
        </span>
      </div>
      <div className="sf-card__body">
        <span className="sf-card__name">{product.name}</span>
        <div className="sf-card__meta">
          <span>{product.sellingPoint || '日常好物'}</span>
        </div>
        <div className="sf-card__meta">
          <div className="sf-card__prices">
            <span className="sf-card__price">
              <small>¥</small>
              {yuan(product.minPrice)}
            </span>
            {product.maxPrice > product.minPrice && <span className="sf-card__from">起</span>}
          </div>
          <span>可售 {product.totalAvailable}</span>
        </div>
      </div>
    </article>
  );
}
