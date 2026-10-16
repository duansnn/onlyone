import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Col,
  Form,
  Input,
  Modal,
  Popconfirm,
  Row,
  Space,
  Statistic,
  Table,
  Tag,
} from 'antd';
import { platformApi } from '@/api/admin';
import { toast } from '@/utils/feedback';
import { money } from '@/utils/format';
import type { PlatformOverview, PlatformShop } from '@/types/mall';

export default function PlatformShops() {
  const [overview, setOverview] = useState<PlatformOverview | null>(null);
  const [rows, setRows] = useState<PlatformShop[]>([]);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [o, page] = await Promise.all([
        platformApi.overview(),
        platformApi.shops({ keyword: keyword || undefined, pageNum, pageSize: 10 }),
      ]);
      setOverview(o);
      setRows(page.rows ?? []);
      setTotal(page.total ?? 0);
    } catch {
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [keyword, pageNum]);

  useEffect(() => {
    void load();
  }, [load]);

  const submit = async () => {
    const values = await form.validateFields();
    try {
      await platformApi.createShop({
        name: values.name.trim(),
        ownerMobile: values.ownerMobile?.trim() || undefined,
        ownerPassword: values.ownerPassword || undefined,
        ownerNickname: values.ownerNickname?.trim() || undefined,
        contactName: values.contactName?.trim() || undefined,
        contactMobile: values.contactMobile?.trim() || undefined,
        shipAddress: values.shipAddress?.trim() || undefined,
        intro: values.intro?.trim() || undefined,
      });
      toast.success('店铺已创建，店主已绑定');
      setOpen(false);
      form.resetFields();
      await load();
    } catch {
      /* 拦截器已提示 */
    }
  };

  return (
    <div>
      <h2 className="mall-section-title">平台 · 店铺管理</h2>

      <Row gutter={[16, 16]} style={{ marginBottom: 20 }}>
        <Col xs={12} md={8} xl={4}>
          <Card>
            <Statistic title="店铺总数" value={overview?.shopCount ?? 0} />
          </Card>
        </Col>
        <Col xs={12} md={8} xl={4}>
          <Card>
            <Statistic title="营业中" value={overview?.activeShopCount ?? 0} />
          </Card>
        </Col>
        <Col xs={12} md={8} xl={4}>
          <Card>
            <Statistic title="全平台待发货" value={overview?.pendingShipmentCount ?? 0} />
          </Card>
        </Col>
        <Col xs={12} md={8} xl={4}>
          <Card>
            <Statistic title="全平台待处理售后" value={overview?.pendingRefundCount ?? 0} />
          </Card>
        </Col>
        <Col xs={12} md={8} xl={4}>
          <Card>
            <Statistic title="今日订单" value={overview?.todayOrderCount ?? 0} />
          </Card>
        </Col>
        <Col xs={12} md={8} xl={4}>
          <Card>
            <Statistic title="今日收款" value={money(overview?.todayPaidAmount ?? 0)} />
          </Card>
        </Col>
      </Row>

      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input.Search
            allowClear
            placeholder="店名"
            style={{ width: 220 }}
            onSearch={(value) => {
              setKeyword(value);
              setPageNum(1);
            }}
          />
          <Button type="primary" onClick={() => setOpen(true)}>
            开通新店铺
          </Button>
        </Space>
      </Card>

      <Table
        rowKey="shopId"
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
          { title: '店名', dataIndex: 'name' },
          { title: '联系人', dataIndex: 'contactName' },
          { title: '联系电话', dataIndex: 'contactMobileMasked', width: 140 },
          {
            title: '在售商品',
            dataIndex: 'onSaleProductCount',
            width: 100,
          },
          {
            title: '待发货',
            dataIndex: 'pendingShipmentCount',
            width: 90,
          },
          {
            title: '今日收款',
            width: 120,
            render: (_, r) => <span className="mall-price">{money(r.todayPaidAmount)}</span>,
          },
          {
            title: '状态',
            width: 100,
            render: (_, r) => (
              <Tag color={r.status === 0 ? 'green' : 'red'}>{r.statusLabel}</Tag>
            ),
          },
          {
            title: '操作',
            width: 130,
            render: (_, r) => (
              <Popconfirm
                title={r.status === 0 ? '停用后该店店面不可访问，确认？' : '恢复营业？'}
                onConfirm={async () => {
                  await platformApi.setShopStatus(String(r.shopId), r.status === 0 ? 1 : 0);
                  toast.success(r.status === 0 ? '店铺已停用' : '店铺已恢复营业');
                  await load();
                }}
              >
                <Button size="small" danger={r.status === 0}>
                  {r.status === 0 ? '停用' : '启用'}
                </Button>
              </Popconfirm>
            ),
          },
        ]}
      />

      <Modal
        open={open}
        title="开通新店铺"
        onCancel={() => setOpen(false)}
        onOk={() => void submit()}
        okText="创建"
        width={560}
      >
        <Alert
          style={{ marginBottom: 16 }}
          type="info"
          showIcon
          message="店主不是第三种账号"
          description="系统只有用户和店铺两种长期实体。填手机号后会绑定已有用户；用户不存在则用你填的密码新建一个，并把它设为该店店主。"
        />
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="店铺名称" rules={[{ required: true, message: '请输入店名' }]}>
            <Input maxLength={40} />
          </Form.Item>
          <Form.Item
            name="ownerMobile"
            label="店主手机号"
            rules={[
              { required: true, message: '请输入店主手机号' },
              { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' },
            ]}
          >
            <Input maxLength={11} />
          </Form.Item>
          <Space size={12} style={{ display: 'flex' }}>
            <Form.Item name="ownerNickname" label="店主昵称">
              <Input maxLength={20} style={{ width: 200 }} />
            </Form.Item>
            <Form.Item
              name="ownerPassword"
              label="初始密码"
              rules={[{ min: 6, message: '至少 6 位' }]}
              extra="用户已存在时忽略"
            >
              <Input.Password style={{ width: 200 }} />
            </Form.Item>
          </Space>
          <Space size={12} style={{ display: 'flex' }}>
            <Form.Item name="contactName" label="联系人">
              <Input maxLength={20} style={{ width: 200 }} />
            </Form.Item>
            <Form.Item name="contactMobile" label="联系电话">
              <Input maxLength={11} style={{ width: 200 }} />
            </Form.Item>
          </Space>
          <Form.Item name="shipAddress" label="发货 / 退货地址">
            <Input.TextArea rows={2} maxLength={200} />
          </Form.Item>
          <Form.Item name="intro" label="店铺简介">
            <Input.TextArea rows={2} maxLength={200} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
