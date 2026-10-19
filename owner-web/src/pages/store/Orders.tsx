import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Divider,
  Drawer,
  Empty,
  Form,
  Input,
  Modal,
  Pagination,
  Popconfirm,
  Radio,
  Rate,
  Select,
  Skeleton,
  Space,
  Tag,
  Timeline,
} from 'antd';
import { useNavigate } from 'react-router-dom';
import { orderApi, refundApi, reviewApi } from '@/api/mall';
import SafeImage from '@/components/SafeImage';
import { toast } from '@/utils/feedback';
import {
  dt,
  money,
  num,
  orderStatusColor,
  refundStatusColor,
} from '@/utils/format';
import type { OrderDetail, OrderSummary, Refund } from '@/types/mall';

const FILTERS = [
  { value: '', label: '全部' },
  { value: 'PENDING_PAYMENT', label: '待确认' },
  { value: 'PENDING_SHIPMENT', label: '待发货' },
  { value: 'PENDING_RECEIPT', label: '待收货' },
  { value: 'FINISHED', label: '已完成' },
  { value: 'REFUNDING', label: '退款中' },
  { value: 'REFUNDED', label: '已退款' },
  { value: 'CANCELLED', label: '已取消' },
];

export default function Orders() {
  const navigate = useNavigate();
  const [status, setStatus] = useState('');
  const [rows, setRows] = useState<OrderSummary[]>([]);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [loading, setLoading] = useState(true);

  const [detail, setDetail] = useState<OrderDetail | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);

  const [cancelReasons, setCancelReasons] = useState<string[]>([]);

  const [refundOrder, setRefundOrder] = useState<OrderSummary | null>(null);
  const [reviewOrder, setReviewOrder] = useState<OrderSummary | null>(null);

  const [refundForm] = Form.useForm();
  const [reviewForm] = Form.useForm();
  const [cancelForm] = Form.useForm();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const page = await orderApi.mine({ status: status || undefined, pageNum, pageSize: 10 });
      setRows(page.rows ?? []);
      setTotal(page.total ?? 0);
    } catch {
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [status, pageNum]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    void orderApi
      .options()
      .then((o) => setCancelReasons(o.cancelReasons ?? []))
      .catch(() => undefined);
  }, []);

  const openDetail = async (order: OrderSummary) => {
    setDetailOpen(true);
    setDetail(null);
    try {
      setDetail(await orderApi.detail(String(order.id)));
    } catch {
      setDetailOpen(false);
    }
  };

  const submitRefund = async () => {
    if (!refundOrder) return;
    try {
      const values = await refundForm.validateFields();
      await refundApi.apply(String(refundOrder.id), {
        type: values.type,
        reason: values.reason,
        description: values.description,
      });
      toast.success('售后申请已提交，等待店主处理');
      setRefundOrder(null);
      refundForm.resetFields();
      await load();
    } catch {
      /* 校验失败或接口报错 */
    }
  };

  const submitReview = async () => {
    if (!reviewOrder) return;
    try {
      const values = await reviewForm.validateFields();
      await reviewApi.submit({
        orderId: String(reviewOrder.id),
        score: values.score,
        content: values.content,
      });
      toast.success('评价已提交');
      setReviewOrder(null);
      reviewForm.resetFields();
      await load();
    } catch {
      /* 校验失败或接口报错 */
    }
  };

  const refundOfOrder: Refund | null = detail?.currentRefund ?? null;

  return (
    <div>
      <h2 className="mall-section-title">我的订单</h2>

      <Radio.Group
        value={status}
        options={FILTERS}
        optionType="button"
        buttonStyle="solid"
        style={{ marginBottom: 16 }}
        onChange={(e) => {
          setStatus(e.target.value);
          setPageNum(1);
        }}
      />

      {loading ? (
        <Skeleton active />
      ) : rows.length === 0 ? (
        <Card>
          <Empty description="没有订单">
            <Button type="primary" onClick={() => navigate('/products')}>
              去逛逛
            </Button>
          </Empty>
        </Card>
      ) : (
        rows.map((order) => (
          <Card
            key={order.id}
            style={{ marginBottom: 14 }}
            title={
              <Space size={12}>
                <span>{order.orderNo}</span>
                <Tag color={orderStatusColor(order.status)}>{order.statusLabel}</Tag>
              </Space>
            }
            extra={<span className="mall-card__meta">{dt(order.createTime)}</span>}
          >
            <div style={{ display: 'flex', gap: 14, alignItems: 'center' }}>
              <SafeImage
                src={order.coverImage}
                alt=""
                style={{ width: 64, height: 64, borderRadius: 10, objectFit: 'cover', background: '#f5f5f5' }}
              />
              <div style={{ flex: 1 }}>
                <div>{order.productSummary}</div>
                <div className="mall-card__meta">
                  共 {order.itemCount} 件 · {order.shopName}
                  {order.shipCompany ? ` · ${order.shipCompany} ${order.shipNo ?? ''}` : ''}
                </div>
              </div>
              <span className="mall-price" style={{ fontSize: 18 }}>{money(order.payAmount)}</span>
            </div>

            <Divider style={{ margin: '14px 0' }} />

            <Space wrap>
              <Button size="small" onClick={() => void openDetail(order)}>
                订单详情
              </Button>
              {order.canCancel && (
                <Popconfirm
                  title="取消订单"
                  icon={null}
                  description={
                    <Form form={cancelForm} layout="vertical" style={{ width: 240 }}>
                      <Form.Item name="reason" initialValue="不想要了" style={{ marginBottom: 0 }}>
                        <Select options={cancelReasons.map((r) => ({ value: r, label: r }))} />
                      </Form.Item>
                    </Form>
                  }
                  onConfirm={async () => {
                    const reason = cancelForm.getFieldValue('reason') || '不想要了';
                    await orderApi.cancel(String(order.id), reason);
                    toast.success('订单已取消');
                    await load();
                  }}
                >
                  <Button size="small">取消订单</Button>
                </Popconfirm>
              )}
              {order.canConfirmReceipt && (
                <Popconfirm
                  title="确认已收到货？"
                  onConfirm={async () => {
                    await orderApi.confirmReceipt(String(order.id));
                    toast.success('已确认收货');
                    await load();
                  }}
                >
                  <Button size="small" type="primary">
                    确认收货
                  </Button>
                </Popconfirm>
              )}
              {order.canApplyRefund && (
                <Button size="small" onClick={() => setRefundOrder(order)}>
                  申请售后
                </Button>
              )}
              {order.canReview && (
                <Button size="small" type="primary" onClick={() => setReviewOrder(order)}>
                  去评价
                </Button>
              )}
            </Space>
          </Card>
        ))
      )}

      {total > 10 && (
        <div style={{ textAlign: 'center', marginTop: 16 }}>
          <Pagination
            current={pageNum}
            total={total}
            pageSize={10}
            showSizeChanger={false}
            onChange={setPageNum}
          />
        </div>
      )}

      {/* 订单详情 */}
      <Drawer
        width={560}
        open={detailOpen}
        title="订单详情"
        onClose={() => setDetailOpen(false)}
      >
        {!detail ? (
          <Skeleton active />
        ) : (
          <>
            <Descriptions column={1} size="small" bordered>
              <Descriptions.Item label="订单号">{detail.summary.orderNo}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={orderStatusColor(detail.summary.status)}>{detail.summary.statusLabel}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="店铺">{detail.summary.shopName}</Descriptions.Item>
              <Descriptions.Item label="收货人">
                {detail.receiverName} {detail.receiverMobile}
                <div className="mall-card__meta">{detail.receiverAddress}</div>
              </Descriptions.Item>
              {detail.buyerRemark && (
                <Descriptions.Item label="买家留言">{detail.buyerRemark}</Descriptions.Item>
              )}
              {detail.payNo && (
                <Descriptions.Item label="支付信息">
                  {detail.payChannel} · {detail.payNo}
                </Descriptions.Item>
              )}
              <Descriptions.Item label="金额">
                商品 {money(detail.summary.goodsAmount)} + 运费 {money(detail.summary.freightAmount)} - 优惠{' '}
                {money(detail.summary.discountAmount)} = <b>{money(detail.summary.payAmount)}</b>
              </Descriptions.Item>
              {detail.summary.shipCompany && (
                <Descriptions.Item label="物流">
                  {detail.summary.shipCompany} {detail.summary.shipNo}
                </Descriptions.Item>
              )}
              {detail.summary.cancelReason && (
                <Descriptions.Item label="取消原因">{detail.summary.cancelReason}</Descriptions.Item>
              )}
            </Descriptions>

            <Divider>商品</Divider>
            {detail.items.map((item) => (
              <div key={item.id} style={{ display: 'flex', gap: 12, alignItems: 'center', padding: '6px 0' }}>
                <SafeImage
                  src={item.imageUrl}
                  alt=""
                  style={{ width: 48, height: 48, borderRadius: 8, objectFit: 'cover', background: '#f5f5f5' }}
                />
                <div style={{ flex: 1 }}>
                  <div>{item.productName}</div>
                  <div className="mall-card__meta">{item.specName} × {item.quantity}</div>
                </div>
                <span className="mall-price">{money(item.amount)}</span>
              </div>
            ))}

            {refundOfOrder && (
              <>
                <Divider>售后</Divider>
                <Alert
                  type="warning"
                  showIcon
                  message={
                    <Space>
                      <Tag color={refundStatusColor(refundOfOrder.status)}>
                        {refundOfOrder.statusLabel}
                      </Tag>
                      {refundOfOrder.typeLabel} · {money(refundOfOrder.refundAmount)}
                    </Space>
                  }
                  description={
                    <div>
                      <div>原因：{refundOfOrder.reason}</div>
                      {refundOfOrder.description && <div>说明：{refundOfOrder.description}</div>}
                      {refundOfOrder.refuseReason && <div>拒绝理由：{refundOfOrder.refuseReason}</div>}
                      {refundOfOrder.returnAddress && (
                        <div>退货地址：{refundOfOrder.returnAddress}</div>
                      )}
                      {refundOfOrder.status === 'WAITING_RETURN' && (
                        <Button
                          size="small"
                          type="primary"
                          style={{ marginTop: 8 }}
                          onClick={() =>
                            navigate('/account?tab=refunds')
                          }
                        >
                          去填写退货运单号
                        </Button>
                      )}
                    </div>
                  }
                />
              </>
            )}

            <Divider>流转记录</Divider>
            <Timeline
              items={(detail.logs ?? []).map((log) => ({
                children: (
                  <div>
                    <div>
                      {log.fromStatusLabel ? `${log.fromStatusLabel} → ` : ''}
                      <b>{log.toStatusLabel}</b>
                    </div>
                    <div className="mall-card__meta">
                      {log.operatorTypeLabel} · {dt(log.createTime)}
                      {log.remark ? ` · ${log.remark}` : ''}
                    </div>
                  </div>
                ),
              }))}
            />
          </>
        )}
      </Drawer>

      {/* 申请售后 */}
      <Modal
        open={Boolean(refundOrder)}
        title={`申请售后 · ${refundOrder?.orderNo ?? ''}`}
        onCancel={() => setRefundOrder(null)}
        onOk={() => void submitRefund()}
        okText="提交申请"
      >
        <Alert
          style={{ marginBottom: 16 }}
          type="info"
          showIcon
          message="售后按整单处理"
          description={
            refundOrder?.status === 'PENDING_SHIPMENT'
              ? '当前订单未发货，只能申请「仅退款」。店主同意后原路退回并恢复库存。'
              : '当前订单已发货，请申请「退货退款」。店主同意后会给出退货地址，你寄回并填写运单号，店主确认收货后退款。'
          }
        />
        <Form
          form={refundForm}
          layout="vertical"
          initialValues={{
            type: refundOrder?.status === 'PENDING_SHIPMENT' ? 'REFUND_ONLY' : 'RETURN_REFUND',
            reason: '不想要了',
          }}
        >
          <Form.Item name="type" label="售后类型" rules={[{ required: true }]}>
            <Radio.Group
              options={
                refundOrder?.status === 'PENDING_SHIPMENT'
                  ? [{ value: 'REFUND_ONLY', label: '仅退款' }]
                  : [{ value: 'RETURN_REFUND', label: '退货退款' }]
              }
            />
          </Form.Item>
          <Form.Item name="reason" label="原因" rules={[{ required: true, message: '请选择原因' }]}>
            <Select
              options={[
                '不想要了',
                '拍错了',
                '商品质量问题',
                '商品与描述不符',
                '少件/漏发',
                '收到商品破损',
              ].map((r) => ({ value: r, label: r }))}
            />
          </Form.Item>
          <Form.Item name="description" label="补充说明">
            <Input.TextArea rows={3} maxLength={300} placeholder="选填，描述一下情况" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 评价 */}
      <Modal
        open={Boolean(reviewOrder)}
        title={`评价订单 · ${reviewOrder?.orderNo ?? ''}`}
        onCancel={() => setReviewOrder(null)}
        onOk={() => void submitReview()}
        okText="提交评价"
      >
        <p className="mall-card__meta">一笔订单只能评价一次，提交后不能修改或删除。</p>
        <Form form={reviewForm} layout="vertical" initialValues={{ score: 5 }}>
          <Form.Item name="score" label="评分" rules={[{ required: true }]}>
            <Rate />
          </Form.Item>
          <Form.Item name="content" label="评价内容">
            <Input.TextArea rows={4} maxLength={500} placeholder="说点什么，帮助其他顾客" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
