import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { orderApi } from '@/api/mall';
import SafeImage from '@/components/SafeImage';
import { dt, money } from '@/utils/format';
import { toast } from '@/utils/feedback';
import type { OrderDetail, OrderStatus } from '@/types/mall';

/** 各状态给一句人话，避免顾客自己猜 */
const STATUS_TIP: Partial<Record<OrderStatus, string>> = {
  PENDING_PAYMENT: '订单已提交，商家确认收款后安排发货',
  PENDING_SHIPMENT: '商家正在备货，发货后可在这里看到物流单号',
  PENDING_RECEIPT: '商品已发出，收到货后请点「确认收货」',
  FINISHED: '订单已完成，感谢你的支持',
  CANCELLED: '订单已取消，如已锁定库存会自动释放',
  REFUNDING: '售后处理中，商家处理后会通知你',
  REFUNDED: '退款已完成',
};

export default function MOrderDetail() {
  const { orderId = '' } = useParams();
  const [data, setData] = useState<OrderDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [cancelOpen, setCancelOpen] = useState(false);
  const [cancelReasons, setCancelReasons] = useState<string[]>(['不想要了', '信息填写错误', '重复下单']);
  const [cancelReason, setCancelReason] = useState('不想要了');
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    if (!orderId) return;
    setLoading(true);
    try {
      setData(await orderApi.detail(orderId));
    } catch {
      /* 拦截器已提示 */
    } finally {
      setLoading(false);
    }
  }, [orderId]);

  useEffect(() => {
    void load();
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
    if (!data) return;
    setBusy(true);
    try {
      await orderApi.cancel(String(data.summary.id), cancelReason);
      toast.success('订单已取消');
      setCancelOpen(false);
      await load();
    } catch {
      /* 拦截器已提示 */
    } finally {
      setBusy(false);
    }
  };

  const confirmReceipt = async () => {
    if (!data) return;
    setBusy(true);
    try {
      await orderApi.confirmReceipt(String(data.summary.id));
      toast.success('已确认收货');
      await load();
    } catch {
      /* 拦截器已提示 */
    } finally {
      setBusy(false);
    }
  };

  if (loading && !data) {
    return (
      <div style={{ padding: 12 }}>
        <div className="m-skeleton" style={{ height: 90 }} />
        <div className="m-skeleton" style={{ height: 150, marginTop: 12 }} />
        <div className="m-skeleton" style={{ height: 120, marginTop: 12 }} />
      </div>
    );
  }

  if (!data) {
    return (
      <div className="m-empty">
        订单不存在或无权查看
        <div style={{ marginTop: 16 }}>
          <Link to="/m/orders" className="m-btn m-btn--sm m-btn--primary">
            返回订单列表
          </Link>
        </div>
      </div>
    );
  }

  const { summary, items, logs, currentRefund } = data;
  const actions = summary.canCancel || summary.canConfirmReceipt;

  return (
    <div className="m-od">
      {/* 状态卡 */}
      <div className="m-od__status">
        <div className="m-od__status-name">{summary.statusLabel}</div>
        <div className="m-od__status-tip">{STATUS_TIP[summary.status] ?? ''}</div>
      </div>

      {/* 收货信息 */}
      <div className="m-card m-od__card">
        <div className="m-od__addr-name">
          {data.receiverName}
          <span className="m-mute" style={{ marginLeft: 8, fontWeight: 400 }}>
            {data.receiverMobile}
          </span>
        </div>
        <div className="m-od__addr-text">{data.receiverAddress}</div>
      </div>

      {/* 商品 */}
      <div className="m-card m-od__card">
        <div className="m-od__card-title">{summary.shopName}</div>
        {items.map((item) => (
          <div key={String(item.id ?? item.skuId)} className="m-od__goods">
            <SafeImage src={item.imageUrl} alt="" className="m-od__goods-img" />
            <div style={{ flex: 1, minWidth: 0 }}>
              <div className="m-clamp2" style={{ fontSize: 13, lineHeight: 1.5 }}>
                {item.productName}
              </div>
              {item.specName && (
                <div className="m-mute" style={{ marginTop: 4 }}>
                  {item.specName}
                </div>
              )}
              <div className="m-mute" style={{ marginTop: 6 }}>
                {money(item.price)} × {item.quantity}
              </div>
            </div>
            <div className="m-price" style={{ fontSize: 14 }}>
              {money(item.amount)}
            </div>
          </div>
        ))}

        <div className="m-od__rows">
          <div className="m-od__row">
            <span className="m-od__label">商品金额</span>
            <span>{money(summary.goodsAmount)}</span>
          </div>
          <div className="m-od__row">
            <span className="m-od__label">运费</span>
            <span>{money(summary.freightAmount)}</span>
          </div>
          {summary.discountAmount > 0 && (
            <div className="m-od__row">
              <span className="m-od__label">优惠</span>
              <span style={{ color: '#e11d48' }}>-{money(summary.discountAmount)}</span>
            </div>
          )}
          <div className="m-od__row m-od__row--total">
            <span className="m-od__label">应付金额</span>
            <span className="m-price" style={{ fontSize: 17 }}>
              {money(summary.payAmount)}
            </span>
          </div>
        </div>
      </div>

      {/* 订单信息 */}
      <div className="m-card m-od__card">
        <div className="m-od__card-title">订单信息</div>
        <div className="m-od__row">
          <span className="m-od__label">订单号</span>
          <span className="m-od__value" style={{ userSelect: 'text' }}>
            {summary.orderNo}
          </span>
        </div>
        <div className="m-od__row">
          <span className="m-od__label">下单时间</span>
          <span className="m-od__value">{dt(summary.createTime)}</span>
        </div>
        {summary.payTime && (
          <div className="m-od__row">
            <span className="m-od__label">收款时间</span>
            <span className="m-od__value">{dt(summary.payTime)}</span>
          </div>
        )}
        {data.payChannel && (
          <div className="m-od__row">
            <span className="m-od__label">收款方式</span>
            <span className="m-od__value">{data.payChannel}</span>
          </div>
        )}
        {summary.shipTime && (
          <div className="m-od__row">
            <span className="m-od__label">发货时间</span>
            <span className="m-od__value">{dt(summary.shipTime)}</span>
          </div>
        )}
        {summary.shipNo && (
          <div className="m-od__row">
            <span className="m-od__label">物流</span>
            <span className="m-od__value">
              {summary.shipCompany} {summary.shipNo}
            </span>
          </div>
        )}
        {data.buyerRemark && (
          <div className="m-od__row">
            <span className="m-od__label">备注</span>
            <span className="m-od__value">{data.buyerRemark}</span>
          </div>
        )}
        {summary.cancelReason && (
          <div className="m-od__row">
            <span className="m-od__label">取消原因</span>
            <span className="m-od__value">{summary.cancelReason}</span>
          </div>
        )}
      </div>

      {/* 售后 */}
      {currentRefund && (
        <div className="m-card m-od__card">
          <div className="m-od__card-title">售后</div>
          <div className="m-od__row">
            <span className="m-od__label">当前状态</span>
            <span className="m-od__value">{currentRefund.statusLabel}</span>
          </div>
          {currentRefund.reason && (
            <div className="m-od__row">
              <span className="m-od__label">申请原因</span>
              <span className="m-od__value">{currentRefund.reason}</span>
            </div>
          )}
          {currentRefund.refuseReason && (
            <div className="m-od__row">
              <span className="m-od__label">商家说明</span>
              <span className="m-od__value">{currentRefund.refuseReason}</span>
            </div>
          )}
        </div>
      )}

      {/* 状态记录 */}
      {logs.length > 0 && (
        <div className="m-card m-od__card">
          <div className="m-od__card-title">订单进度</div>
          {logs.map((log) => (
            <div key={String(log.id)} className="m-log">
              <b>{log.toStatusLabel}</b>
              {log.remark ? <div style={{ marginTop: 2 }}>{log.remark}</div> : null}
              <div style={{ marginTop: 2 }}>{dt(log.createTime)}</div>
            </div>
          ))}
        </div>
      )}

      <div style={{ height: actions ? 104 : 24 }} />

      {/* 底部操作条：这里没有「去支付」，支付功能未接入也不做假按钮 */}
      {actions && (
        <div className="m-detailbar">
          <div style={{ marginRight: 'auto' }}>
            <div className="m-mute">应付</div>
            <div className="m-price">{money(summary.payAmount)}</div>
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            {summary.canCancel && (
              <button
                className="m-btn m-btn--ghost"
                style={{ height: 42 }}
                onClick={() => setCancelOpen(true)}
              >
                取消订单
              </button>
            )}
            {summary.canConfirmReceipt && (
              <button
                className="m-btn m-btn--primary"
                style={{ height: 42 }}
                disabled={busy}
                onClick={() => void confirmReceipt()}
              >
                确认收货
              </button>
            )}
          </div>
        </div>
      )}

      {cancelOpen && (
        <>
          <div className="m-mask" onClick={() => setCancelOpen(false)} />
          <div className="m-sheet">
            <button className="m-sheet__close" onClick={() => setCancelOpen(false)}>
              ×
            </button>
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
              disabled={busy}
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
