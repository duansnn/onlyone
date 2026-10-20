import { useCallback, useEffect, useState } from 'react';
import {
  Avatar,
  Button,
  Card,
  Descriptions,
  Drawer,
  Empty,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Skeleton,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd';
import { customerApi } from '@/api/admin';
import StatusTag from '@/components/StatusTag';
import { useAuthStore } from '@/store/auth';
import { toast } from '@/utils/feedback';
import { dt, money, orderStatusTone } from '@/utils/format';
import type { CustomerDetail, PlatformUser } from '@/types/mall';

const PAGE_SIZE = 10;

/**
 * 顾客管理。
 *
 * <p>单商家模式：顾客自己在店面注册，店主在这里看「谁买过、买了多少、地址在哪」，
 * 并能停用账号、重置密码。这里不新建账号，也不改顾客资料。</p>
 */
export default function Customers() {
  const me = useAuthStore((state) => state.user);
  const [rows, setRows] = useState<PlatformUser[]>([]);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState<number | undefined>(undefined);
  const [loading, setLoading] = useState(true);

  const [detail, setDetail] = useState<CustomerDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [target, setTarget] = useState<PlatformUser | null>(null);
  const [form] = Form.useForm();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const page = await customerApi.page({ keyword: keyword || undefined, status, pageNum, pageSize: PAGE_SIZE });
      setRows(page.rows ?? []);
      setTotal(page.total ?? 0);
    } catch {
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [keyword, status, pageNum]);

  useEffect(() => {
    void load();
  }, [load]);

  const openDetail = async (row: PlatformUser) => {
    setDetailLoading(true);
    setDetail({ profile: row, addresses: [], orders: [] });
    try {
      setDetail(await customerApi.detail(String(row.id)));
    } catch {
      /* 拦截器已提示，保留基础信息 */
    } finally {
      setDetailLoading(false);
    }
  };

  const submitReset = async () => {
    if (!target) return;
    const values = await form.validateFields();
    try {
      await customerApi.resetPassword(String(target.id), values.password);
      toast.success('密码已重置，请通过其他渠道告知顾客');
      setTarget(null);
      form.resetFields();
    } catch {
      /* 拦截器已提示 */
    }
  };

  const toggleStatus = async (row: PlatformUser) => {
    try {
      await customerApi.setStatus(String(row.id), row.status === 0 ? 1 : 0);
      toast.success(row.status === 0 ? '账号已停用' : '账号已恢复');
      await load();
    } catch {
      /* 拦截器已提示 */
    }
  };

  const profile = detail?.profile;

  return (
    <div>
      <div className="admin-page-head">
        <div>
          <h2 className="mall-section-title" style={{ marginBottom: 4 }}>
            顾客管理
          </h2>
          <p className="admin-page-head__desc">
            顾客在店面自行注册，这里只做查看与管理，共 <b>{total}</b> 位顾客。
          </p>
        </div>
      </div>

      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input.Search
            allowClear
            placeholder="昵称 / 账号 / 手机号"
            style={{ width: 260 }}
            onSearch={(value) => {
              setKeyword(value);
              setPageNum(1);
            }}
          />
          <Select
            allowClear
            placeholder="全部状态"
            style={{ width: 140 }}
            value={status}
            onChange={(value) => {
              setStatus(value);
              setPageNum(1);
            }}
            options={[
              { value: 0, label: '正常' },
              { value: 1, label: '已停用' },
            ]}
          />
        </Space>
      </Card>

      <Table
        rowKey="id"
        scroll={{ x: 'max-content' }}
        loading={loading}
        dataSource={rows}
        pagination={{
          current: pageNum,
          pageSize: PAGE_SIZE,
          total,
          showSizeChanger: false,
          onChange: setPageNum,
        }}
        columns={[
          {
            title: '顾客',
            width: 220,
            render: (_, r) => (
              <div className="customer-cell">
                <Avatar size={34} className="admin-avatar">
                  {(r.nickname || r.username || '?').slice(0, 1)}
                </Avatar>
                <div className="customer-cell__text">
                  <div className="customer-cell__name">
                    {r.nickname || '未设昵称'}
                    {r.merchant && (
                      <Tag variant="filled" className="admin-tag">
                        店主
                      </Tag>
                    )}
                  </div>
                  <div className="customer-cell__account">{r.username}</div>
                </div>
              </div>
            ),
          },
          {
            title: '手机号',
            width: 150,
            render: (_, r) =>
              r.mobile ? (
                <Typography.Text copyable={{ text: r.mobile }}>{r.mobile}</Typography.Text>
              ) : (
                <span className="admin-muted">未填写</span>
              ),
          },
          {
            title: '订单数',
            width: 90,
            align: 'right',
            render: (_, r) => (r.orderCount > 0 ? `${r.orderCount} 单` : <span className="admin-muted">—</span>),
          },
          {
            title: '累计消费',
            width: 120,
            align: 'right',
            render: (_, r) =>
              r.paidAmount > 0 ? <b className="admin-amount">{money(r.paidAmount)}</b> : <span className="admin-muted">—</span>,
          },
          {
            title: '最后下单',
            width: 150,
            render: (_, r) =>
              r.lastOrderTime ? dt(r.lastOrderTime) : <span className="admin-muted">从未下单</span>,
          },
          { title: '注册时间', width: 150, render: (_, r) => dt(r.createTime) },
          {
            title: '状态',
            width: 90,
            render: (_, r) => (
              <StatusTag tone={r.status === 0 ? 'done' : 'closed'}>{r.statusLabel}</StatusTag>
            ),
          },
          {
            title: '操作',
            width: 230,
            fixed: 'right',
            render: (_, r) => {
              const isSelf = me && String(me.id) === String(r.id);
              return (
                <Space size={4}>
                  <Button size="small" type="link" onClick={() => void openDetail(r)}>
                    详情
                  </Button>
                  <Button
                    size="small"
                    type="link"
                    onClick={() => {
                      setTarget(r);
                      form.resetFields();
                    }}
                  >
                    重置密码
                  </Button>
                  <Popconfirm
                    title={
                      isSelf
                        ? '不能停用自己'
                        : r.status === 0
                          ? '停用后该账号将无法登录，确认？'
                          : '恢复该账号登录？'
                    }
                    disabled={Boolean(isSelf)}
                    onConfirm={() => void toggleStatus(r)}
                  >
                    <Button size="small" type="link" danger={r.status === 0} disabled={Boolean(isSelf)}>
                      {r.status === 0 ? '停用' : '启用'}
                    </Button>
                  </Popconfirm>
                </Space>
              );
            },
          },
        ]}
      />

      <Drawer
        open={Boolean(detail)}
        onClose={() => setDetail(null)}
        size={560}
        title="顾客详情"
        destroyOnHidden
      >
        {detailLoading ? (
          <Skeleton active paragraph={{ rows: 8 }} />
        ) : profile ? (
          <div className="customer-detail">
            <div className="customer-detail__head">
              <Avatar size={52} className="admin-avatar">
                {(profile.nickname || profile.username || '?').slice(0, 1)}
              </Avatar>
              <div className="customer-detail__id">
                <div className="customer-detail__name">
                  {profile.nickname || '未设昵称'}
                  <StatusTag tone={profile.status === 0 ? 'done' : 'closed'}>{profile.statusLabel}</StatusTag>
                </div>
                <div className="customer-detail__account">账号 {profile.username}</div>
              </div>
            </div>

            <div className="customer-stats">
              <div className="customer-stats__item">
                <span>订单数</span>
                <b>{profile.orderCount}</b>
              </div>
              <div className="customer-stats__item">
                <span>累计消费</span>
                <b>{money(profile.paidAmount)}</b>
              </div>
              <div className="customer-stats__item">
                <span>收货地址</span>
                <b>{profile.addressCount}</b>
              </div>
            </div>

            <Descriptions column={1} size="small" styles={{ label: { width: 92 } }}>
              <Descriptions.Item label="手机号">{profile.mobile || '未填写'}</Descriptions.Item>
              <Descriptions.Item label="邮箱">{profile.email || '未填写'}</Descriptions.Item>
              <Descriptions.Item label="注册时间">{dt(profile.createTime)}</Descriptions.Item>
              <Descriptions.Item label="最后下单">
                {profile.lastOrderTime ? dt(profile.lastOrderTime) : '从未下单'}
              </Descriptions.Item>
            </Descriptions>

            <h4 className="customer-detail__section">收货地址</h4>
            {detail?.addresses.length ? (
              <div className="customer-addresses">
                {detail.addresses.map((address) => (
                  <div key={address.id} className="customer-address">
                    <div className="customer-address__line">
                      <b>{address.receiver}</b>
                      <span>{address.mobile}</span>
                      {address.isDefault === 1 && (
                        <Tag variant="filled" className="admin-tag">
                          默认
                        </Tag>
                      )}
                    </div>
                    <div className="customer-address__full">{address.fullAddress}</div>
                  </div>
                ))}
              </div>
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="还没有填写收货地址" />
            )}

            <h4 className="customer-detail__section">最近订单</h4>
            {detail?.orders.length ? (
              <div className="customer-orders">
                {detail.orders.map((order) => (
                  <div key={order.id} className="customer-order">
                    <div className="customer-order__top">
                      <span className="customer-order__no">{order.orderNo}</span>
                      <StatusTag tone={orderStatusTone(order.status)}>{order.statusLabel}</StatusTag>
                    </div>
                    <div className="customer-order__body">
                      <span className="customer-order__summary">{order.productSummary}</span>
                      <b className="admin-amount">{money(order.payAmount)}</b>
                    </div>
                    <div className="customer-order__time">{dt(order.createTime)}</div>
                  </div>
                ))}
              </div>
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="还没有下过单" />
            )}
          </div>
        ) : null}
      </Drawer>

      <Modal
        open={Boolean(target)}
        title={`重置密码 · ${target?.nickname || target?.username || ''}`}
        onCancel={() => setTarget(null)}
        onOk={() => void submitReset()}
        okText="重置"
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="password"
            label="新密码"
            rules={[
              { required: true, message: '请输入新密码' },
              { min: 6, message: '至少 6 位' },
            ]}
            extra="重置后请通过安全渠道告知顾客，系统不会发送明文短信"
          >
            <Input.Password maxLength={64} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}