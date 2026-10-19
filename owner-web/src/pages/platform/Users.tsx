import { useCallback, useEffect, useState } from 'react';
import { Alert, Button, Card, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag } from 'antd';
import { platformApi } from '@/api/admin';
import { useAuthStore } from '@/store/auth';
import { toast } from '@/utils/feedback';
import { dt } from '@/utils/format';
import type { PlatformUser } from '@/types/mall';

export default function PlatformUsers() {
  const me = useAuthStore((state) => state.user);
  const [rows, setRows] = useState<PlatformUser[]>([]);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState<number | undefined>(undefined);
  const [loading, setLoading] = useState(true);
  const [target, setTarget] = useState<PlatformUser | null>(null);
  const [form] = Form.useForm();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const page = await platformApi.users({ keyword: keyword || undefined, status, pageNum, pageSize: 10 });
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

  const submitReset = async () => {
    if (!target) return;
    const values = await form.validateFields();
    try {
      await platformApi.resetPassword(String(target.id), values.password);
      toast.success('密码已重置');
      setTarget(null);
      form.resetFields();
      await load();
    } catch {
      /* 拦截器已提示 */
    }
  };

  return (
    <div>
      <h2 className="mall-section-title">平台 · 用户管理</h2>

      <Alert
        style={{ marginBottom: 16 }}
        type="info"
        showIcon
        message="这里管理的是「用户」，不是「店主」"
        description="店铺身份由店铺成员表决定，不在这张表里。所以这里不能把人设成店主——那是店铺后台或开通店铺时的事。"
      />

      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input.Search
            allowClear
            placeholder="账号 / 昵称 / 手机号"
            style={{ width: 240 }}
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
              { value: 1, label: '停用' },
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
          pageSize: 10,
          total,
          showSizeChanger: false,
          onChange: setPageNum,
        }}
        columns={[
          {
            title: '用户',
            render: (_, r) => (
              <div>
                <div>
                  {r.nickname} {r.platformAdmin && <Tag color="gold">平台管理员</Tag>}
                </div>
                <div className="mall-card__meta">{r.username}</div>
              </div>
            ),
          },
          { title: '手机号（打星）', dataIndex: 'mobileMasked', width: 160 },
          { title: '邮箱', dataIndex: 'email', width: 180 },
          {
            title: '密码',
            width: 100,
            render: (_, r) =>
              r.passwordSet ? <Tag color="green">已设置</Tag> : <Tag>未设置</Tag>,
          },
          {
            title: '状态',
            width: 90,
            render: (_, r) => <Tag color={r.status === 0 ? 'green' : 'red'}>{r.statusLabel}</Tag>,
          },
          { title: '注册时间', width: 150, render: (_, r) => dt(r.createTime) },
          {
            title: '操作',
            width: 190,
            render: (_, r) => {
              const isSelf = me && String(me.id) === String(r.id);
              return (
                <Space>
                  <Button
                    size="small"
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
                    onConfirm={async () => {
                      await platformApi.setUserStatus(String(r.id), r.status === 0 ? 1 : 0);
                      toast.success(r.status === 0 ? '账号已停用' : '账号已恢复');
                      await load();
                    }}
                  >
                    <Button size="small" danger={r.status === 0} disabled={Boolean(isSelf)}>
                      {r.status === 0 ? '停用' : '启用'}
                    </Button>
                  </Popconfirm>
                </Space>
              );
            },
          },
        ]}
      />

      <Modal
        open={Boolean(target)}
        title={`重置密码 · ${target?.nickname ?? ''}`}
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
            extra="重置后请通过安全渠道告知本人，系统不会发送明文短信"
          >
            <Input.Password maxLength={64} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
