import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { cartApi } from '@/api/mall';
import SafeImage from '@/components/SafeImage';
import { money, num } from '@/utils/format';
import { toast } from '@/utils/feedback';
import type { Cart, CartItem } from '@/types/mall';

export default function MCart() {
  const navigate = useNavigate();
  const [cart, setCart] = useState<Cart | null>(null);
  const [loading, setLoading] = useState(true);
  const [busySku, setBusySku] = useState('');

  const load = useCallback(async () => {
    try {
      const data = await cartApi.list();
      setCart(data);
    } catch {
      /* 拦截器已提示 */
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const changeQty = async (item: CartItem, quantity: number) => {
    if (quantity < 1 || quantity > item.availableStock) return;
    setBusySku(String(item.skuId));
    try {
      const data = await cartApi.updateQuantity({ skuId: String(item.skuId), quantity });
      setCart(data);
    } catch {
      /* 拦截器已提示 */
    } finally {
      setBusySku('');
    }
  };

  const remove = async (item: CartItem) => {
    setBusySku(String(item.skuId));
    try {
      await cartApi.remove(String(item.skuId));
      await load();
    } catch {
      /* 拦截器已提示 */
    } finally {
      setBusySku('');
    }
  };

  const shops = cart?.shops ?? [];
  const validGroups = shops.filter((group) => group.items.length > 0);
  const totalCount = validGroups.reduce(
    (sum, group) => sum + group.items.reduce((s, item) => s + item.quantity, 0),
    0,
  );
  const totalAmount = validGroups.reduce(
    (sum, group) => sum + group.items.reduce((s, item) => s + num(item.price) * item.quantity, 0),
    0,
  );

  const checkout = () => {
    if (validGroups.length === 0) {
      toast.warning('购物车是空的');
      return;
    }
    if (validGroups.length > 1) {
      toast.warning('手机端暂不支持跨店铺合并结算，请分开下单');
      return;
    }
    const group = validGroups[0];
    sessionStorage.setItem(
      'checkout:payload',
      JSON.stringify({
        shopId: String(group.shopId),
        items: group.items.map((item) => ({
          skuId: String(item.skuId),
          quantity: item.quantity,
          price: num(item.price),
        })),
      }),
    );
    navigate('/m/checkout');
  };

  if (loading) {
    return (
      <div>
        <div className="m-skeleton" />
        <div className="m-skeleton" />
      </div>
    );
  }

  if (shops.length === 0) {
    return (
      <div className="m-empty">
        购物车还是空的
        <div style={{ marginTop: 16 }}>
          <Link to="/m/list" className="m-btn m-btn--sm m-btn--primary">
            去逛逛
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div style={{ paddingBottom: validGroups.length > 0 ? 76 : 0 }}>
      {shops.map((group) => (
        <div key={group.shopId} className="m-card" style={{ padding: '0 12px' }}>
          <div className="m-row" style={{ height: 42 }}>
            <span style={{ fontSize: 14, fontWeight: 600 }}>{group.shopName}</span>
            {group.shopStatus !== 1 && <span className="m-tag m-tag--warn">店铺休息中</span>}
          </div>

          {group.items.map((item) => (
            <div key={item.skuId} className="m-cart-item" style={{ paddingLeft: 0, paddingRight: 0 }}>
              <Link to={`/m/product/${item.productId}`}>
                <SafeImage src={item.imageUrl} alt={item.productName} className="m-cart-item__img" />
              </Link>
              <div className="m-cart-item__body">
                <div className="m-cart-item__name m-clamp2">{item.productName}</div>
                <div className="m-cart-item__spec">
                  {item.specName}
                  {!item.valid && ` · ${item.invalidReason ?? '已失效'}`}
                </div>
                <div className="m-cart-item__foot">
                  <span className="m-price">{money(item.price)}</span>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <button
                      onClick={() => void remove(item)}
                      disabled={busySku === String(item.skuId)}
                      style={{ border: 0, background: 'none', color: '#a89f95', fontSize: 12, padding: 0 }}
                    >
                      删除
                    </button>
                    <div className="m-stepper">
                      <button
                        disabled={busySku === String(item.skuId) || item.quantity <= 1}
                        onClick={() => void changeQty(item, item.quantity - 1)}
                      >
                        −
                      </button>
                      <span>{item.quantity}</span>
                      <button
                        disabled={busySku === String(item.skuId) || item.quantity >= item.availableStock}
                        onClick={() => void changeQty(item, item.quantity + 1)}
                      >
                        +
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))}

          {(group.invalidItems ?? []).map((item) => (
            <div key={`invalid-${item.skuId}`} className="m-cart-item m-cart-invalid" style={{ paddingLeft: 0, paddingRight: 0 }}>
              <SafeImage src={item.imageUrl} alt={item.productName} className="m-cart-item__img" />
              <div className="m-cart-item__body">
                <div className="m-cart-item__name m-clamp2">{item.productName}</div>
                <div className="m-cart-item__spec">{item.invalidReason ?? '已失效'}</div>
                <div className="m-cart-item__foot">
                  <span className="m-mute">不可结算</span>
                  <button
                    onClick={() => void remove(item)}
                    style={{ border: 0, background: 'none', color: '#a89f95', fontSize: 12, padding: 0 }}
                  >
                    移除
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      ))}

      {validGroups.length > 0 && (
        <div className="m-bottombar">
          <div className="m-bottombar__sum">
            <div className="m-mute">共 {totalCount} 件</div>
            <div>
              合计 <span className="m-price" style={{ fontSize: 20 }}>{money(totalAmount)}</span>
            </div>
          </div>
          <button className="m-btn m-btn--primary" style={{ minWidth: 116 }} onClick={checkout}>
            去结算
          </button>
        </div>
      )}
    </div>
  );
}
