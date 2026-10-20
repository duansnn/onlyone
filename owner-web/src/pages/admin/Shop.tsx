import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Table,
  Tabs,
  Tag,
  Tooltip,
} from 'antd';
import {
  CheckCircleFilled,
  ClockCircleOutlined,
  CrownOutlined,
  EditOutlined,
  EnvironmentOutlined,
  InfoCircleOutlined,
  PhoneOutlined,
  PlusOutlined,
  ReloadOutlined,
  ShopOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { categoryApi, shopApi } from '@/api/admin';
import { SingleImageUploader } from '@/components/ImageUploader';
import SafeImage from '@/components/SafeImage';
import { isShopOwner, useAuthStore } from '@/store/auth';
import { toast } from '@/utils/feedback';
import { dt } from '@/utils/format';
import type { Category, Shop as ShopType, ShopMember } from '@/types/mall';

const FALLBACK_BANNER = '/images/hero-daily-goods.jpg';

const ROLE_OPTIONS = [
  {
    value: 'STAFF',
    title: '店员',
    desc: '可管商品、库存、订单、售后；改不了店铺资料，也加不了人',
    icon: <UserOutlined />,
  },
  {
    value: 'OWNER',
    title: '店主',
    desc: '全部权限，包括店铺资料、店员管理与优惠券',
    icon: <CrownOutlined />,
  },
];

/** 十位随机密码，去掉 0/O/1/l/I 这类容易看错的字符 */
function randomPassword() {
  const chars = 'abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  return Array.from({ length: 10 }, () => chars[Math.floor(Math.random() * chars.length)]).join('');
}

/** 角色选择：两张卡片，比下拉更容易看清权限差别 */
function RolePicker({
  value,
  onChange,
  disabled,
}: {
  value?: string;
  onChange?: (value: string) => void;
  disabled?: boolean;
}) {
  return (
    <div className="role-pick">
      {ROLE_OPTIONS.map((opt) => (
        <button
          key={opt.value}
          type="button"
          disabled={disabled}
          className={`role-pick__item${value === opt.value ? ' is-active' : ''}`}
          onClick={() => onChange?.(opt.value)}
        >
          <span className="role-pick__icon">{opt.icon}</span>
          <span className="role-pick__text">
            <b>{opt.title}</b>
            <small>{opt.desc}</small>
          </span>
          {value === opt.value && <CheckCircleFilled className="role-pick__ok" />}
        </button>
      ))}
    </div>
  );
}

export default function Shop() {
  const shopId = useAuthStore((state) => state.currentShopId) ?? '';
  const shops = useAuthStore((state) => state.shops);
  const owner = isShopOwner(shops, shopId);

  const [profile, setProfile] = useState<ShopType | null>(null);
  const [members, setMembers] = useState<ShopMember[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [memberOpen, setMemberOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<ShopMember | null>(null);
  const [categoryOpen, setCategoryOpen] = useState(false);

  const [profileForm] = Form.useForm();
  const [memberForm] = Form.useForm();
  const [editForm] = Form.useForm();
  const [categoryForm] = Form.useForm();

  // 右侧预览跟着表单走
  const watchName = Form.useWatch<string>('name', profileForm);
  const watchIntro = Form.useWatch<string>('intro', profileForm);
  const watchNotice = Form.useWatch<string>('notice', profileForm);
  const watchBanner = Form.useWatch<string>('bannerUrl', profileForm);
  const watchContact = Form.useWatch<string>('contactName', profileForm);
  const watchMobile = Form.useWatch<string>('contactMobile', profileForm);
  const watchHours = Form.useWatch<string>('businessHours', profileForm);
  const watchShip = Form.useWatch<string>('shipAddress', profileForm);

  const watchMemberNick = Form.useWatch<string>('nickname', memberForm);
  const watchEditNick = Form.useWatch<string>('nickname', editForm);

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
      toast.success('店员已添加，把手机号和初始密码告诉他即可登录');
      setMemberOpen(false);
      memberForm.resetFields();
      await loadMembers();
    } catch {
      /* 拦截器已提示 */
    }
  };

  /** 打开编辑：把现有资料填进去，密码留空表示不改 */
  const openEdit = (member: ShopMember) => {
    setEditTarget(member);
    editForm.resetFields();
    editForm.setFieldsValue({
      nickname: member.nickname || '',
      mobile: member.mobile || member.username,
      password: '',
    });
  };

  const saveEdit = async () => {
    if (!editTarget) return;
    const values = await editForm.validateFields();
    try {
      await shopApi.updateMember(shopId, String(editTarget.id), {
        nickname: values.nickname.trim(),
        mobile: values.mobile.trim(),
        password: values.password ? values.password : undefined,
      });
      toast.success('成员资料已更新');
      setEditTarget(null);
      editForm.resetFields();
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

  const ownerCount = members.filter((m) => m.role === 'OWNER' && m.status === 0).length;

  const checks = [
    { label: '已填店铺名称', done: Boolean(watchName?.trim()) },
    { label: '已写店铺简介', done: Boolean(watchIntro?.trim()) },
    { label: '已填退货地址', done: Boolean(watchShip?.trim()) },
    { label: '有店铺横幅', done: Boolean(watchBanner) },
  ];

  return (
    <div>
      <h2 className="mall-section-title">店铺与分类</h2>

      {!owner && (
        <Alert
          style={{ marginBottom: 16 }}
          type="warning"
          showIcon
          message="你是以店员身份进来的"
          description="店铺资料和店员管理只有店主可以修改，商品、库存、订单、售后不受影响。"
        />
      )}

      <Tabs
        items={[
          {
            key: 'profile',
            label: '店铺资料',
            children: (
              <Form form={profileForm} layout="vertical" className="shop-form" disabled={!owner}>
                <div className="shop-pane">
                  <div className="shop-pane__main">
                    {/* 01 品牌形象 */}
                    <section className="pf-block">
                      <header className="pf-block__head">
                        <span className="pf-block__num">01</span>
                        <div>
                          <h3 className="pf-block__title">
                            <ShopOutlined /> 品牌形象
                          </h3>
                          <p className="pf-block__desc">
                            店名会出现在店头、订单和评价里；横幅显示在店铺首页顶部
                          </p>
                        </div>
                      </header>

                      <Form.Item
                        name="name"
                        label="店铺名称"
                        rules={[{ required: true, message: '请输入店名' }]}
                      >
                        <Input maxLength={40} showCount placeholder="例如 日用铺" />
                      </Form.Item>
                      <Form.Item
                        name="intro"
                        label="店铺简介"
                        extra="一句话讲清楚卖什么，显示在首页标题下方"
                      >
                        <Input.TextArea rows={2} maxLength={200} showCount />
                      </Form.Item>
                      <Form.Item name="bannerUrl" label="店铺横幅" style={{ marginBottom: 0 }}>
                        <SingleImageUploader
                          folder="shop"
                          tip="建议 3:1，不超过 5 MB；不传就用默认场景图"
                        />
                      </Form.Item>
                    </section>

                    {/* 02 联系与营业 */}
                    <section className="pf-block">
                      <header className="pf-block__head">
                        <span className="pf-block__num">02</span>
                        <div>
                          <h3 className="pf-block__title">
                            <PhoneOutlined /> 联系与营业
                          </h3>
                          <p className="pf-block__desc">
                            顾客在店铺首页能看到营业时间和公告，客服电话只在售后时展示
                          </p>
                        </div>
                      </header>

                      <div className="pf-grid2">
                        <Form.Item name="contactName" label="联系人">
                          <Input maxLength={20} placeholder="例如 日用铺掌柜" />
                        </Form.Item>
                        <Form.Item
                          name="contactMobile"
                          label="联系电话"
                          rules={[{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }]}
                        >
                          <Input maxLength={11} placeholder="11 位手机号" />
                        </Form.Item>
                      </div>

                      <div className="pf-grid2">
                        <Form.Item name="businessHours" label="营业时间">
                          <Input maxLength={40} placeholder="例如 每天 9:00 - 21:00" />
                        </Form.Item>
                        <Form.Item name="payNote" label="支付说明">
                          <Input maxLength={200} placeholder="例如：线下付款，收款后统一发货" />
                        </Form.Item>
                      </div>

                      <Form.Item name="notice" label="店铺公告" style={{ marginBottom: 0 }}>
                        <Input.TextArea
                          rows={2}
                          maxLength={200}
                          showCount
                          placeholder="放假、发货延迟、活动通知……写在这里会挂在店铺首页"
                        />
                      </Form.Item>
                    </section>

                    {/* 03 售后地址 */}
                    <section className="pf-block">
                      <header className="pf-block__head">
                        <span className="pf-block__num">03</span>
                        <div>
                          <h3 className="pf-block__title">
                            <EnvironmentOutlined /> 售后地址
                          </h3>
                          <p className="pf-block__desc">
                            「退货退款」同意后会把这里作为退货收件地址发给顾客
                          </p>
                        </div>
                      </header>
                      <Form.Item name="shipAddress" label="发货 / 退货地址" style={{ marginBottom: 0 }}>
                        <Input.TextArea
                          rows={2}
                          maxLength={200}
                          showCount
                          placeholder="省 / 市 / 区 + 详细地址 + 收件人"
                        />
                      </Form.Item>
                    </section>
                  </div>

                  {/* 右侧预览 */}
                  <aside className="shop-side">
                    <div className="shop-side__label">店面预览</div>
                    <div className="shop-preview">
                      <div className="shop-preview__banner">
                        <SafeImage src={watchBanner || FALLBACK_BANNER} alt="" />
                        <em className="shop-preview__badge">营业中</em>
                      </div>
                      <div className="shop-preview__body">
                        <span className="shop-preview__logo">
                          {(watchName?.trim() || '日').slice(0, 1)}
                        </span>
                        <div className="shop-preview__name">{watchName?.trim() || '店铺名称'}</div>
                        <div className="shop-preview__intro">
                          {watchIntro?.trim() || '一句话简介会显示在这里'}
                        </div>
                        <ul className="shop-preview__meta">
                          <li>
                            <PhoneOutlined />
                            {watchContact?.trim() || '联系人'}
                            {watchMobile ? ` · ${watchMobile}` : ''}
                          </li>
                          <li>
                            <ClockCircleOutlined />
                            {watchHours?.trim() || '营业时间未填'}
                          </li>
                          <li>
                            <EnvironmentOutlined />
                            {watchShip?.trim() ? '退货地址已填' : '退货地址未填'}
                          </li>
                        </ul>
                      </div>
                      {watchNotice?.trim() && (
                        <div className="shop-preview__notice">公告 · {watchNotice.trim()}</div>
                      )}
                    </div>

                    <div className="pf-checks">
                      <div className="pf-checks__title">店铺完整度</div>
                      <ul>
                        {checks.map((item) => (
                          <li key={item.label} className={item.done ? 'is-done' : ''}>
                            <CheckCircleFilled />
                            {item.label}
                          </li>
                        ))}
                      </ul>
                      <p className="pf-checks__tip">
                        退货地址尤其重要：没有它，顾客同意退款后不知道该寄回哪里
                      </p>
                    </div>
                  </aside>
                </div>

                <div className="shop-save">
                  <span className="shop-save__hint">
                    <InfoCircleOutlined />
                    {profile?.updateTime ? `上次保存于 ${dt(profile.updateTime)}` : '还没有保存过'}
                  </span>
                  <Button type="primary" disabled={!owner} onClick={() => void saveProfile()}>
                    保存店铺资料
                  </Button>
                </div>
              </Form>
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
                    icon={<PlusOutlined />}
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
                        r.isParent ? <Tag>一级</Tag> : <Tag variant="filled" className="admin-tag">二级</Tag>,
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
              <div>
                <div className="member-head">
                  <div>
                    <div className="member-head__title">
                      <TeamOutlined /> 共 {members.length} 人
                    </div>
                    <div className="member-head__sub">
                      店主 {ownerCount} 人 · 店员 {members.length - ownerCount} 人
                      {ownerCount <= 1 && ' · 至少保留一个店主，否则没人能改店铺资料'}
                    </div>
                  </div>
                  <Button
                    type="primary"
                    icon={<PlusOutlined />}
                    disabled={!owner}
                    onClick={() => {
                      memberForm.resetFields();
                      memberForm.setFieldsValue({ role: 'STAFF', password: randomPassword() });
                      setMemberOpen(true);
                    }}
                  >
                    添加店员
                  </Button>
                </div>

                {members.length === 0 ? (
                  <Card>
                    <Empty description="还没有店员，添加后他就能用手机号登录后台" />
                  </Card>
                ) : (
                  <div className="member-grid">
                    {members.map((m) => (
                      <div
                        key={String(m.id)}
                        className={`member-card${m.status === 0 ? '' : ' is-off'}`}
                      >
                        <div className="member-card__top">
                          <span
                            className={`member-card__avatar${m.role === 'OWNER' ? ' is-owner' : ''}`}
                          >
                            {(m.nickname || m.username || 'U').slice(0, 1).toUpperCase()}
                          </span>
                          <div className="member-card__id">
                            <div className="member-card__name">
                              {m.nickname || m.username}
                              <span className={`member-card__role is-${m.role.toLowerCase()}`}>
                                {m.roleLabel}
                              </span>
                            </div>
                            <div className="member-card__acct">
                              {m.mobile || m.username}
                            </div>
                          </div>
                        </div>

                        <div className="member-card__meta">
                          <span>加入于 {dt(m.createTime)}</span>
                          <span className={m.status === 0 ? 'is-on' : 'is-off'}>{m.statusLabel}</span>
                        </div>

                        <div className="member-card__ops">
                          <Select
                            size="small"
                            disabled={!owner}
                            value={m.role}
                            style={{ width: 92 }}
                            options={[
                              { value: 'OWNER', label: '店主' },
                              { value: 'STAFF', label: '店员' },
                            ]}
                            onChange={async (value) => {
                              try {
                                await shopApi.updateMember(shopId, String(m.id), {
                                  role: value,
                                  status: m.status,
                                });
                                toast.success('角色已更新');
                                await loadMembers();
                              } catch {
                                /* 例如把最后一个店主降级 */
                              }
                            }}
                          />
                          <div className="member-card__btns">
                            <Button
                              size="small"
                              icon={<EditOutlined />}
                              disabled={!owner}
                              onClick={() => openEdit(m)}
                            >
                              编辑
                            </Button>
                            <Popconfirm
                              title={m.status === 0 ? '停用后他将无法登录后台' : '恢复他的后台登录'}
                              onConfirm={async () => {
                                await shopApi.updateMember(shopId, String(m.id), {
                                  role: m.role,
                                  status: m.status === 0 ? 1 : 0,
                                });
                                toast.success(m.status === 0 ? '已停用' : '已启用');
                                await loadMembers();
                              }}
                            >
                              <Button size="small" danger={m.status === 0} disabled={!owner}>
                                {m.status === 0 ? '停用' : '启用'}
                              </Button>
                            </Popconfirm>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}

                <Alert
                  style={{ marginTop: 16 }}
                  type="info"
                  showIcon
                  message="店员不需要自己注册"
                  description="你在这里填手机号和初始密码，系统就会建好账号（已注册的手机号直接挂上来）。店员用「手机号 + 初始密码」在登录页登录，自动进入店铺后台。要改名字、换手机号或重置密码，点卡片上的「编辑」。"
                />
              </div>
            ),
          },
        ]}
      />

      {/* 添加店员 */}
      <Modal
        open={memberOpen}
        title={
          <div className="member-modal__title">
            <span className="member-modal__badge">
              <TeamOutlined />
            </span>
            <span>
              添加店员
              <small>不需要他自己注册，你来建账号</small>
            </span>
          </div>
        }
        width={560}
        okText="创建账号并加入"
        cancelText="取消"
        onCancel={() => setMemberOpen(false)}
        onOk={() => void saveMember()}
      >
        <Form form={memberForm} layout="vertical" className="member-form">
          <div className="member-form__preview">
            <span className="member-form__avatar">
              {(watchMemberNick?.trim() || '新').slice(0, 1).toUpperCase()}
            </span>
            <div>
              <div className="member-form__name">{watchMemberNick?.trim() || '新店员'}</div>
              <div className="member-form__hint">
                手机号就是登录账号，初始密码建好后可以随时重置
              </div>
            </div>
          </div>

          <div className="pf-grid2">
            <Form.Item
              name="mobile"
              label="手机号（登录账号）"
              rules={[
                { required: true, message: '请输入手机号' },
                { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' },
              ]}
              extra="已注册的手机号会直接加入本店，不会重复建号"
            >
              <Input maxLength={11} placeholder="11 位手机号" />
            </Form.Item>
            <Form.Item
              name="nickname"
              label="昵称"
              rules={[{ required: true, message: '请输入昵称' }]}
            >
              <Input maxLength={20} placeholder="后台显示的名字" />
            </Form.Item>
          </div>

          <Form.Item
            name="password"
            label="初始密码"
            rules={[
              { required: true, message: '请输入初始密码' },
              { min: 6, message: '至少 6 位' },
            ]}
            extra="建议生成一个随机密码，让店员登录后自己改"
          >
            <Input
              maxLength={32}
              addonAfter={
                <Tooltip title="生成随机密码">
                  <Button
                    type="text"
                    size="small"
                    icon={<ReloadOutlined />}
                    onClick={() => memberForm.setFieldValue('password', randomPassword())}
                  >
                    随机
                  </Button>
                </Tooltip>
              }
            />
          </Form.Item>

          <Form.Item name="role" label="角色" rules={[{ required: true, message: '请选择角色' }]}>
            <RolePicker />
          </Form.Item>
        </Form>
      </Modal>

      {/* 编辑成员资料 */}
      <Modal
        open={Boolean(editTarget)}
        title={
          <div className="member-modal__title">
            <span className="member-modal__badge is-edit">
              <EditOutlined />
            </span>
            <span>
              编辑成员资料
              <small>昵称、手机号、密码都能改，保存后立即生效</small>
            </span>
          </div>
        }
        width={560}
        okText="保存修改"
        cancelText="取消"
        onCancel={() => setEditTarget(null)}
        onOk={() => void saveEdit()}
      >
        <Form form={editForm} layout="vertical" className="member-form">
          <div className="member-form__preview">
            <span className="member-form__avatar">
              {(watchEditNick?.trim() || editTarget?.nickname || 'U').slice(0, 1).toUpperCase()}
            </span>
            <div>
              <div className="member-form__name">
                {watchEditNick?.trim() || editTarget?.nickname || '成员'}
                <span className={`member-card__role is-${(editTarget?.role || 'STAFF').toLowerCase()}`}>
                  {editTarget?.roleLabel}
                </span>
              </div>
              <div className="member-form__hint">
                {editTarget?.role === 'OWNER'
                  ? '店主拥有店铺全部权限，包括店铺资料与店员管理'
                  : '店员用手机号登录，进来就是店铺后台'}
              </div>
            </div>
          </div>

          <div className="pf-grid2">
            <Form.Item name="nickname" label="昵称" rules={[{ required: true, message: '请输入昵称' }]}>
              <Input maxLength={20} placeholder="后台显示的名字" />
            </Form.Item>
            <Form.Item
              name="mobile"
              label="手机号（登录账号）"
              rules={[
                { required: true, message: '请输入手机号' },
                { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' },
              ]}
              extra="换号码时登录账号一起换，改了要通知他"
            >
              <Input maxLength={11} placeholder="11 位手机号" />
            </Form.Item>
          </div>

          <Form.Item
            name="password"
            label="重置密码"
            rules={[{ min: 6, message: '至少 6 位' }]}
            extra="留空表示不改密码；重置后请把新密码告诉他"
          >
            <Input.Password
              maxLength={32}
              placeholder="不修改就留空"
              autoComplete="new-password"
              addonAfter={
                <Tooltip title="生成随机密码">
                  <Button
                    type="text"
                    size="small"
                    icon={<ReloadOutlined />}
                    onClick={() => editForm.setFieldValue('password', randomPassword())}
                  >
                    随机
                  </Button>
                </Tooltip>
              }
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 新增类目 */}
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
