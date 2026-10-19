import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { orderApi } from '@/api/mall';
import SafeImage from '@/components/SafeImage';
import { dt, money } from '@/utils/format';
import { toast } from '@/utils/feedback';
import type { OrderSummary } from '@/types/mall';

const STATUS_TABS = [
  { value: '', label: '全部' },
  { value: 'PENDING_PAYMENT', label: '待确认' },
  { value: 'PENDING_SHIPMENT', label: '待发货' },
  { value: 'PENDING_RECEIPT', label: '待收货' },
  { value: 'FINISHED', label: '已完成' },
];

const PAGE_SIZE = 10;

export default function MOrders() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [status, setStatus] = useState(params.get('status') ?? '');
  const [orders, setOrders] = useState<OrderSummary[]>([]);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [loading, setLoading] = useState(true);
  const [done, setDone] = useState(false);
  const [cancelReasons, setCancelReasons] = useState<string[]>(['不想要了', '信息填写错误', '重复下单']);

  const [cancelTarget, setCancelTarget] = useState<OrderSummary | null>(null);
  const [cancelReason, setCancelReason] = useState('不想要了');
  const [cancelling, setCancelling] = useState(false);

  const load = useCallback(
    async (page: number, replace: boolean) => {
      setLoading(true);
      try {
        const result = await orderApi.mine({
          status: status || undefined,
          pageNum: page,
          pageSize: PAGE_SIZE,
        });
        setOrders((prev) => (replace ? result.rows : [...prev, ...result.rows]));
        setTotal(result.total);
        setPageNum(page);
        setDone(page * PAGE_SIZE >= result.total);
      } catch {
        /* 拦截器已提示 */
      } finally {
        setLoading(false);
      }
    },
    [status],
  );

  useEffect(() => {
    void load(1, true);
  }, [load]);

  useEffect(() => {
    void orderApi
      .options()
      .then((options) => {
        if (options.cancelReasons?.length) {
          setCancelReasons(options.cancelReasons);
          setCancelReason(options.cancelReasons[0]);
        }
      })
      .catch(() => undefined);
  }, []);

  const doCancel = async () => {
    if (!cancelTarget) return;
    setCancelling(true);
    try {
      await orderApi.cancel(String(cancelTarget.id), cancelReason);
      toast.success('订单已取消');
      setCancelTarget(null);
      await load(1, true);
    } catch {
      /* 拦截器已提示 */
    } finally {
      setCancelling(false);
    }
  };

  const confirm = async (order: OrderSummary) => {
    try {
      await orderApi.confirmReceipt(String(order.id));
      toast.success('已确认收货');
      await load(1, true);
    } catch {
      /* 拦截器已提示 */
    }
  };

  return (
    <div>
      <div className="m-tabs">
        {STATUS_TABS.map((tab) => (
          <button
            key={tab.value}
            className={`m-tab${status === tab.value ? ' is-active' : ''}`}
            onClick={() => setStatus(tab.value)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {loading && orders.length === 0 && (
        <>
          <div className="m-skeleton" style={{ height: 150 }} />
          <div className="m-skeleton" style={{ height: 150 }} />
        </>
      )}

      {!loading && orders.length === 0 && (
        <div className="m-empty">
          这里还没有订单
          <div style={{ marginTop: 16 }}>
            <button className="m-btn m-btn--sm m-btn--primary" onClick={() => navigate('/m/list')}>
              去逛逛
            </button>
          </div>
        </div>
      )}

      {orders.map((order) => (
        <div
          key={order.id}
          className="m-order m-order--link"
          onClick={() => navigate(`/m/orders/${order.id}`)}
        >
          <div className="m-order__head">
            <span>{order.shopName}</span>
            <span className="m-order__head-right">
              <span className="m-order__status">{order.statusLabel}</span>
              <span className="m-order__arrow" aria-hidden>
                ›
              </span>
            </span>
          </div>

          <div className="m-order__goods">
            <SafeImage src={order.coverImage} alt="" className="m-order__img" />
            <div style={{ flex: 1, minWidth: 0 }}>
              <div className="m-clamp2" style={{ fontSize: 13, lineHeight: 1.5 }}>
                {order.productSummary || '商品信息'}
              </div>
              <div className="m-mute" style={{ marginTop: 4 }}>
                共 {order.itemCount} 件 · {dt(order.createTime)}
              </div>
            </div>
            <div style={{ textAlign: 'right' }}>
              <div className="m-price">{money(order.payAmount)}</div>
            </div>
          </div>

          {(order.canCancel || order.canConfirmReceipt) && (
            <div className="m-order__foot">
              {order.canCancel && (
                <button
                  className="m-btn m-btn--sm m-btn--ghost"
                  onClick={(event) => {
                    event.stopPropagation();
                    setCancelReason(cancelReasons[0]);
                    setCancelTarget(order);
                  }}
                >
                  取消订单
                </button>
              )}
              {order.canConfirmReceipt && (
                <button
                  className="m-btn m-btn--sm m-btn--ghost"
                  onClick={(event) => {
                    event.stopPropagation();
                    void confirm(order);
                  }}
                >
                  确认收货
                </button>
              )}
            </div>
          )}

          {order.status === 'PENDING_SHIPMENT' && (
            <div className="m-mute" style={{ marginTop: 8 }}>
              店主正在备货，发货后可在这里看到物流单号
            </div>
          )}
          {order.shipNo && (
            <div className="m-mute" style={{ marginTop: 8 }}>
              {order.shipCompany} · {order.shipNo}
            </div>
          )}
        </div>
      ))}

      {!loading && !done && orders.length > 0 && (
        <div style={{ padding: '0 12px 24px', textAlign: 'center' }}>
          <button className="m-btn m-btn--sm m-btn--ghost" onClick={() => void load(pageNum + 1, false)}>
            加载更多（{total} 单）
          </button>
        </div>
      )}

      {cancelTarget && (
        <>
          <div className="m-mask" onClick={() => setCancelTarget(null)} />
          <div className="m-sheet">
            <button className="m-sheet__close" onClick={() => setCancelTarget(null)}>×</button>
            <div className="m-sheet__title">取消订单</div>
            {cancelReasons.map((reason) => (
              <div
                key={reason}
                className={`m-picker-item${cancelReason === reason ? ' is-active' : ''}`}
                onClick={() => setCancelReason(reason)}
              >
                {reason}
              </div>
            ))}
            <button
              className="m-btn m-btn--danger m-btn--block"
              style={{ marginTop: 12 }}
              disabled={cancelling}
              onClick={() => void doCancel()}
            >
              确认取消
            </button>
          </div>
        </>
      )}
    </div>
  );
}
