import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
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
import { DownloadOutlined, WarningOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { stockApi } from '@/api/admin';
import EmptyState from '@/components/EmptyState';
import { useAuthStore } from '@/store/auth';
import { toast } from '@/utils/feedback';
import { dt } from '@/utils/format';
import type { SkuStock, StockLog, StockTake } from '@/types/mall';

const REASONS = [
  { value: 'MANUAL', label: '手工盘点' },
  { value: 'INIT', label: '初始化' },
  { value: 'RETURN', label: '退货回补' },
];

export default function Stock() {
  const shopId = useAuthStore((state) => state.currentShopId) ?? '';
  const [tab, setTab] = useState('skus');
  const [skus, setSkus] = useState<SkuStock[]>([]);
  const [skuTotal, setSkuTotal] = useState(0);
  const [skuPage, setSkuPage] = useState(1);
  const [skuKeyword, setSkuKeyword] = useState('');
  const [skuLoading, setSkuLoading] = useState(true);

  const [logs, setLogs] = useState<StockLog[]>([]);
  const [logTotal, setLogTotal] = useState(0);
  const [logPage, setLogPage] = useState(1);
  const [logSkuId, setLogSkuId] = useState<string | undefined>(undefined);
  const [logLabel, setLogLabel] = useState<string | undefined>(undefined);
  const [logLoading, setLogLoading] = useState(false);

  // 库存预警
  const [warnings, setWarnings] = useState<SkuStock[]>([]);
  const [warningLoading, setWarningLoading] = useState(false);
  const [safeTarget, setSafeTarget] = useState<SkuStock | null>(null);
  const [safeForm] = Form.useForm();

  // 盘点
  const [takes, setTakes] = useState<StockTake[]>([]);
  const [takeLoading, setTakeLoading] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [createForm] = Form.useForm();
  // 正在盘点（草稿）的单 + 逐 SKU 实盘数
  const [taking, setTaking] = useState<StockTake | null>(null);
  const [takeInput, setTakeInput] = useState<Record<string, number | undefined>>({});

  const [target, setTarget] = useState<SkuStock | null>(null);
  const [form] = Form.useForm();

  const [exporting, setExporting] = useState(false);

  const downloadBlob = async (blob: Blob, prefix: string) => {
    if (blob.type.includes('json') || blob.type.startsWith('text/')) {
      const text = await blob.text();
      let message = '导出失败';
      try {
        const body = JSON.parse(text) as { msg?: string };
        if (body.msg) message = body.msg;
      } catch {
        /* 不是 JSON 时用默认文案 */
      }
      toast.error(message);
      return;
    }
    const href = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = href;
    link.download = `${prefix}-${dayjs().format('YYYYMMDD-HHmm')}.xlsx`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(href);
    toast.success('已开始下载');
  };

  const exportSkus = async () => {
    if (!shopId || exporting) return;
    setExporting(true);
    try {
      const blob = await stockApi.exportSkus(shopId, { keyword: skuKeyword || undefined });
      await downloadBlob(blob, '库存');
    } catch {
      /* 网络错误由拦截器提示 */
    } finally {
      setExporting(false);
    }
  };

  const exportLogs = async () => {
    if (!shopId || exporting) return;
    setExporting(true);
    try {
      const blob = await stockApi.exportLogs(shopId, { skuId: logSkuId });
      await downloadBlob(blob, '库存流水');
    } catch {
      /* 网络错误由拦截器提示 */
    } finally {
      setExporting(false);
    }
  };

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

  const loadWarnings = useCallback(async () => {
    if (!shopId) return;
    setWarningLoading(true);
    try {
      setWarnings(await stockApi.warnings(shopId));
    } catch {
      setWarnings([]);
    } finally {
      setWarningLoading(false);
    }
  }, [shopId]);

  const loadTakes = useCallback(async () => {
    if (!shopId) return;
    setTakeLoading(true);
    try {
      setTakes(await stockApi.listTakes(shopId));
    } catch {
      setTakes([]);
    } finally {
      setTakeLoading(false);
    }
  }, [shopId]);

  useEffect(() => {
    void loadSkus();
  }, [loadSkus]);

  useEffect(() => {
    void loadLogs();
  }, [loadLogs]);

  useEffect(() => {
    if (tab === 'warnings') void loadWarnings();
  }, [tab, loadWarnings]);

  useEffect(() => {
    if (tab === 'takes') void loadTakes();
  }, [tab, loadTakes]);

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

  const submitSafe = async () => {
    if (!safeTarget) return;
    const values = await safeForm.validateFields();
    try {
      await stockApi.safeStock(shopId, {
        skuId: String(safeTarget.skuId),
        safeStock: values.safeStock ?? null,
      });
      toast.success('安全阈值已保存');
      setSafeTarget(null);
      safeForm.resetFields();
      await loadSkus();
      await loadWarnings();
    } catch {
      /* 拦截器已提示 */
    }
  };

  const openCreate = () => {
    createForm.resetFields();
    setCreateOpen(true);
  };

  const doCreate = async () => {
    const values = await createForm.validateFields();
    try {
      const take = await stockApi.createTake(shopId, {
        title: values.title,
        remark: values.remark,
      });
      toast.success('盘点单已创建，开始登记实盘数');
      setCreateOpen(false);
      setTaking(take);
      setTakeInput({});
      setTab('takes');
    } catch {
      /* 拦截器已提示 */
    }
  };

  const doSubmitTake = async () => {
    if (!taking) return;
    const items = skus
      .filter((s) => takeInput[String(s.skuId)] !== undefined)
      .map((s) => ({ skuId: String(s.skuId), actualStock: takeInput[String(s.skuId)]! }));
    if (items.length === 0) {
      toast.error('请至少填写一个规格的实盘数');
      return;
    }
    try {
      await stockApi.submitTake(shopId, String(taking.id), { items });
      toast.success('盘点完成，差异已生成库存调整');
      setTaking(null);
      setTakeInput({});
      await loadTakes();
      await loadSkus();
      await loadLogs();
      await loadWarnings();
    } catch {
      /* 拦截器已提示 */
    }
  };

  return (
    <div>
      <h2 className="mall-section-title">库存管理</h2>

      <Tabs
        activeKey={tab}
        onChange={setTab}
        items={[
          {
            key: 'skus',
            label: '规格库存',
            children: (
              <>
                <Space style={{ marginBottom: 16 }} wrap>
                  <Input.Search
                    allowClear
                    placeholder="规格名 / 货号"
                    style={{ width: 240 }}
                    onSearch={(value) => {
                      setSkuKeyword(value);
                      setSkuPage(1);
                    }}
                  />
                  <Button icon={<DownloadOutlined />} loading={exporting} onClick={() => void exportSkus()}>
                    导出库存
                  </Button>
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
                      render: (_, r) => (
                        <span>
                          <b style={r.safeStock != null && r.availableStock <= r.safeStock ? { color: '#d64545' } : undefined}>
                            {r.availableStock}
                          </b>
                          {r.safeStock != null && r.availableStock <= r.safeStock && (
                            <Tag color="error" style={{ marginLeft: 6 }} bordered={false}>
                              低库存
                            </Tag>
                          )}
                        </span>
                      ),
                    },
                    {
                      title: '安全阈值',
                      width: 110,
                      render: (_, r) => (r.safeStock != null ? `${r.safeStock}` : <span className="mall-card__meta">未设置</span>),
                    },
                    {
                      title: '状态',
                      width: 90,
                      render: (_, r) =>
                        r.status === 0 ? <Tag color="green">启用</Tag> : <Tag>停用</Tag>,
                    },
                    {
                      title: '操作',
                      width: 240,
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
                              setSafeTarget(r);
                              safeForm.setFieldsValue({ safeStock: r.safeStock ?? undefined });
                            }}
                          >
                            设阈值
                          </Button>
                          <Button
                            size="small"
                            onClick={() => {
                              setLogSkuId(String(r.skuId));
                              setLogLabel(`${r.productName} · ${r.specName}`);
                              setLogPage(1);
                              setTab('logs');
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
            key: 'warnings',
            label: (
              <span>
                库存预警{warnings.length > 0 ? ` (${warnings.length})` : ''}
              </span>
            ),
            children: (
              <>
                {warnings.length > 0 && (
                  <Alert
                    type="warning"
                    showIcon
                    icon={<WarningOutlined />}
                    style={{ marginBottom: 16 }}
                    message={`有 ${warnings.length} 个规格可售库存达到或低于安全阈值，建议尽快补货。`}
                  />
                )}
                {warningLoading ? null : warnings.length === 0 ? (
                  <Card>
                    <EmptyState
                      title="没有库存预警"
                      description="给规格设置安全阈值后，可售库存一旦降到阈值以下，就会在这里提醒你。"
                    />
                  </Card>
                ) : (
                  <Table
                    rowKey="skuId"
                    scroll={{ x: 'max-content' }}
                    loading={warningLoading}
                    dataSource={warnings}
                    pagination={false}
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
                      { title: '总库存', dataIndex: 'stock', width: 100 },
                      { title: '锁定中', dataIndex: 'lockedStock', width: 100 },
                      {
                        title: '可售',
                        width: 100,
                        render: (_, r) => <b style={{ color: '#d64545' }}>{r.availableStock}</b>,
                      },
                      { title: '安全阈值', dataIndex: 'safeStock', width: 100 },
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
                              补库存
                            </Button>
                            <Button
                              size="small"
                              onClick={() => {
                                setSafeTarget(r);
                                safeForm.setFieldsValue({ safeStock: r.safeStock ?? undefined });
                              }}
                            >
                              改阈值
                            </Button>
                          </Space>
                        ),
                      },
                    ]}
                  />
                )}
              </>
            ),
          },
          {
            key: 'takes',
            label: '库存盘点',
            children: (
              <>
                <Space style={{ marginBottom: 16 }}>
                  <Button type="primary" onClick={openCreate}>
                    新建盘点单
                  </Button>
                  <span className="mall-card__meta">盘点会按「实盘数 - 账面数」生成差异调整，并写进库存流水</span>
                </Space>

                {takeLoading ? null : takes.length === 0 ? (
                  <Card>
                    <EmptyState
                      title="还没有盘点单"
                      description="定期盘点能发现账面与实物的差异，点上方「新建盘点单」开始。"
                    />
                  </Card>
                ) : (
                  takes.map((take) => (
                    <Card
                      key={take.id}
                      style={{ marginBottom: 14 }}
                      title={
                        <Space>
                          <span>{take.title}</span>
                          <Tag color={take.status === 'DONE' ? 'green' : 'default'}>
                            {take.statusLabel}
                          </Tag>
                        </Space>
                      }
                      extra={<span className="mall-card__meta">{dt(take.createTime)}</span>}
                    >
                      {take.remark && <div className="mall-card__meta" style={{ marginBottom: 12 }}>{take.remark}</div>}
                      {take.items.length === 0 ? (
                        <div className="mall-card__meta">尚未登记实盘数</div>
                      ) : (
                        <Table
                          rowKey="skuId"
                          size="small"
                          scroll={{ x: 'max-content' }}
                          pagination={false}
                          dataSource={take.items}
                          columns={[
                            { title: '商品', dataIndex: 'productName' },
                            { title: '规格', dataIndex: 'specName' },
                            { title: '账面', dataIndex: 'bookStock', width: 80 },
                            { title: '实盘', dataIndex: 'actualStock', width: 80 },
                            {
                              title: '差异',
                              width: 90,
                              render: (_, r) => (
                                <span className={r.diffQty !== 0 ? 'mall-price' : ''}>
                                  {r.diffQty > 0 ? `+${r.diffQty}` : r.diffQty}
                                </span>
                              ),
                            },
                          ]}
                        />
                      )}
                    </Card>
                  ))
                )}
              </>
            ),
          },
          {
            key: 'logs',
            label: '库存流水',
            children: (
              <>
                <Space style={{ marginBottom: 16 }} wrap>
                  <span className="mall-card__meta">筛选规格</span>
                  <Select
                    allowClear
                    style={{ width: 260 }}
                    placeholder="全部规格"
                    value={logSkuId}
                    onChange={(value) => {
                      setLogSkuId(value);
                      if (!value) setLogLabel(undefined);
                      setLogPage(1);
                    }}
                    options={[
                      ...(logSkuId && logLabel && !skus.some((s) => String(s.skuId) === logSkuId)
                        ? [{ value: logSkuId, label: logLabel }]
                        : []),
                      ...skus.map((s) => ({
                        value: String(s.skuId),
                        label: `${s.productName} · ${s.specName}`,
                      })),
                    ]}
                  />
                  {logSkuId && (
                    <Button
                      size="small"
                      onClick={() => {
                        setLogSkuId(undefined);
                        setLogLabel(undefined);
                        setLogPage(1);
                      }}
                    >
                      清除筛选
                    </Button>
                  )}
                  <Button icon={<DownloadOutlined />} loading={exporting} onClick={() => void exportLogs()}>
                    导出流水
                  </Button>
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
                          <div>{r.productName || '已删除的商品'}</div>
                          <div className="mall-card__meta">{r.specName || '规格已删除'}</div>
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

      {/* 调整库存 */}
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

      {/* 设置安全阈值 */}
      <Modal
        open={Boolean(safeTarget)}
        title="设置安全库存阈值"
        onCancel={() => setSafeTarget(null)}
        onOk={() => void submitSafe()}
        okText="保存"
      >
        <Card size="small" style={{ marginBottom: 16 }}>
          <div>{safeTarget?.productName}</div>
          <div className="mall-card__meta">
            {safeTarget?.specName} · 当前可售 {safeTarget?.availableStock}
          </div>
        </Card>
        <Form form={safeForm} layout="vertical">
          <Form.Item
            name="safeStock"
            label="安全阈值"
            extra="可售库存降到这个数（含）以下时触发预警；留空表示不预警"
          >
            <InputNumber min={0} style={{ width: '100%' }} placeholder="留空表示不预警" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 新建盘点单 */}
      <Modal
        open={createOpen}
        title="新建盘点单"
        onCancel={() => setCreateOpen(false)}
        onOk={() => void doCreate()}
        okText="创建"
      >
        <Form form={createForm} layout="vertical">
          <Form.Item name="title" label="盘点单标题" rules={[{ required: true, message: '请输入标题' }]}>
            <Input maxLength={64} placeholder="例如：11 月底盘点" />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input maxLength={255} placeholder="选填" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 登记盘点（提交实盘数） */}
      <Modal
        open={Boolean(taking)}
        title={taking?.title ?? '登记盘点'}
        onCancel={() => {
          setTaking(null);
          setTakeInput({});
        }}
        onOk={() => void doSubmitTake()}
        okText="提交盘点"
        width={720}
      >
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message="对每个要盘点的规格填「实盘库存」，留空的规格不会参与本次盘点；提交后按「实盘 - 账面」自动生成差异调整。"
        />
        <Table
          rowKey="skuId"
          size="small"
          scroll={{ y: 360 }}
          pagination={false}
          dataSource={skus}
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
              title: '账面库存',
              dataIndex: 'stock',
              width: 90,
            },
            {
              title: '实盘库存',
              width: 140,
              render: (_, r) => (
                <InputNumber
                  min={0}
                  style={{ width: 120 }}
                  placeholder="留空跳过"
                  value={takeInput[String(r.skuId)]}
                  onChange={(v) =>
                    setTakeInput((prev) => ({ ...prev, [String(r.skuId)]: v ?? undefined }))
                  }
                />
              ),
            },
          ]}
        />
      </Modal>
    </div>
  );
}
