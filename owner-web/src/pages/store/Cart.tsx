import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Card, Checkbox, Empty, InputNumber, Popconfirm, Skeleton, Space, Tag } from 'antd';
import { useNavigate } from 'react-router-dom';
import { cartApi } from '@/api/mall';
import SafeImage from '@/components/SafeImage';
import { useAuthStore } from '@/store/auth';
import { toast } from '@/utils/feedback';
import { money, num } from '@/utils/format';
import type { Cart as CartType, CartItem } from '@/types/mall';

export default function Cart() {
  const navigate = useNavigate();
  const shops = useAuthStore((state) => state.shops);
  const [cart, setCart] = useState<CartType | null>(null);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<string[]>([]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await cartApi.list();
      setCart(data);
      setSelected((prev) => {
        const valid = new Set(
          (data.shops ?? []).flatMap((g) => g.items.map((i) => String(i.skuId))),
        );
        const kept = prev.filter((id) => valid.has(id));
        return kept.length ? kept : [...valid];
      });
    } catch {
      setCart(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const groups = cart?.shops ?? [];

  const selectedItems = useMemo(() => {
    const all: CartItem[] = groups.flatMap((g) => g.items);
    return all.filter((i) => selected.includes(String(i.skuId)));
  }, [groups, selected]);

  const totalAmount = selectedItems.reduce((sum, i) => sum + num(i.price) * i.quantity, 0);

  const toggle = (skuId: string, checked: boolean) =>
    setSelected((prev) => (checked ? [...new Set([...prev, skuId])] : prev.filter((id) => id !== skuId)));

  const changeQuantity = async (item: CartItem, quantity: number) => {
    try {
      await cartApi.updateQuantity({ skuId: String(item.skuId), quantity });
      await load();
    } catch {
      /* 拦截器已提示 */
    }
  };

  const removeItem = async (item: CartItem) => {
    try {
      await cartApi.remove(String(item.skuId));
      toast.success('已移出购物车');
      await load();
    } catch {
      /* 拦截器已提示 */
    }
  };

  const checkout = () => {
    if (!selectedItems.length) {
      toast.warning('请先选择要结算的商品');
      return;
    }
    const shopId = groups.find((g) => g.items.some((i) => selected.includes(String(i.skuId))))?.shopId;
    if (!shopId) {
      toast.warning('未找到商品所属店铺');
      return;
    }
    // 自己店铺的货不能自己买：自购会刷虚假销量、自己领券自己核销
    const myRole = shops.find((s) => String(s.shopId) === String(shopId))?.role;
    if (myRole) {
      toast.warning(
        myRole === 'OWNER'
          ? '你是本店店主，不能用店主账号在自己店铺下单'
          : '你是本店店员，不能用员工账号在自己店铺下单',
      );
      return;
    }
    // 一次只能结算一家店的商品（后端按店铺拆单）；price 必须回传，后端会校验价格是否被改过
    const items = groups
      .filter((g) => String(g.shopId) === String(shopId))
      .flatMap((g) => g.items)
      .filter((i) => selected.includes(String(i.skuId)))
      .map((i) => ({ skuId: String(i.skuId), quantity: i.quantity, price: num(i.price) }));
    sessionStorage.setItem('checkout:payload', JSON.stringify({ shopId: String(shopId), items }));
    navigate('/checkout');
  };

  if (loading) return <Skeleton active />;

  if (!cart || groups.length === 0) {
    return (
      <Card>
        <Empty description="购物车还是空的" />
        <div style={{ textAlign: 'center', marginTop: 16 }}>
          <Button type="primary" onClick={() => navigate('/products')}>
            去挑点东西
          </Button>
        </div>
      </Card>
    );
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 className="mall-section-title" style={{ margin: 0 }}>
          购物车（{cart.itemCount}）
        </h2>
        <Popconfirm
          title="确认清空购物车？"
          onConfirm={async () => {
            await cartApi.clear();
            toast.success('购物车已清空');
            await load();
          }}
        >
          <Button type="link" danger>
            清空
          </Button>
        </Popconfirm>
      </div>

      {groups.map((group) => (
        <Card
          key={group.shopId}
          style={{ marginBottom: 14 }}
          title={<Space>{group.shopName}</Space>}
          extra={group.shopStatus !== 0 ? <Tag color="red">店铺已停用</Tag> : null}
        >
          {group.items.map((item) => (
            <div
              key={item.skuId}
              style={{ display: 'flex', gap: 14, alignItems: 'center', padding: '10px 0' }}
            >
              <Checkbox
                checked={selected.includes(String(item.skuId))}
                onChange={(e) => toggle(String(item.skuId), e.target.checked)}
              />
              <SafeImage
                src={item.imageUrl}
                alt={item.productName}
                style={{ width: 64, height: 64, borderRadius: 10, objectFit: 'cover', background: '#f5f5f5' }}
              />
              <div style={{ flex: 1, minWidth: 160 }}>
                <div>{item.productName}</div>
                <div className="mall-card__meta">
                  {item.specName} · 可售 {item.availableStock}
                </div>
              </div>
              <span className="mall-price">{money(item.price)}</span>
              <InputNumber
                min={1}
                max={Math.max(1, item.availableStock)}
                value={item.quantity}
                onChange={(v) => void changeQuantity(item, Number(v) || 1)}
              />
              <span style={{ width: 90, textAlign: 'right' }} className="mall-price">
                {money(num(item.price) * item.quantity)}
              </span>
              <Button type="link" danger onClick={() => void removeItem(item)}>
                删除
              </Button>
            </div>
          ))}

          {(group.invalidItems ?? []).length > 0 && (
            <div style={{ marginTop: 8 }}>
              {(group.invalidItems ?? []).map((item) => (
                <div
                  key={item.skuId}
                  style={{ display: 'flex', gap: 14, alignItems: 'center', padding: '10px 0', opacity: 0.55 }}
                >
                  <Checkbox disabled />
                  <SafeImage
                    src={item.imageUrl}
                    alt=""
                    style={{ width: 64, height: 64, borderRadius: 10, objectFit: 'cover' }}
                  />
                  <div style={{ flex: 1 }}>
                    <div>{item.productName}</div>
                    <div className="mall-card__meta">{item.specName} · {item.invalidReason}</div>
                  </div>
                  <Button type="link" danger onClick={() => void removeItem(item)}>
                    删除
                  </Button>
                </div>
              ))}
            </div>
          )}
        </Card>
      ))}

      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
          <Space size={16}>
            <Checkbox
              indeterminate={selected.length > 0 && selected.length < groups.flatMap((g) => g.items).length}
              checked={
                groups.flatMap((g) => g.items).length > 0 &&
                selected.length === groups.flatMap((g) => g.items).length
              }
              onChange={(e) =>
                setSelected(
                  e.target.checked ? groups.flatMap((g) => g.items).map((i) => String(i.skuId)) : [],
                )
              }
            >
              全选
            </Checkbox>
            <span className="mall-card__meta">
              已选 {selectedItems.length} 种
            </span>
          </Space>
          <Space size={16}>
            <span>
              合计 <span className="mall-price" style={{ fontSize: 22 }}>{money(totalAmount)}</span>
            </span>
            <Button type="primary" size="large" onClick={checkout}>
              去结算
            </Button>
          </Space>
        </div>
      </Card>
    </div>
  );
}
