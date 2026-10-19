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
import { categoryApi, shopApi } from '@/api/admin';
import { SingleImageUploader } from '@/components/ImageUploader';
import { isShopOwner, useAuthStore } from '@/store/auth';
import { toast } from '@/utils/feedback';
import { dt } from '@/utils/format';
import type { Category, Shop as ShopType, ShopMember } from '@/types/mall';

export default function Shop() {
  const shopId = useAuthStore((state) => state.currentShopId) ?? '';
  const shops = useAuthStore((state) => state.shops);
  const owner = isShopOwner(shops, shopId);

  const [profile, setProfile] = useState<ShopType | null>(null);
  const [members, setMembers] = useState<ShopMember[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [memberOpen, setMemberOpen] = useState(false);
  const [categoryOpen, setCategoryOpen] = useState(false);

  const [profileForm] = Form.useForm();
  const [memberForm] = Form.useForm();
  const [categoryForm] = Form.useForm();

  const loadProfile = useCallback(async () => {
    if (!shopId) return;
    try {
      const data = await shopApi.profile(shopId);
      setProfile(data);
      profileForm.setFieldsValue(data);
    } catch {
      /* ignore */
    }
  }, [shopId, profileForm]);

  const loadMembers = useCallback(async () => {
    if (!shopId) return;
    try {
      setMembers((await shopApi.members(shopId)) ?? []);
    } catch {
      /* ignore */
    }
  }, [shopId]);

  const loadCategories = useCallback(async () => {
    if (!shopId) return;
    try {
      setCategories((await categoryApi.tree(shopId)) ?? []);
    } catch {
      /* ignore */
    }
  }, [shopId]);

  useEffect(() => {
    void loadProfile();
    void loadMembers();
    void loadCategories();
  }, [loadProfile, loadMembers, loadCategories]);

  const saveProfile = async () => {
    const values = await profileForm.validateFields();
    try {
      await shopApi.updateProfile(shopId, { ...values, version: profile?.version });
      toast.success('店铺资料已保存');
      await loadProfile();
    } catch {
      /* 拦截器已提示 */
    }
  };

  const saveMember = async () => {
    const values = await memberForm.validateFields();
    try {
      await shopApi.addMember(shopId, {
        mobile: values.mobile.trim(),
        password: values.password,
        nickname: values.nickname.trim(),
        role: values.role,
      });
      toast.success('店员已添加');
      setMemberOpen(false);
      memberForm.resetFields();
      await loadMembers();
    } catch {
      /* 拦截器已提示 */
    }
  };

  const saveCategory = async () => {
    const values = await categoryForm.validateFields();
    try {
      await categoryApi.save(shopId, {
        parentId: values.parentId ?? '0',
        name: values.name,
        sort: values.sort ?? 0,
      });
      toast.success('类目已保存');
      setCategoryOpen(false);
      categoryForm.resetFields();
      await loadCategories();
    } catch {
      /* 拦截器已提示 */
    }
  };

  const parentOptions = [
    { value: '0', label: '作为一级类目' },
    ...categories.map((c) => ({ value: String(c.id), label: `挂在「${c.name}」下` })),
  ];

  const categoryRows = categories.flatMap((parent) => [
    { ...parent, isParent: true },
    ...(parent.children ?? []).map((child) => ({ ...child, isParent: false, parentName: parent.name })),
  ]);

  return (
    <div>
      <h2 className="mall-section-title">店铺与分类</h2>

      {!owner && (
        <Alert
          style={{ marginBottom: 16 }}
          type="warning"
          showIcon
          message="你是以店员身份进来的"
          description="店铺资料和店员管理只有店主（OWNER）可以修改，商品、库存、订单、售后不受影响。"
        />
      )}

      <Tabs
        items={[
          {
            key: 'profile',
            label: '店铺资料',
            children: (
              <Card>
                <Form form={profileForm} layout="vertical" disabled={!owner} style={{ maxWidth: 620 }}>
                  <Form.Item name="name" label="店铺名称" rules={[{ required: true, message: '请输入店名' }]}>
                    <Input maxLength={40} />
                  </Form.Item>
                  <Form.Item name="intro" label="店铺简介">
                    <Input.TextArea rows={2} maxLength={200} />
                  </Form.Item>
                  <Form.Item name="notice" label="店铺公告">
                    <Input.TextArea rows={2} maxLength={200} />
                  </Form.Item>
                  <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
                    <Form.Item
                      name="contactName"
                      label="联系人"
                      style={{ flex: '1 1 160px', minWidth: 0 }}
                    >
                      <Input maxLength={20} />
                    </Form.Item>
                    <Form.Item
                      name="contactMobile"
                      label="联系电话"
                      rules={[{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }]}
                      style={{ flex: '1 1 160px', minWidth: 0 }}
                    >
                      <Input maxLength={11} />
                    </Form.Item>
                  </div>
                  <Form.Item name="businessHours" label="营业时间">
                    <Input maxLength={40} placeholder="例如 每天 9:00 - 21:00" />
                  </Form.Item>
                  <Form.Item
                    name="shipAddress"
                    label="发货 / 退货地址"
                    extra="「退货退款」同意后会把这里作为退货收件地址发给顾客，务必填完整"
                  >
                    <Input.TextArea rows={2} maxLength={200} />
                  </Form.Item>
                  <Form.Item name="bannerUrl" label="店铺横幅">
                    <SingleImageUploader
                      folder="shop"
                      tip="显示在店铺首页顶部，建议 3:1，不超过 5 MB"
                    />
                  </Form.Item>
                  <Form.Item name="payNote" label="支付说明">
                    <Input maxLength={200} placeholder="例如：下单后 30 分钟内完成支付" />
                  </Form.Item>
                  <Button type="primary" disabled={!owner} onClick={() => void saveProfile()}>
                    保存
                  </Button>
                </Form>
              </Card>
            ),
          },
          {
            key: 'categories',
            label: '类目管理',
            children: (
              <Card
                title="两级类目"
                extra={
                  <Button
                    type="primary"
                    onClick={() => {
                      categoryForm.resetFields();
                      categoryForm.setFieldsValue({ parentId: '0', sort: 0 });
                      setCategoryOpen(true);
                    }}
                  >
                    新增类目
                  </Button>
                }
              >
                <Table
                  rowKey="id"
                  scroll={{ x: 'max-content' }}
                  pagination={false}
                  dataSource={categoryRows}
                  columns={[
                    {
                      title: '类目',
                      render: (_, r: Category & { isParent: boolean; parentName?: string }) => (
                        <span style={{ paddingLeft: r.isParent ? 0 : 20 }}>
                          {r.isParent ? r.name : `└ ${r.name}`}
                        </span>
                      ),
                    },
                    {
                      title: '层级',
                      width: 90,
                      render: (_, r: Category & { isParent: boolean }) =>
                        r.isParent ? <Tag>一级</Tag> : <Tag color="blue">二级</Tag>,
                    },
                    { title: '排序', dataIndex: 'sort', width: 80 },
                    {
                      title: '状态',
                      width: 100,
                      render: (_, r: Category) => (
                        <Tag color={r.status === 0 ? 'green' : 'default'}>{r.statusLabel}</Tag>
                      ),
                    },
                    {
                      title: '操作',
                      width: 120,
                      render: (_, r: Category) => (
                        <Button
                          size="small"
                          onClick={async () => {
                            await categoryApi.setStatus(shopId, String(r.id), r.status === 0 ? 1 : 0);
                            toast.success(r.status === 0 ? '已停用' : '已启用');
                            await loadCategories();
                          }}
                        >
                          {r.status === 0 ? '停用' : '启用'}
                        </Button>
                      ),
                    },
                  ]}
                />
              </Card>
            ),
          },
          {
            key: 'members',
            label: '店员管理',
            children: (
              <Card
                title={`共 ${members.length} 人`}
                extra={
                  <Button
                    type="primary"
                    disabled={!owner}
                    onClick={() => {
                      memberForm.resetFields();
                      memberForm.setFieldsValue({ role: 'STAFF' });
                      setMemberOpen(true);
                    }}
                  >
                    添加店员
                  </Button>
                }
              >
                <Table
                  rowKey="id"
                  scroll={{ x: 'max-content' }}
                  pagination={false}
                  dataSource={members}
                  columns={[
                    { title: '昵称', dataIndex: 'nickname' },
                    { title: '账号', dataIndex: 'username' },
                    { title: '手机号', dataIndex: 'mobile' },
                    {
                      title: '角色',
                      width: 110,
                      render: (_, r) => (
                        <Tag color={r.role === 'OWNER' ? 'gold' : 'blue'}>{r.roleLabel}</Tag>
                      ),
                    },
                    {
                      title: '状态',
                      width: 100,
                      render: (_, r) => (
                        <Tag color={r.status === 0 ? 'green' : 'default'}>{r.statusLabel}</Tag>
                      ),
                    },
                    { title: '加入时间', width: 150, render: (_, r) => dt(r.createTime) },
                    {
                      title: '操作',
                      width: 160,
                      render: (_, r) => (
                        <Space>
                          <Select
                            size="small"
                            disabled={!owner}
                            value={r.role}
                            style={{ width: 90 }}
                            options={[
                              { value: 'OWNER', label: '店主' },
                              { value: 'STAFF', label: '店员' },
                            ]}
                            onChange={async (value) => {
                              try {
                                await shopApi.updateMember(shopId, String(r.id), {
                                  role: value,
                                  status: r.status,
                                });
                                toast.success('角色已更新');
                                await loadMembers();
                              } catch {
                                /* 例如把最后一个店主降级 */
                              }
                            }}
                          />
                          <Button
                            size="small"
                            disabled={!owner}
                            onClick={async () => {
                              await shopApi.updateMember(shopId, String(r.id), {
                                role: r.role,
                                status: r.status === 0 ? 1 : 0,
                              });
                              toast.success(r.status === 0 ? '已停用' : '已启用');
                              await loadMembers();
                            }}
                          >
                            {r.status === 0 ? '停用' : '启用'}
                          </Button>
                        </Space>
                      ),
                    },
                  ]}
                />
                <Alert
                  style={{ marginTop: 16 }}
                  type="info"
                  showIcon
                  message="每家店至少要留一个店主"
                  description="把最后一个店主降级为店员会被后端拒绝；被停用的店员将无法登录后台。"
                />
              </Card>
            ),
          },
        ]}
      />

      <Modal
        open={memberOpen}
        title="添加店员"
        onCancel={() => setMemberOpen(false)}
        onOk={() => void saveMember()}
        okText="添加"
      >
        <Alert
          style={{ marginBottom: 16 }}
          type="info"
          showIcon
          message="店员用的是「用户」账号"
          description="系统里只有用户和店铺两种长期实体。手机号若已注册就直接建立店员关系，未注册则新建一个用户账号。"
        />
        <Form form={memberForm} layout="vertical">
          <Form.Item
            name="mobile"
            label="手机号"
            rules={[
              { required: true, message: '请输入手机号' },
              { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' },
            ]}
          >
            <Input maxLength={11} />
          </Form.Item>
          <Form.Item name="nickname" label="昵称" rules={[{ required: true, message: '请输入昵称' }]}>
            <Input maxLength={20} />
          </Form.Item>
          <Form.Item
            name="password"
            label="初始密码"
            rules={[
              { required: true, message: '请输入初始密码' },
              { min: 6, message: '至少 6 位' },
            ]}
          >
            <Input.Password />
          </Form.Item>
          <Form.Item name="role" label="角色" rules={[{ required: true }]}>
            <Select
              options={[
                { value: 'STAFF', label: '店员（可管商品/订单，改不了店铺资料）' },
                { value: 'OWNER', label: '店主（全部权限）' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        open={categoryOpen}
        title="新增类目"
        onCancel={() => setCategoryOpen(false)}
        onOk={() => void saveCategory()}
        okText="保存"
      >
        <Form form={categoryForm} layout="vertical">
          <Form.Item name="parentId" label="父级" rules={[{ required: true }]}>
            <Select options={parentOptions} />
          </Form.Item>
          <Form.Item name="name" label="类目名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input maxLength={20} />
          </Form.Item>
          <Form.Item name="sort" label="排序" extra="越小越靠前">
            <InputNumber min={0} max={9999} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
