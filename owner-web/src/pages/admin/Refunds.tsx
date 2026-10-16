import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Form,
  Input,
  Modal,
  Pagination,
  Popconfirm,
  Select,
  Skeleton,
  Space,
  Tag,
} from 'antd';
import { adminRefundApi } from '@/api/admin';
import { useAuthStore } from '@/store/auth';
import { toast } from '@/utils/feedback';
import { dt, money, refundStatusColor } from '@/utils/format';
import type { Refund } from '@/types/mall';

const FILTERS = [
  { value: '', label: '全部' },
  { value: 'PENDING', label: '待处理' },
  { value: 'AGREED', label: '已同意' },
  { value: 'WAITING_RETURN', label: '待顾客寄回' },
  { value: 'RECEIVED', label: '已收货待退款' },
  { value: 'REJECTED', label: '已拒绝' },
  { value: 'SUCCESS', label: '已完成' },
];

export default function AdminRefunds() {
  const shopId = useAuthStore((state) => state.currentShopId) ?? '';
  const [status, setStatus] = useState('');
  const [rows, setRows] = useState<Refund[]>([]);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [loading, setLoading] = useState(true);
  const [refusing, setRefusing] = useState<Refund | null>(null);
  const [form] = Form.useForm();

  const load = useCallback(async () => {
    if (!shopId) return;
    setLoading(true);
    try {
      const page = await adminRefundApi.page(shopId, {
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

  const agree = async (refund: Refund) => {
    try {
      const result = await adminRefundApi.agree(shopId, String(refund.id));
      toast.success(
        refund.type === 'REFUND_ONLY'
          ? '已同意并原路退款'
          : `已同意，退货地址已给出：${result?.returnAddress ?? '店铺发货地址'}`,
      );
      await load();
    } catch {
      /* 拦截器已提示 */
    }
  };

  const confirmReceived = async (refund: Refund) => {
    try {
      await adminRefundApi.confirmReceived(shopId, String(refund.id));
      toast.success('已确认收货并完成退款');
      await load();
    } catch {
      /* 拦截器已提示 */
    }
  };

  const submitRefuse = async () => {
    if (!refusing) return;
    const values = await form.validateFields();
    try {
      await adminRefundApi.refuse(shopId, String(refusing.id), values.refuseReason);
      toast.success('已拒绝，订单已回到申请前的状态');
      setRefusing(null);
      form.resetFields();
      await load();
    } catch {
      /* 拦截器已提示 */
    }
  };

  return (
    <div>
      <h2 className="mall-section-title">退款 / 售后</h2>

      <Alert
        style={{ marginBottom: 16 }}
        type="info"
        showIcon
        message="售后按整单处理"
        description="未发货的订单只能「仅退款」，同意后原路退回并恢复库存；已发货的订单走「退货退款」，同意后系统给出退货地址，顾客寄回并填写运单号，你确认收货后再发起退款。拒绝必须填写理由，订单会回到申请前的状态。"
      />

      <Card style={{ marginBottom: 16 }}>
        <Select
          value={status}
          style={{ width: 180 }}
          options={FILTERS}
          onChange={(value) => {
            setStatus(value);
            setPageNum(1);
          }}
        />
      </Card>

      {loading ? (
        <Skeleton active />
      ) : rows.length === 0 ? (
        <Card>暂无售后申请</Card>
      ) : (
        rows.map((r) => (
          <Card
            key={r.id}
            style={{ marginBottom: 14 }}
            title={
              <Space size={12}>
                <span>{r.orderNo}</span>
                <Tag color={refundStatusColor(r.status)}>{r.statusLabel}</Tag>
                <Tag>{r.typeLabel}</Tag>
              </Space>
            }
            extra={<span className="mall-card__meta">申请于 {dt(r.createTime)}</span>}
          >
            <Descriptions column={2} size="small">
              <Descriptions.Item label="退款金额">
                <span className="mall-price">{money(r.refundAmount)}</span>
              </Descriptions.Item>
              <Descriptions.Item label="原因">{r.reason}</Descriptions.Item>
              {r.description && (
                <Descriptions.Item label="说明" span={2}>
                  {r.description}
                </Descriptions.Item>
              )}
              {r.returnAddress && (
                <Descriptions.Item label="退货地址" span={2}>
                  {r.returnAddress}
                </Descriptions.Item>
              )}
              {r.returnShipNo && (
                <Descriptions.Item label="顾客寄回" span={2}>
                  {r.returnCompany} {r.returnShipNo}
                </Descriptions.Item>
              )}
              {r.refuseReason && (
                <Descriptions.Item label="拒绝理由" span={2}>
                  {r.refuseReason}
                </Descriptions.Item>
              )}
            </Descriptions>

            <Space style={{ marginTop: 14 }} wrap>
              {r.status === 'PENDING' && (
                <>
                  <Popconfirm
                    title={
                      r.type === 'REFUND_ONLY'
                        ? '同意后将原路退款并恢复库存，确认？'
                        : '同意后将给出退货地址，确认？'
                    }
                    onConfirm={() => void agree(r)}
                  >
                    <Button size="small" type="primary">
                      同意
                    </Button>
                  </Popconfirm>
                  <Button size="small" danger onClick={() => setRefusing(r)}>
                    拒绝
                  </Button>
                </>
              )}
              {r.status === 'AGREED' && (
                <span className="mall-card__meta">已同意，等待顾客寄回</span>
              )}
              {r.status === 'RECEIVED' && (
                <Popconfirm title="确认已收到退货并完成退款？" onConfirm={() => void confirmReceived(r)}>
                  <Button size="small" type="primary">
                    确认收货并退款
                  </Button>
                </Popconfirm>
              )}
            </Space>
          </Card>
        ))
      )}

      {total > 10 && (
        <div style={{ textAlign: 'center', marginTop: 16 }}>
          <Pagination
            current={pageNum}
            pageSize={10}
            total={total}
            showSizeChanger={false}
            onChange={setPageNum}
          />
        </div>
      )}

      <Modal
        open={Boolean(refusing)}
        title="拒绝售后申请"
        onCancel={() => setRefusing(null)}
        onOk={() => void submitRefuse()}
        okText="确认拒绝"
        okButtonProps={{ danger: true }}
      >
        <Alert
          style={{ marginBottom: 16 }}
          type="warning"
          showIcon
          message="拒绝后订单会回到申请前的状态"
        />
        <Form form={form} layout="vertical">
          <Form.Item
            name="refuseReason"
            label="拒绝理由"
            rules={[{ required: true, message: '必须填写拒绝理由' }]}
          >
            <Input.TextArea rows={3} maxLength={255} placeholder="例如：商品已拆封使用，不符合退货条件" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
