import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
} from 'antd';
import { stockApi } from '@/api/admin';
import { useAuthStore } from '@/store/auth';
import { toast } from '@/utils/feedback';
import { dt } from '@/utils/format';
import type { SkuStock, StockLog } from '@/types/mall';

const REASONS = [
  { value: 'MANUAL', label: '手工盘点' },
  { value: 'INIT', label: '初始化' },
  { value: 'RETURN', label: '退货回补' },
];

export default function Stock() {
  const shopId = useAuthStore((state) => state.currentShopId) ?? '';
  const [skus, setSkus] = useState<SkuStock[]>([]);
  const [skuTotal, setSkuTotal] = useState(0);
  const [skuPage, setSkuPage] = useState(1);
  const [skuKeyword, setSkuKeyword] = useState('');
  const [skuLoading, setSkuLoading] = useState(true);

  const [logs, setLogs] = useState<StockLog[]>([]);
  const [logTotal, setLogTotal] = useState(0);
  const [logPage, setLogPage] = useState(1);
  const [logSkuId, setLogSkuId] = useState<string | undefined>(undefined);
  const [logLoading, setLogLoading] = useState(false);

  const [target, setTarget] = useState<SkuStock | null>(null);
  const [form] = Form.useForm();

  const loadSkus = useCallback(async () => {
    if (!shopId) return;
    setSkuLoading(true);
    try {
      const page = await stockApi.skus(shopId, {
        keyword: skuKeyword || undefined,
        pageNum: skuPage,
        pageSize: 10,
      });
      setSkus(page.rows ?? []);
      setSkuTotal(page.total ?? 0);
    } catch {
      setSkus([]);
    } finally {
      setSkuLoading(false);
    }
  }, [shopId, skuKeyword, skuPage]);

  const loadLogs = useCallback(async () => {
    if (!shopId) return;
    setLogLoading(true);
    try {
      const page = await stockApi.logs(shopId, { skuId: logSkuId, pageNum: logPage, pageSize: 10 });
      setLogs(page.rows ?? []);
      setLogTotal(page.total ?? 0);
    } catch {
      setLogs([]);
    } finally {
      setLogLoading(false);
    }
  }, [shopId, logSkuId, logPage]);

  useEffect(() => {
    void loadSkus();
  }, [loadSkus]);

  useEffect(() => {
    void loadLogs();
  }, [loadLogs]);

  const submit = async () => {
    if (!target) return;
    const values = await form.validateFields();
    try {
      await stockApi.adjust(shopId, {
        skuId: String(target.skuId),
        stock: values.stock,
        reason: values.reason,
        remark: values.remark,
      });
      toast.success('库存已更新');
      setTarget(null);
      form.resetFields();
      await loadSkus();
      await loadLogs();
    } catch {
      /* 拦截器已提示 */
    }
  };

  return (
    <div>
      <h2 className="mall-section-title">库存管理</h2>

      <Tabs
        items={[
          {
            key: 'skus',
            label: '规格库存',
            children: (
              <>
                <Space style={{ marginBottom: 16 }}>
                  <Input.Search
                    allowClear
                    placeholder="规格名 / 货号"
                    style={{ width: 240 }}
                    onSearch={(value) => {
                      setSkuKeyword(value);
                      setSkuPage(1);
                    }}
                  />
                </Space>
                <Table
                  rowKey="skuId"
                  scroll={{ x: 'max-content' }}
                  loading={skuLoading}
                  dataSource={skus}
                  pagination={{
                    current: skuPage,
                    pageSize: 10,
                    total: skuTotal,
                    showSizeChanger: false,
                    onChange: setSkuPage,
                  }}
                  columns={[
                    {
                      title: '商品 / 规格',
                      render: (_, r) => (
                        <div>
                          <div>{r.productName}</div>
                          <div className="mall-card__meta">
                            {r.specName}
                            {r.skuCode ? ` · ${r.skuCode}` : ''}
                          </div>
                        </div>
                      ),
                    },
                    { title: '总库存', dataIndex: 'stock', width: 100 },
                    { title: '锁定中', dataIndex: 'lockedStock', width: 100 },
                    {
                      title: '可售',
                      width: 100,
                      render: (_, r) => <b>{r.availableStock}</b>,
                    },
                    {
                      title: '状态',
                      width: 90,
                      render: (_, r) =>
                        r.status === 0 ? <Tag color="green">启用</Tag> : <Tag>停用</Tag>,
                    },
                    {
                      title: '操作',
                      width: 160,
                      render: (_, r) => (
                        <Space>
                          <Button
                            size="small"
                            type="primary"
                            onClick={() => {
                              setTarget(r);
                              form.setFieldsValue({ stock: r.stock, reason: 'MANUAL' });
                            }}
                          >
                            调整库存
                          </Button>
                          <Button
                            size="small"
                            onClick={() => {
                              setLogSkuId(String(r.skuId));
                              setLogPage(1);
                            }}
                          >
                            流水
                          </Button>
                        </Space>
                      ),
                    },
                  ]}
                />
              </>
            ),
          },
          {
            key: 'logs',
            label: '库存流水',
            children: (
              <>
                <Space style={{ marginBottom: 16 }}>
                  <span className="mall-card__meta">筛选规格</span>
                  <Select
                    allowClear
                    style={{ width: 260 }}
                    placeholder="全部规格"
                    value={logSkuId}
                    onChange={(value) => {
                      setLogSkuId(value);
                      setLogPage(1);
                    }}
                    options={skus.map((s) => ({
                      value: String(s.skuId),
                      label: `${s.productName} · ${s.specName}`,
                    }))}
                  />
                </Space>
                <Table
                  rowKey="id"
                  scroll={{ x: 'max-content' }}
                  loading={logLoading}
                  dataSource={logs}
                  pagination={{
                    current: logPage,
                    pageSize: 10,
                    total: logTotal,
                    showSizeChanger: false,
                    onChange: setLogPage,
                  }}
                  columns={[
                    {
                      title: '商品 / 规格',
                      render: (_, r) => (
                        <div>
                          <div>{r.productName}</div>
                          <div className="mall-card__meta">{r.specName}</div>
                        </div>
                      ),
                    },
                    {
                      title: '变动',
                      width: 90,
                      render: (_, r) => (
                        <span className={r.changeQty >= 0 ? 'mall-price' : ''}>
                          {r.changeQty >= 0 ? `+${r.changeQty}` : r.changeQty}
                        </span>
                      ),
                    },
                    { title: '变动后总库存', dataIndex: 'afterStock', width: 130 },
                    { title: '变动后锁定', dataIndex: 'afterLocked', width: 130 },
                    { title: '原因', width: 110, render: (_, r) => r.reasonLabel },
                    {
                      title: '备注',
                      render: (_, r) =>
                        r.remark ||
                        (r.orderId ? `关联订单 ${r.orderId}` : <span className="mall-card__meta">-</span>),
                    },
                    { title: '时间', width: 150, render: (_, r) => dt(r.createTime) },
                  ]}
                />
              </>
            ),
          },
        ]}
      />

      <Modal
        open={Boolean(target)}
        title="调整库存"
        onCancel={() => setTarget(null)}
        onOk={() => void submit()}
        okText="确认调整"
      >
        <Card size="small" style={{ marginBottom: 16 }}>
          <div>{target?.productName}</div>
          <div className="mall-card__meta">
            {target?.specName} · 当前总库存 {target?.stock}，锁定 {target?.lockedStock}，可售{' '}
            {target?.availableStock}
          </div>
        </Card>
        <Form form={form} layout="vertical">
          <Form.Item
            name="stock"
            label="调整后总库存"
            rules={[{ required: true, message: '请输入库存' }]}
            extra="填的是调整后的绝对值，不是增减量"
          >
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="reason" label="原因" rules={[{ required: true }]}>
            <Select options={REASONS} />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input maxLength={100} placeholder="例如：盘点差异" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
