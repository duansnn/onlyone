import { useCallback, useEffect, useRef, useState } from 'react';
import {
  Button,
  Card,
  DatePicker,
  Descriptions,
  Divider,
  Drawer,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Timeline,
} from 'antd';
import { useSearchParams } from 'react-router-dom';
import dayjs from 'dayjs';
import { adminOrderApi } from '@/api/admin';
import SafeImage from '@/components/SafeImage';
import { useAuthStore } from '@/store/auth';
import { toast } from '@/utils/feedback';
import { dt, money, orderStatusTone } from '@/utils/format';
import StatusTag from '@/components/StatusTag';
import type { OrderDetail, OrderSummary } from '@/types/mall';

const STATUSES = [
  { value: '', label: '全部' },
  { value: 'PENDING_PAYMENT', label: '待确认' },
  { value: 'PENDING_SHIPMENT', label: '待发货' },
  { value: 'PENDING_RECEIPT', label: '待收货' },
  { value: 'FINISHED', label: '已完成' },
  { value: 'REFUNDING', label: '退款中' },
  { value: 'REFUNDED', label: '已退款' },
  { value: 'CANCELLED', label: '已取消' },
];

export default function AdminOrders() {
  const shopId = useAuthStore((state) => state.currentShopId) ?? '';
  const [searchParams, setSearchParams] = useSearchParams();
  const [rows, setRows] = useState<OrderSummary[]>([]);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [loading, setLoading] = useState(true);
  const [companies, setCompanies] = useState<string[]>([]);
  const [detail, setDetail] = useState<OrderDetail | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const [shipTarget, setShipTarget] = useState<OrderSummary | null>(null);
  const [shipForm] = Form.useForm();
  const [filterForm] = Form.useForm();

  const status = searchParams.get('status') ?? '';

  const load = useCallback(async () => {
    if (!shopId) return;
    setLoading(true);
    try {
      const values = filterForm.getFieldsValue();
      const page = await adminOrderApi.page(shopId, {
        status: status || undefined,
        orderNo: values.orderNo || undefined,
        receiverMobile: values.receiverMobile || undefined,
        dateFrom: values.range?.[0] ? values.range[0].format('YYYY-MM-DD') : undefined,
        dateTo: values.range?.[1] ? values.range[1].format('YYYY-MM-DD') : undefined,
        pageNum,
        pageSize: 10,
      });
      setRows(page.rows ?? []);
      setTotal(page.total ?? 0);
    } catch {
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [shopId, status, pageNum, filterForm]);

  useEffect(() => {
    void load();
  }, [load]);

  // 看板「今日订单」点进来带 from/to，落到同一个日期区间；只应用一次，避免反复重查
  const rangeFrom = searchParams.get('from') ?? '';
  const rangeTo = searchParams.get('to') ?? '';
  const appliedRange = useRef('');
  useEffect(() => {
    if (!rangeFrom || !rangeTo) return;
    const key = `${rangeFrom}~${rangeTo}`;
    if (appliedRange.current === key) return;
    appliedRange.current = key;
    filterForm.setFieldsValue({ range: [dayjs(rangeFrom), dayjs(rangeTo)] });
    void load();
  }, [rangeFrom, rangeTo, filterForm, load]);

  useEffect(() => {
    if (!shopId) return;
    void adminOrderApi
      .shipCompanies(shopId)
      .then((list) => setCompanies(list ?? []))
      .catch(() => undefined);
  }, [shopId]);

  const openDetail = async (order: OrderSummary) => {
    setDetailOpen(true);
    setDetail(null);
    try {
      setDetail(await adminOrderApi.detail(shopId, String(order.id)));
    } catch {
      setDetailOpen(false);
    }
  };

  /** 线下收款确认：不与任何支付平台交互，只是把订单推进到待发货 */
  const confirmPaid = async (row: OrderSummary) => {
    try {
      await adminOrderApi.confirmPayment(shopId, String(row.id));
      toast.success('已确认收款，订单转为待发货');
      await load();
    } catch {
      /* 拦截器已提示 */
    }
  };

  const submitShip = async () => {
    if (!shipTarget) return;
    const values = await shipForm.validateFields();
    try {
      if (shipTarget.status === 'PENDING_SHIPMENT') {
        await adminOrderApi.ship(shopId, String(shipTarget.id), values);
        toast.success('已发货');
      } else {
        await adminOrderApi.updateShip(shopId, String(shipTarget.id), values);
        toast.success('运单信息已更新');
      }
      setShipTarget(null);
      shipForm.resetFields();
      await load();
    } catch {
      /* 拦截器已提示 */
    }
  };

  return (
    <div>
      <div className="admin-page-head">
        <div>
          <h2 className="mall-section-title">订单与发货</h2>
          <p className="admin-page-head__desc">
            全店所有顾客的订单 · 当前筛选 <b>{total}</b> 单（你自己下的单在店面「我的订单」里）
          </p>
        </div>
      </div>

      <Card style={{ marginBottom: 16 }}>
        <Space wrap align="end">
          <Select
            value={status}
            options={STATUSES}
            style={{ width: 140 }}
            onChange={(value) => {
              const next = new URLSearchParams(searchParams);
              if (value) next.set('status', value);
              else next.delete('status');
              setSearchParams(next);
              setPageNum(1);
            }}
          />
          <Form form={filterForm} layout="inline" onFinish={() => void load()}>
            <Form.Item name="orderNo" label="订单号">
              <Input allowClear placeholder="精确" style={{ width: 180 }} />
            </Form.Item>
            <Form.Item name="receiverMobile" label="收件手机">
              <Input allowClear placeholder="精确" style={{ width: 150 }} />
            </Form.Item>
            <Form.Item name="range" label="下单日期">
              <DatePicker.RangePicker />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit">
                查询
              </Button>
            </Form.Item>
            <Form.Item>
              <Button
                onClick={() => {
                  filterForm.resetFields();
                  void load();
                }}
              >
                重置
              </Button>
            </Form.Item>
          </Form>
        </Space>
      </Card>

      <Table
        rowKey="id"
        scroll={{ x: 'max-content' }}
        loading={loading}
        dataSource={rows}
        pagination={{
          current: pageNum,
          pageSize: 10,
          total,
          showSizeChanger: false,
          showTotal: (t) => `共 ${t} 单`,
          onChange: setPageNum,
        }}
        columns={[
          {
            title: '订单',
            render: (_, r) => (
              <div>
                <div>{r.orderNo}</div>
                <div className="mall-card__meta">{dt(r.createTime)}</div>
              </div>
            ),
          },
          {
            title: '商品',
            render: (_, r) => (
              <Space>
                <SafeImage
                  src={r.coverImage}
                  alt=""
                  style={{ width: 40, height: 40, borderRadius: 8, objectFit: 'cover' }}
                />
                <div style={{ maxWidth: 220 }}>
                  <div style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {r.productSummary}
                  </div>
                  <div className="mall-card__meta">共 {r.itemCount} 件</div>
                </div>
              </Space>
            ),
          },
          { title: '收件手机', dataIndex: 'receiverMobileMasked', width: 130 },
          {
            title: '金额',
            width: 110,
            render: (_, r) => <span className="mall-price">{money(r.payAmount)}</span>,
          },
          {
            title: '状态',
            width: 100,
            render: (_, r) => <StatusTag tone={orderStatusTone(r.status)}>{r.statusLabel}</StatusTag>,
          },
          {
            title: '物流',
            width: 160,
            render: (_, r) =>
              r.shipCompany ? `${r.shipCompany} ${r.shipNo ?? ''}` : <span className="mall-card__meta">-</span>,
          },
          {
            title: '操作',
            width: 220,
            render: (_, r) => (
              <Space>
                <Button size="small" onClick={() => void openDetail(r)}>
                  详情
                </Button>
                {r.status === 'PENDING_PAYMENT' && (
                  <Popconfirm
                    title="确认已经收到这笔钱？"
                    description="确认后订单转为待发货。系统不对接在线支付，请先线下收款。"
                    okText="已收款"
                    cancelText="取消"
                    onConfirm={() => void confirmPaid(r)}
                  >
                    <Button size="small" type="primary">
                      确认收款
                    </Button>
                  </Popconfirm>
                )}
                {r.status === 'PENDING_SHIPMENT' && (
                  <Button
                    size="small"
                    type="primary"
                    onClick={() => {
                      setShipTarget(r);
                      shipForm.setFieldsValue({ shipCompany: companies[0] ?? '顺丰' });
                    }}
                  >
                    发货
                  </Button>
                )}
                {['PENDING_RECEIPT', 'FINISHED'].includes(r.status) && (
                  <Button
                    size="small"
                    onClick={() => {
                      setShipTarget(r);
                      shipForm.setFieldsValue({
                        shipCompany: r.shipCompany,
                        shipNo: r.shipNo,
                      });
                    }}
                  >
                    改运单
                  </Button>
                )}
              </Space>
            ),
          },
        ]}
      />

      <Drawer size={560} open={detailOpen} title="订单详情" onClose={() => setDetailOpen(false)}>
        {!detail ? (
          <Card loading />
        ) : (
          <>
            <Descriptions column={1} size="small" bordered>
              <Descriptions.Item label="订单号">{detail.summary.orderNo}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <StatusTag tone={orderStatusTone(detail.summary.status)}>{detail.summary.statusLabel}</StatusTag>
              </Descriptions.Item>
              <Descriptions.Item label="收货信息">
                {detail.receiverName} {detail.receiverMobile}
                <div className="mall-card__meta">{detail.receiverAddress}</div>
              </Descriptions.Item>
              {detail.buyerRemark && (
                <Descriptions.Item label="买家留言">{detail.buyerRemark}</Descriptions.Item>
              )}
              <Descriptions.Item label="金额">
                商品 {money(detail.summary.goodsAmount)} + 运费 {money(detail.summary.freightAmount)} - 优惠{' '}
                {money(detail.summary.discountAmount)} = <b>{money(detail.summary.payAmount)}</b>
              </Descriptions.Item>
              {detail.payNo && (
                <Descriptions.Item label="支付">
                  {detail.payChannel} · {detail.payNo}
                </Descriptions.Item>
              )}
              {detail.summary.shipCompany && (
                <Descriptions.Item label="物流">
                  {detail.summary.shipCompany} {detail.summary.shipNo}
                </Descriptions.Item>
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

      <Modal
        open={Boolean(shipTarget)}
        title={shipTarget?.status === 'PENDING_SHIPMENT' ? '订单发货' : '修改运单信息'}
        onCancel={() => setShipTarget(null)}
        onOk={() => void submitShip()}
        okText="确认"
      >
        <Form form={shipForm} layout="vertical">
          <Form.Item
            name="shipCompany"
            label="快递公司"
            rules={[{ required: true, message: '请选择或填写快递公司' }]}
          >
            <Select
              showSearch
              options={(companies.length ? companies : ['顺丰', '中通', '圆通', '韵达', '京东物流']).map(
                (c) => ({ value: c, label: c }),
              )}
            />
          </Form.Item>
          <Form.Item
            name="shipNo"
            label="运单号"
            rules={[{ required: true, message: '请输入运单号' }]}
          >
            <Input maxLength={40} placeholder="例如 SF1234567890" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
