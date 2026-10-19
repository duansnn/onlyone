import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Card,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
} from 'antd';
import dayjs from 'dayjs';
import { adminCouponApi } from '@/api/admin';
import { useAuthStore } from '@/store/auth';
import { toast } from '@/utils/feedback';
import { dt, fenToYuanInput, money, yuanToFen } from '@/utils/format';
import type { Coupon } from '@/types/mall';

export default function Coupons() {
  const shopId = useAuthStore((state) => state.currentShopId) ?? '';
  const [status, setStatus] = useState('');
  const [rows, setRows] = useState<Coupon[]>([]);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [loading, setLoading] = useState(true);

  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Coupon | null>(null);
  const [grantTarget, setGrantTarget] = useState<Coupon | null>(null);
  const [form] = Form.useForm();
  const [grantForm] = Form.useForm();

  const load = useCallback(async () => {
    if (!shopId) return;
    setLoading(true);
    try {
      const page = await adminCouponApi.page(shopId, {
        status: status || undefined,
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
  }, [shopId, status, pageNum]);

  useEffect(() => {
    void load();
  }, [load]);

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ perUserLimit: 1, total: 100, status: '0' });
    setOpen(true);
  };

  const openEdit = (record: Coupon) => {
    setEditing(record);
    form.setFieldsValue({
      id: String(record.id),
      name: record.name,
      thresholdAmount: fenToYuanInput(record.thresholdAmount),
      reduceAmount: fenToYuanInput(record.reduceAmount),
      total: record.total,
      perUserLimit: record.perUserLimit,
      status: String(record.status),
      range:
        record.startTime && record.endTime
          ? [dayjs(record.startTime), dayjs(record.endTime)]
          : undefined,
    });
    setOpen(true);
  };

  const submit = async () => {
    const values = await form.validateFields();
    const range = values.range as [dayjs.Dayjs, dayjs.Dayjs] | undefined;
    try {
      await adminCouponApi.save(shopId, {
        id: editing ? String(editing.id) : undefined,
        name: values.name,
        thresholdAmount: yuanToFen(values.thresholdAmount),
        reduceAmount: yuanToFen(values.reduceAmount),
        total: values.total,
        perUserLimit: values.perUserLimit,
        status: values.status,
        startTime: range?.[0] ? range[0].format('YYYY-MM-DDTHH:mm:ss') : undefined,
        endTime: range?.[1] ? range[1].format('YYYY-MM-DDTHH:mm:ss') : undefined,
      });
      toast.success(editing ? '优惠券已更新' : '优惠券已创建');
      setOpen(false);
      await load();
    } catch {
      /* 拦截器已提示 */
    }
  };

  const submitGrant = async () => {
    if (!grantTarget) return;
    const values = await grantForm.validateFields();
    try {
      await adminCouponApi.grant(shopId, String(grantTarget.id), values.mobile.trim());
      toast.success('已定向发放');
      setGrantTarget(null);
      grantForm.resetFields();
      await load();
    } catch {
      /* 拦截器已提示 */
    }
  };

  return (
    <div>
      <h2 className="mall-section-title">优惠券</h2>

      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Select
            value={status}
            style={{ width: 140 }}
            onChange={(value) => {
              setStatus(value);
              setPageNum(1);
            }}
            options={[
              { value: '', label: '全部状态' },
              { value: '0', label: '启用' },
              { value: '1', label: '停用' },
            ]}
          />
          <Button type="primary" onClick={openCreate}>
            新建优惠券
          </Button>
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
          onChange: setPageNum,
        }}
        columns={[
          { title: '券名', dataIndex: 'name' },
          {
            title: '门槛 / 优惠',
            width: 180,
            render: (_, r) => `满 ${money(r.thresholdAmount)} 减 ${money(r.reduceAmount)}`,
          },
          {
            title: '已领 / 总量',
            width: 120,
            render: (_, r) => `${r.received} / ${r.total}`,
          },
          { title: '每人限领', dataIndex: 'perUserLimit', width: 100 },
          {
            title: '有效期',
            width: 200,
            render: (_, r) => (
              <span className="mall-card__meta">
                {dt(r.startTime)} ~ {dt(r.endTime)}
              </span>
            ),
          },
          {
            title: '状态',
            width: 100,
            render: (_, r) => (
              <Tag color={r.status === 0 ? 'green' : 'default'}>
                {r.expired ? '已过期' : r.statusLabel}
              </Tag>
            ),
          },
          {
            title: '操作',
            width: 230,
            render: (_, r) => (
              <Space>
                <Button size="small" onClick={() => openEdit(r)}>
                  编辑
                </Button>
                <Popconfirm
                  title={r.status === 0 ? '停用后顾客将无法领取' : '启用后顾客可领取'}
                  onConfirm={async () => {
                    await adminCouponApi.setStatus(shopId, String(r.id), r.status === 0 ? 1 : 0);
                    toast.success(r.status === 0 ? '已停用' : '已启用');
                    await load();
                  }}
                >
                  <Button size="small">{r.status === 0 ? '停用' : '启用'}</Button>
                </Popconfirm>
                <Button size="small" type="primary" onClick={() => setGrantTarget(r)}>
                  定向发放
                </Button>
              </Space>
            ),
          },
        ]}
      />

      {/* 新建 / 编辑 */}
      <Modal
        open={open}
        title={editing ? '编辑优惠券' : '新建优惠券'}
        onCancel={() => setOpen(false)}
        onOk={() => void submit()}
        okText="保存"
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="券名" rules={[{ required: true, message: '请输入券名' }]}>
            <Input maxLength={40} placeholder="例如：满 30 减 5" />
          </Form.Item>
          <Space size={12} style={{ display: 'flex' }}>
            <Form.Item
              name="thresholdAmount"
              label="使用门槛（元）"
              rules={[{ required: true, message: '必填' }]}
            >
              <InputNumber min={0} precision={2} style={{ width: 150 }} />
            </Form.Item>
            <Form.Item
              name="reduceAmount"
              label="优惠金额（元）"
              rules={[{ required: true, message: '必填' }]}
            >
              <InputNumber min={0.01} precision={2} style={{ width: 150 }} />
            </Form.Item>
          </Space>
          <Space size={12} style={{ display: 'flex' }}>
            <Form.Item name="total" label="发放总量" rules={[{ required: true, message: '必填' }]}>
              <InputNumber min={1} style={{ width: 150 }} />
            </Form.Item>
            <Form.Item
              name="perUserLimit"
              label="每人限领"
              rules={[{ required: true, message: '必填' }]}
            >
              <InputNumber min={1} style={{ width: 150 }} />
            </Form.Item>
            <Form.Item name="status" label="状态">
              <Select
                style={{ width: 120 }}
                options={[
                  { value: '0', label: '启用' },
                  { value: '1', label: '停用' },
                ]}
              />
            </Form.Item>
          </Space>
          <Form.Item name="range" label="有效期（选填）">
            <DatePicker.RangePicker showTime style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      {/* 定向发放 */}
      <Modal
        open={Boolean(grantTarget)}
        title={`定向发放 · ${grantTarget?.name ?? ''}`}
        onCancel={() => setGrantTarget(null)}
        onOk={() => void submitGrant()}
        okText="发放"
      >
        <Form form={grantForm} layout="vertical">
          <Form.Item
            name="mobile"
            label="顾客手机号"
            rules={[
              { required: true, message: '请输入手机号' },
              { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' },
            ]}
            extra="顾客必须是已注册用户，否则会提示查不到"
          >
            <Input maxLength={11} placeholder="11 位手机号" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
