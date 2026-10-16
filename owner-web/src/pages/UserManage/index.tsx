import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Card,
  Descriptions,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Spin,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
} from 'antd';
import type { TableProps } from 'antd';
import {
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import { createUser, getUserById, pageUsers, removeUser, updateUser } from '@/api/user';
import type { PageResult, SysUser, SysUserQuery } from '@/types/api';
import { toast } from '@/utils/feedback';

const { Text } = Typography;

const EMPTY_PAGE: PageResult<SysUser> = { rows: [], total: 0, pageNum: 1, pageSize: 10, pages: 0 };

const STATUS_OPTIONS = [
  { label: '正常', value: 0 },
  { label: '停用', value: 1 },
];

interface UserFormValues {
  username: string;
  nickname: string;
  mobile?: string;
  email?: string;
  deptName?: string;
  status?: number;
  remark?: string;
}

export default function UserManage() {
  const [searchForm] = Form.useForm<Partial<SysUserQuery>>();
  const [editForm] = Form.useForm<UserFormValues>();

  const [query, setQuery] = useState<SysUserQuery>({ pageNum: 1, pageSize: 10 });
  const [page, setPage] = useState<PageResult<SysUser>>(EMPTY_PAGE);
  const [loading, setLoading] = useState(false);

  const [modalOpen, setModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState<'create' | 'edit'>('create');
  const [editing, setEditing] = useState<SysUser | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState<SysUser | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);

  const load = useCallback(async (current: SysUserQuery) => {
    setLoading(true);
    try {
      setPage(await pageUsers(current));
    } catch {
      // 拦截器已提示
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load(query);
  }, [load, query]);

  const handleSearch = () => {
    const values = searchForm.getFieldsValue();
    setQuery({ ...values, pageNum: 1, pageSize: query.pageSize });
  };

  const handleReset = () => {
    searchForm.resetFields();
    setQuery({ pageNum: 1, pageSize: query.pageSize });
  };

  const handleTableChange: TableProps<SysUser>['onChange'] = (pagination) => {
    setQuery((prev) => ({
      ...prev,
      pageNum: pagination.current ?? 1,
      pageSize: pagination.pageSize ?? 10,
    }));
  };

  const openCreate = () => {
    setModalMode('create');
    setEditing(null);
    editForm.resetFields();
    editForm.setFieldsValue({ status: 0 } as UserFormValues);
    setModalOpen(true);
  };

  const openEdit = (record: SysUser) => {
    setModalMode('edit');
    setEditing(record);
    editForm.setFieldsValue({
      username: record.username,
      nickname: record.nickname,
      mobile: undefined, // 列表中的手机号已脱敏，不参与回填
      email: record.email,
      deptName: record.deptName,
      status: record.status ?? 0,
      remark: record.remark,
    });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    const values = await editForm.validateFields();
    setSubmitting(true);
    try {
      if (modalMode === 'create') {
        const id = await createUser({
          username: values.username,
          nickname: values.nickname,
          mobile: values.mobile,
          email: values.email,
          deptName: values.deptName,
          remark: values.remark,
        });
        toast.success(`新增成功，用户 ID：${id}`);
      } else if (editing) {
        await updateUser({
          id: editing.id,
          nickname: values.nickname,
          mobile: values.mobile,
          email: values.email,
          deptName: values.deptName,
          status: values.status,
          remark: values.remark,
          // 乐观锁：回传列表/详情中拿到的版本号
          version: editing.version,
        });
        toast.success('修改成功');
      }
      setModalOpen(false);
      void load(query);
    } catch {
      // 校验失败或接口异常，拦截器已提示
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (record: SysUser) => {
    try {
      await removeUser(record.id);
      toast.success('删除成功（逻辑删除）');
      // 当前页删空后回退一页
      const nextPageNum = page.rows.length === 1 && (query.pageNum ?? 1) > 1 ? (query.pageNum ?? 1) - 1 : query.pageNum;
      setQuery((prev) => ({ ...prev, pageNum: nextPageNum }));
      if (nextPageNum === query.pageNum) void load(query);
    } catch {
      // 拦截器已提示
    }
  };

  const handleShowDetail = async (record: SysUser) => {
    setDetailOpen(true);
    setDetailLoading(true);
    setDetail(record);
    try {
      setDetail(await getUserById(record.id));
    } catch {
      // 拦截器已提示
    } finally {
      setDetailLoading(false);
    }
  };

  const columns: TableProps<SysUser>['columns'] = [
    { title: 'ID', dataIndex: 'id', width: 90, fixed: 'left' },
    { title: '登录账号', dataIndex: 'username', width: 130 },
    { title: '昵称', dataIndex: 'nickname', width: 120 },
    {
      title: '手机号',
      dataIndex: 'mobile',
      width: 140,
      render: (value?: string) => (value ? <Text code>{value}</Text> : '-'),
    },
    { title: '邮箱', dataIndex: 'email', width: 200, render: (v?: string) => v ?? '-' },
    { title: '所属部门', dataIndex: 'deptName', width: 150, render: (v?: string) => v ?? '-' },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (value?: number) =>
        value === 1 ? <Tag color="red">停用</Tag> : <Tag color="green">正常</Tag>,
    },
    { title: '创建时间', dataIndex: 'createTime', width: 180, render: (v?: string) => v ?? '-' },
    {
      title: '操作',
      key: 'action',
      width: 200,
      fixed: 'right',
      render: (_, record) => (
        <Space size={4} wrap>
          <Tooltip title="查询详情（后端启用 Redis 缓存）">
            <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => void handleShowDetail(record)}>
              详情
            </Button>
          </Tooltip>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEdit(record)}>
            编辑
          </Button>
          <Popconfirm
            title="确认删除该用户？"
            description="后端为逻辑删除（del_flag = 1）"
            okText="删除"
            cancelText="取消"
            okButtonProps={{ danger: true }}
            onConfirm={() => void handleDelete(record)}
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <Tabs
      defaultActiveKey="portal"
      items={[
        {
          key: 'portal',
          label: '门户用户',
          children: (
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
              <Card>
                <Form form={searchForm} layout="inline" style={{ rowGap: 12 }} onFinish={handleSearch}>
          <Form.Item name="username" label="登录账号">
            <Input allowClear placeholder="支持模糊匹配" style={{ width: 160 }} />
          </Form.Item>
          <Form.Item name="nickname" label="昵称">
            <Input allowClear placeholder="支持模糊匹配" style={{ width: 160 }} />
          </Form.Item>
          <Form.Item name="deptName" label="所属部门">
            <Input allowClear placeholder="精确匹配" style={{ width: 160 }} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select allowClear placeholder="全部" style={{ width: 120 }} options={STATUS_OPTIONS} />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
                查询
              </Button>
              <Button icon={<ReloadOutlined />} onClick={handleReset}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card
        title={
          <Space>
            <span>用户列表</span>
            <Tag className="owner-chip">共 {page.total} 条</Tag>
          </Space>
        }
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新增用户
          </Button>
        }
      >
        <Table<SysUser>
          rowKey="id"
          size="middle"
          loading={loading}
          columns={columns}
          dataSource={page.rows}
          scroll={{ x: 1300 }}
          onChange={handleTableChange}
          pagination={{
            current: query.pageNum ?? 1,
            pageSize: query.pageSize ?? 10,
            total: page.total,
            showSizeChanger: true,
            showQuickJumper: true,
            pageSizeOptions: [10, 20, 50, 100],
            showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条 / 共 ${total} 条`,
          }}
        />
      </Card>

      <Modal
        open={modalOpen}
        title={modalMode === 'create' ? '新增门户用户' : `编辑门户用户 #${editing?.id ?? ''}`}
        okText="保存"
        cancelText="取消"
        confirmLoading={submitting}
        onOk={() => void handleSubmit()}
        onCancel={() => setModalOpen(false)}
        destroyOnHidden
        width={560}
      >
        <Form form={editForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="username"
            label="登录账号"
            rules={[
              { required: true, message: '请输入登录账号' },
              { min: 3, max: 32, message: '长度需在 3~32 之间' },
            ]}
          >
            <Input placeholder="例如 zhangsan" disabled={modalMode === 'edit'} />
          </Form.Item>

          <Form.Item name="nickname" label="昵称" rules={[{ required: true, message: '请输入昵称' }]}>
            <Input placeholder="例如 张三" maxLength={32} />
          </Form.Item>

          <Form.Item
            name="mobile"
            label="手机号"
            rules={[{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }]}
            extra={modalMode === 'edit' ? '列表中的手机号已脱敏，留空表示不修改' : undefined}
          >
            <Input placeholder="11 位手机号" maxLength={11} />
          </Form.Item>

          <Form.Item name="email" label="邮箱" rules={[{ type: 'email', message: '邮箱格式不正确' }]}>
            <Input placeholder="例如 zhangsan@owner.com" maxLength={64} />
          </Form.Item>

          <Form.Item name="deptName" label="所属部门">
            <Input placeholder="例如 平台研发中心" maxLength={64} />
          </Form.Item>

          {modalMode === 'edit' && (
            <Form.Item name="status" label="状态">
              <Select options={STATUS_OPTIONS} />
            </Form.Item>
          )}

          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} maxLength={255} showCount placeholder="可选" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        open={detailOpen}
        title="用户详情"
        footer={<Button onClick={() => setDetailOpen(false)}>关闭</Button>}
        onCancel={() => setDetailOpen(false)}
        destroyOnHidden
        width={600}
      >
        <Spin spinning={detailLoading}>
          <Descriptions column={1} size="small" bordered>
            <Descriptions.Item label="ID">{detail?.id ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="登录账号">{detail?.username ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="昵称">{detail?.nickname ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="手机号（已脱敏）">{detail?.mobile ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="邮箱">{detail?.email ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="所属部门">{detail?.deptName ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="状态">
              {detail?.status === 1 ? <Tag color="red">停用</Tag> : <Tag color="green">正常</Tag>}
            </Descriptions.Item>
            <Descriptions.Item label="乐观锁版本号">{detail?.version ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="创建时间">{detail?.createTime ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="更新时间">{detail?.updateTime ?? '-'}</Descriptions.Item>
          </Descriptions>
        </Spin>
        <Text type="secondary" style={{ display: 'block', marginTop: 12 }}>
          该接口在后端标注了 <Text code>@Cacheable(cacheNames = &quot;sysUser&quot;)</Text>，第二次请求将直接命中 Redis。
        </Text>
      </Modal>
            </Space>
          ),
        },
      ]}
    />
  );
}
