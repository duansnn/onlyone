import { Link } from 'react-router-dom';
import SafeImage from '@/components/SafeImage';
import { num } from '@/utils/format';
import type { ProductCard } from '@/types/mall';

/** 手机端商品卡：两列网格用，图片 1:1，价格红色 */
export default function MProductCard({ product }: { product: ProductCard }) {
  const price = num(product.minPrice);
  const max = num(product.maxPrice);
  return (
    <Link to={`/m/product/${product.id}`} className="m-product">
      <SafeImage src={product.coverImage} alt={product.name} className="m-product__img" />
      <div className="m-product__body">
        <div className="m-product__name m-clamp2">{product.name}</div>
        <div className="m-product__foot">
          <span className="m-price">
            <small>¥</small>
            {(price / 100).toFixed(2)}
            {max > price && <small> 起</small>}
          </span>
          <span className="m-product__sold">
            {product.totalAvailable > 0 ? `剩 ${product.totalAvailable}` : '缺货'}
          </span>
        </div>
      </div>
    </Link>
  );
}
