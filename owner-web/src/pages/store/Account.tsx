import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Card,
  Cascader,
  Divider,
  Empty,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Switch,
  Table,
  Tabs,
  Tag,
  message as antdMessage,
} from 'antd';
import { Link, Navigate, useSearchParams } from 'react-router-dom';
import { addressApi, authApi, couponApi, refundApi, storeApi } from '@/api/mall';
import {
  findRegionPath,
  loadRegions,
  REGION_FIELD_NAMES,
  splitRegion,
  type RegionNode,
} from '@/data/regions';
import { useAuthStore, isMerchant, shopRoleLabel } from '@/store/auth';
import { useShopStore } from '@/store/shop';
import { toast } from '@/utils/feedback';
import { dt, money, refundStatusTone } from '@/utils/format';
import StatusTag from '@/components/StatusTag';
import AvatarUploader from '@/components/AvatarUploader';
import type { Address, Coupon, CouponUser, Refund } from '@/types/mall';

/** 这几种状态的券允许顾客自己删掉；占用中和已核销的要留着 */
const DELETABLE_COUPON_STATUS = ['UNUSED', 'EXPIRED', 'RETURNED'];

export default function Account() {
  const [searchParams, setSearchParams] = useSearchParams();
  const tab = searchParams.get('tab') ?? 'profile';
  const { user, shops, currentShopId, setUser } = useAuthStore();
  const shopId = useShopStore((state) => state.shopId);
  // 店主、店员都是 MERCHANT，具体身份看当前店铺里的 role
  const roleLabel = shopRoleLabel(shops, currentShopId);

  const [addresses, setAddresses] = useState<Address[]>([]);
  const [coupons, setCoupons] = useState<CouponUser[]>([]);
  const [center, setCenter] = useState<Coupon[]>([]);
  const [refunds, setRefunds] = useState<Refund[]>([]);
  const [addressOpen, setAddressOpen] = useState(false);
  const [editing, setEditing] = useState<Address | null>(null);
  const [regions, setRegions] = useState<RegionNode[]>([]);
  const [shipTarget, setShipTarget] = useState<Refund | null>(null);
  /** 我的券按状态筛选，空串为全部 */
  const [couponStatus, setCouponStatus] = useState('');

  const [profileForm] = Form.useForm();
  const [pwdForm] = Form.useForm();
  const [mobileForm] = Form.useForm();
  const [addressForm] = Form.useForm();
  const [shipForm] = Form.useForm();

  // 昵称会随输入实时变化，卡片头部那行名字跟着走
  const profileName = Form.useWatch<string>('nickname', profileForm);

  const loadAddresses = useCallback(async () => {
    try {
      setAddresses(await addressApi.list());
    } catch {
      /* ignore */
    }
  }, []);

  const loadCoupons = useCallback(async () => {
    try {
      const [mine, centerPage] = await Promise.all([
        couponApi.mine(couponStatus ? { status: couponStatus } : undefined),
        shopId ? couponApi.center(shopId, { pageNum: 1, pageSize: 20 }) : Promise.resolve(null),
      ]);
      setCoupons(mine ?? []);
      setCenter(centerPage?.rows ?? []);
    } catch {
      /* ignore */
    }
  }, [shopId, couponStatus]);

  const loadRefunds = useCallback(async () => {
    try {
      const page = await refundApi.mine({ pageNum: 1, pageSize: 50 });
      setRefunds(page.rows ?? []);
    } catch {
      /* ignore */
    }
  }, []);

  useEffect(() => {
    if (user) {
      profileForm.setFieldsValue({
        nickname: user.nickname,
        email: user.email,
        avatarUrl: user.avatarUrl,
      });
    }
  }, [user, profileForm]);

  useEffect(() => {
    if (tab === 'address') void loadAddresses();
    if (tab === 'coupons') void loadCoupons();
    if (tab === 'refunds') void loadRefunds();
  }, [tab, loadAddresses, loadCoupons, loadRefunds]);

  const saveProfile = async () => {
    const values = await profileForm.validateFields();
    // 空字符串代表「清掉头像」，后端会 trimToNull 落成 null；不传则保持原值
    const me = await authApi.updateProfile({ ...values, avatarUrl: values.avatarUrl ?? '' });
    setUser(me);
    toast.success('资料已更新');
  };

  const changePassword = async () => {
    const values = await pwdForm.validateFields();
    await authApi.changePassword(values);
    toast.success('密码已修改');
    pwdForm.resetFields();
  };

  const sendCode = async () => {
    const mobile = mobileForm.getFieldValue('mobile');
    if (!/^1[3-9]\d{9}$/.test(mobile ?? '')) {
      toast.warning('请先输入正确的新手机号');
      return;
    }
    const result = await authApi.sendMobileCode(mobile);
    if (result?.devCode) {
      antdMessage.info(`本地联调验证码：${result.devCode}`);
      mobileForm.setFieldValue('code', result.devCode);
    } else {
      toast.success('验证码已发送');
    }
  };

  const changeMobile = async () => {
    const values = await mobileForm.validateFields();
    await authApi.changeMobile(values);
    const me = await authApi.me();
    setUser(me);
    toast.success('手机号已更换');
    mobileForm.resetFields();
  };

  /** 打开地址弹窗；首次打开时才加载省市区数据（独立 chunk），编辑时把名称还原成级联路径 */
  const openAddress = async (target?: Address) => {
    const options = regions.length ? regions : await loadRegions();
    if (!regions.length) setRegions(options);
    setEditing(target ?? null);
    addressForm.resetFields();
    if (target) {
      addressForm.setFieldsValue({
        ...target,
        region: findRegionPath(target.province, target.city, target.district),
        isDefault: target.isDefault === 1,
      });
    }
    setAddressOpen(true);
  };

  const saveAddress = async () => {
    const values = await addressForm.validateFields();
    // 级联选择器给的是一个路径数组，提交前拆回 province / city / district
    const { region, ...rest } = values;
    const payload = { ...rest, ...splitRegion(region) };
    if (editing) await addressApi.update({ ...payload, id: editing.id });
    else await addressApi.create(payload);
    toast.success('地址已保存');
    setAddressOpen(false);
    setEditing(null);
    addressForm.resetFields();
    await loadAddresses();
  };

  const submitReturnShip = async () => {
    if (!shipTarget) return;
    const values = await shipForm.validateFields();
    await refundApi.returnShip(String(shipTarget.id), values);
    toast.success('退货运单号已提交，等店主确认收货');
    setShipTarget(null);
    shipForm.resetFields();
    await loadRefunds();
  };

  // 旧链接 /account?tab=orders 曾因为不存在该 tab 而白屏，订单其实是独立页面
  if (tab === 'orders' || tab === 'order') return <Navigate to="/orders" replace />;

  return (
    <div>
      <h2 className="mall-section-title">我的账号</h2>
      <Tabs
        activeKey={tab}
        onChange={(key) => setSearchParams({ tab: key })}
        items={[
          {
            key: 'profile',
            label: '个人资料',
            children: (
              <div className="acc-pf">
                <Space direction="vertical" size={16} style={{ width: '100%', minWidth: 0 }}>
                <Card>
                  <Form form={profileForm} layout="vertical">
                    <div className="acc-head">
                      <Form.Item name="avatarUrl" noStyle>
                        <AvatarUploader size={84} tip="" />
                      </Form.Item>
                      <div className="acc-head__meta">
                        <div className="acc-head__name">
                          {profileName || user?.nickname || user?.username || '未命名'}
                        </div>
                        <div className="acc-head__sub">
                          {user?.mobile ? `手机号 ${user.mobile}` : `账号 ${user?.username ?? '-'}`}
                        </div>
                        <span className="acc-head__chip">
                          {roleLabel ?? (isMerchant(user) ? '店主' : '顾客')}
                        </span>
                      </div>
                    </div>

                    <Form.Item
                      name="nickname"
                      label="昵称"
                      rules={[{ required: true, message: '请输入昵称' }]}
                    >
                      <Input maxLength={20} placeholder="怎么称呼你" />
                    </Form.Item>
                    <Form.Item name="email" label="邮箱" extra="用于找回账号，不会展示给其他顾客">
                      <Input maxLength={64} placeholder="选填" />
                    </Form.Item>
                    <Button type="primary" onClick={() => void saveProfile()}>
                      保存资料
                    </Button>
                  </Form>
                </Card>

                <Card title="修改密码">
                  <Form form={pwdForm} layout="vertical">
                    <Form.Item
                      name="currentPassword"
                      label="当前密码"
                      rules={[{ required: true, message: '请输入当前密码' }]}
                    >
                      <Input.Password />
                    </Form.Item>
                    <Form.Item
                      name="newPassword"
                      label="新密码"
                      rules={[
                        { required: true, message: '请输入新密码' },
                        { min: 6, message: '至少 6 位' },
                      ]}
                    >
                      <Input.Password />
                    </Form.Item>
                    <Button type="primary" onClick={() => void changePassword()}>
                      修改密码
                    </Button>
                  </Form>
                </Card>

                <Card title="更换手机号">
                  <Form form={mobileForm} layout="vertical">
                    <Form.Item
                      name="mobile"
                      label="新手机号"
                      rules={[{ required: true, pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }]}
                    >
                      <Input maxLength={11} />
                    </Form.Item>
                    <Form.Item
                      name="code"
                      label="验证码"
                      rules={[{ required: true, message: '请输入验证码' }]}
                    >
                      <Input
                        addonAfter={
                          <Button type="link" size="small" onClick={() => void sendCode()}>
                            获取验证码
                          </Button>
                        }
                      />
                    </Form.Item>
                    <Button type="primary" onClick={() => void changeMobile()}>
                      确认更换
                    </Button>
                  </Form>
                </Card>
                </Space>

                <aside className="acc-pf__side">
                  <Card title="账号概况" className="acc-side">
                    <ul className="acc-kv">
                      <li>
                        <span>登录账号</span>
                        <b>{user?.username ?? '-'}</b>
                      </li>
                      <li>
                        <span>身份</span>
                        <b>{roleLabel ?? (isMerchant(user) ? '店主' : '顾客')}</b>
                      </li>
                      <li>
                        <span>手机号</span>
                        <b>{user?.mobile ?? '未绑定'}</b>
                      </li>
                      <li>
                        <span>邮箱</span>
                        <b className={user?.email ? '' : 'is-muted'}>{user?.email || '未绑定'}</b>
                      </li>
                      <li>
                        <span>头像</span>
                        <b className={user?.avatarUrl ? '' : 'is-muted'}>
                          {user?.avatarUrl ? '已设置' : '未设置'}
                        </b>
                      </li>
                    </ul>
                  </Card>

                  <Card title="快捷入口" className="acc-side">
                    <div className="acc-links">
                      <Link to="/orders">我的订单</Link>
                      <button type="button" onClick={() => setSearchParams({ tab: 'address' })}>
                        收货地址
                      </button>
                      <button type="button" onClick={() => setSearchParams({ tab: 'coupons' })}>
                        我的优惠券
                      </button>
                      <button type="button" onClick={() => setSearchParams({ tab: 'refunds' })}>
                        退款 / 售后
                      </button>
                    </div>
                  </Card>
                </aside>
              </div>
            ),
          },
          {
            key: 'address',
            label: '收货地址',
            children: (
              <Card
                title={`共 ${addresses.length} 个地址`}
                extra={
                  <Button
                    type="primary"
                    onClick={() => void openAddress()}
                  >
                    新增地址
                  </Button>
                }
              >
                {addresses.length === 0 ? (
                  <Empty description="还没有收货地址" />
                ) : (
                  addresses.map((a) => (
                    <div key={a.id}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
                        <div>
                          <Space size={8}>
                            <strong>{a.receiver}</strong>
                            <span>{a.mobile}</span>
                            {a.isDefault === 1 && <Tag color="blue">默认</Tag>}
                          </Space>
                          <div className="mall-card__meta">{a.fullAddress}</div>
                        </div>
                        <Space>
                          <Button
                            size="small"
                            onClick={() => void openAddress(a)}
                          >
                            编辑
                          </Button>
                          {a.isDefault !== 1 && (
                            <Button
                              size="small"
                              onClick={async () => {
                                await addressApi.setDefault(String(a.id));
                                toast.success('已设为默认地址');
                                await loadAddresses();
                              }}
                            >
                              设为默认
                            </Button>
                          )}
                          <Popconfirm
                            title="删除这个地址？"
                            onConfirm={async () => {
                              await addressApi.remove(String(a.id));
                              toast.success('已删除');
                              await loadAddresses();
                            }}
                          >
                            <Button size="small" danger>
                              删除
                            </Button>
                          </Popconfirm>
                        </Space>
                      </div>
                      <Divider style={{ margin: '12px 0' }} />
                    </div>
                  ))
                )}
              </Card>
            ),
          },
          {
            key: 'coupons',
            label: '我的优惠券',
            children: (
              <Space direction="vertical" size={16} style={{ width: '100%' }}>
                <Card title="领券中心">
                  {center.length === 0 ? (
                    <Empty description="本店暂无可领优惠券" />
                  ) : (
                    <div className="mall-grid">
                      {center.map((c) => (
                        <div key={c.id} className="mall-coupon">
                          <div>
                            <div className="mall-coupon__amount">
                              <small>¥</small>
                              {money(c.reduceAmount).replace('¥', '')}
                            </div>
                            <div className="mall-card__meta">
                              满 {money(c.thresholdAmount)} 可用
                            </div>
                            <div>{c.name}</div>
                            <div className="mall-card__meta">
                              限领 {c.perUserLimit} 张 · 已领 {c.received}/{c.total}
                            </div>
                          </div>
                          <Button
                            type="primary"
                            size="small"
                            onClick={async () => {
                              try {
                                await couponApi.receive(String(c.id));
                                toast.success('领取成功');
                                await loadCoupons();
                              } catch {
                                /* 拦截器已提示 */
                              }
                            }}
                          >
                            领取
                          </Button>
                        </div>
                      ))}
                    </div>
                  )}
                </Card>

                <Card
                  title={`我的券（${coupons.length}）`}
                  extra={
                    <Select
                      size="small"
                      value={couponStatus}
                      onChange={setCouponStatus}
                      style={{ width: 116 }}
                      options={[
                        { value: '', label: '全部状态' },
                        { value: 'UNUSED', label: '未使用' },
                        { value: 'USED', label: '已使用' },
                        { value: 'EXPIRED', label: '已过期' },
                      ]}
                    />
                  }
                >
                  {coupons.length === 0 ? (
                    <Empty description={couponStatus ? '这个状态下还没有券' : '还没有领过券'} />
                  ) : (
                    <Table
                      rowKey="id"
                      size="small"
                      pagination={false}
                      dataSource={coupons}
                      columns={[
                        { title: '券名', dataIndex: 'name' },
                        {
                          title: '优惠',
                          render: (_, r) => `满 ${money(r.thresholdAmount)} 减 ${money(r.reduceAmount)}`,
                        },
                        {
                          title: '状态',
                          render: (_, r) => (
                            <Tag color={r.status === 'UNUSED' ? 'green' : 'default'}>{r.statusLabel}</Tag>
                          ),
                        },
                        { title: '有效期至', render: (_, r) => dt(r.endTime) },
                        {
                          title: '操作',
                          width: 90,
                          render: (_, r) =>
                            DELETABLE_COUPON_STATUS.includes(r.status) ? (
                              <Popconfirm
                                title="删除这张券？"
                                description="删掉后不再出现在「我的券」里，用不上的券可以清掉"
                                okText="删除"
                                okButtonProps={{ danger: true }}
                                onConfirm={async () => {
                                  await couponApi.remove(String(r.id));
                                  toast.success('券已删除');
                                  await loadCoupons();
                                }}
                              >
                                <Button size="small" type="text" danger>
                                  删除
                                </Button>
                              </Popconfirm>
                            ) : (
                              <span className="mall-card__meta">—</span>
                            ),
                        },
                      ]}
                    />
                  )}
                </Card>
              </Space>
            ),
          },
          {
            key: 'refunds',
            label: '退款/售后',
            children: (
              <Card title={`共 ${refunds.length} 条`}>
                {refunds.length === 0 ? (
                  <Empty description="没有售后记录" />
                ) : (
                  refunds.map((r) => (
                    <div key={r.id}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
                        <div>
                          <Space size={8}>
                            <span>{r.orderNo}</span>
                            <StatusTag tone={refundStatusTone(r.status)}>{r.statusLabel}</StatusTag>
                            <Tag>{r.typeLabel}</Tag>
                            <span className="mall-price">{money(r.refundAmount)}</span>
                          </Space>
                          <div className="mall-card__meta">
                            {r.shopName} · 原因：{r.reason} · 申请于 {dt(r.createTime)}
                          </div>
                          {r.refuseReason && (
                            <div className="mall-card__meta">拒绝理由：{r.refuseReason}</div>
                          )}
                          {r.returnAddress && (
                            <div className="mall-card__meta">退货地址：{r.returnAddress}</div>
                          )}
                          {r.returnShipNo && (
                            <div className="mall-card__meta">
                              已寄回：{r.returnCompany} {r.returnShipNo}
                            </div>
                          )}
                        </div>
                        {r.status === 'WAITING_RETURN' && (
                          <Button
                            size="small"
                            type="primary"
                            onClick={() => {
                              setShipTarget(r);
                              shipForm.resetFields();
                            }}
                          >
                            填写退货运单号
                          </Button>
                        )}
                      </div>
                      <Divider style={{ margin: '12px 0' }} />
                    </div>
                  ))
                )}
              </Card>
            ),
          },
        ]}
      />

      {/* 地址编辑 */}
      <Modal
        open={addressOpen}
        title={editing ? '编辑地址' : '新增地址'}
        onCancel={() => setAddressOpen(false)}
        onOk={() => void saveAddress()}
        okText="保存"
      >
        <Form form={addressForm} layout="vertical">
          <Form.Item name="receiver" label="收货人" rules={[{ required: true, message: '请输入收货人' }]}>
            <Input maxLength={20} />
          </Form.Item>
          <Form.Item
            name="mobile"
            label="手机号"
            rules={[{ required: true, pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }]}
          >
            <Input maxLength={11} />
          </Form.Item>
          <Form.Item
            name="region"
            label="所在地区"
            rules={[{ required: true, message: '请选择省 / 市 / 区' }]}
          >
            <Cascader
              options={regions}
              fieldNames={REGION_FIELD_NAMES}
              placeholder="请选择省 / 市 / 区"
              showSearch
              allowClear
            />
          </Form.Item>
          <Form.Item name="detail" label="详细地址" rules={[{ required: true, message: '请输入详细地址' }]}>
            <Input.TextArea rows={2} maxLength={120} />
          </Form.Item>
          <Form.Item name="isDefault" label="设为默认" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>

      {/* 退货寄回 */}
      <Modal
        open={Boolean(shipTarget)}
        title="填写退货运单号"
        onCancel={() => setShipTarget(null)}
        onOk={() => void submitReturnShip()}
        okText="提交"
      >
        <Form form={shipForm} layout="vertical">
          <Form.Item
            name="returnCompany"
            label="快递公司"
            rules={[{ required: true, message: '请输入快递公司' }]}
          >
            <Input maxLength={32} placeholder="顺丰 / 中通 / 圆通…" />
          </Form.Item>
          <Form.Item
            name="returnShipNo"
            label="运单号"
            rules={[
              { required: true, message: '请输入运单号' },
              { pattern: /^[A-Za-z0-9-]{6,40}$/, message: '6~40 位字母、数字或短横线' },
            ]}
          >
            <Input maxLength={40} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
