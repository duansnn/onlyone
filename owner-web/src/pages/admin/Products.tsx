import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Drawer,
  Form,
  Input,
  InputNumber,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
} from 'antd';
import {
  CheckCircleFilled,
  DeleteOutlined,
  EditOutlined,
  FileTextOutlined,
  PlusOutlined,
  PictureOutlined,
  TagsOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons';
import { categoryApi, productApi } from '@/api/admin';
import ImageUploader from '@/components/ImageUploader';
import SafeImage from '@/components/SafeImage';
import { useAuthStore } from '@/store/auth';
import { toast } from '@/utils/feedback';
import {
  dt,
  fenToYuanInput,
  money,
  plainText,
  productStatusTone,
  yuanToFen,
} from '@/utils/format';
import StatusTag from '@/components/StatusTag';
import type { Category, ProductAdmin } from '@/types/mall';

interface ProductForm {
  categoryId: string;
  name: string;
  sellingPoint?: string;
  detail?: string;
  status: string;
  sort?: number;
  images: string[];
  skus: {
    id?: string;
    specName: string;
    price?: number;
    listPrice?: number;
    stock?: number;
    skuCode?: string;
    weightGram?: number;
  }[];
}

function flattenLeaf(nodes: Category[]): { value: string; label: string }[] {
  const out: { value: string; label: string }[] = [];
  const walk = (list: Category[], prefix = '') => {
    list.forEach((n) => {
      const label = prefix ? `${prefix} / ${n.name}` : n.name;
      if (n.children?.length) walk(n.children, label);
      else out.push({ value: String(n.id), label });
    });
  };
  walk(nodes);
  return out;
}

export default function Products() {
  const shopId = useAuthStore((state) => state.currentShopId) ?? '';
  const [rows, setRows] = useState<ProductAdmin[]>([]);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState('');
  const [categoryOptions, setCategoryOptions] = useState<{ value: string; label: string }[]>([]);

  const [open, setOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [version, setVersion] = useState<number | undefined>(undefined);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm<ProductForm>();

  // 右侧预览卡片要跟着表单实时变，这几个值单独盯着
  const watchName = Form.useWatch<string>('name', form);
  const watchPoint = Form.useWatch<string>('sellingPoint', form);
  const watchImages = Form.useWatch<string[]>('images', form);
  const watchStatus = Form.useWatch<string>('status', form);
  const watchCategory = Form.useWatch<string>('categoryId', form);
  const watchSkus = Form.useWatch<ProductForm['skus']>('skus', form);

  const preview = useMemo(() => {
    const list = watchSkus ?? [];
    const prices = list
      .map((sku) => Number(sku?.price) || 0)
      .filter((price) => price > 0);
    const stock = list.reduce((sum, sku) => sum + (Number(sku?.stock) || 0), 0);
    const images = (watchImages ?? []).filter((url) => url && url.trim());
    return {
      cover: images[0] ?? '',
      priceText: prices.length ? `¥${Math.min(...prices).toFixed(2)}` : '¥ 定价',
      skuCount: list.length,
      stock,
      imageCount: images.length,
      checks: [
        { label: '已选择类目', done: Boolean(watchCategory) },
        { label: '已填写商品名称', done: Boolean(watchName?.trim()) },
        { label: '已上传至少 1 张图片', done: images.length > 0 },
        { label: '至少 1 个规格填了售价', done: prices.length > 0 },
        { label: '可售库存大于 0', done: stock > 0 },
      ],
    };
  }, [watchSkus, watchImages, watchName, watchCategory]);

  const load = useCallback(async () => {
    if (!shopId) return;
    setLoading(true);
    try {
      const page = await productApi.page(shopId, {
        keyword: keyword || undefined,
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
  }, [shopId, keyword, status, pageNum]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (!shopId) return;
    void categoryApi
      .tree(shopId)
      .then((tree) => setCategoryOptions(flattenLeaf(tree ?? [])))
      .catch(() => undefined);
  }, [shopId]);

  const openCreate = () => {
    setEditingId(null);
    setVersion(undefined);
    form.resetFields();
    form.setFieldsValue({
      status: 'DRAFT',
      sort: 0,
      images: [],
      skus: [{ specName: '默认规格' }],
    });
    setOpen(true);
  };

  const openEdit = async (record: ProductAdmin) => {
    try {
      const detail = await productApi.detail(shopId, String(record.id));
      setEditingId(String(detail.id));
      setVersion(detail.version);
      form.setFieldsValue({
        categoryId: String(detail.categoryId),
        name: detail.name,
        sellingPoint: detail.sellingPoint,
        detail: plainText(detail.detail),
        status: detail.status,
        sort: detail.sort,
        images: detail.images ?? [],
        skus: (detail.skus ?? []).map((s) => ({
          id: String(s.id),
          specName: s.specName,
          price: fenToYuanInput(s.price),
          listPrice: s.listPrice ? fenToYuanInput(s.listPrice) : undefined,
          stock: s.stock,
          skuCode: s.skuCode,
          weightGram: s.weightGram,
        })),
      });
      setOpen(true);
    } catch {
      /* 拦截器已提示 */
    }
  };

  const submit = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      const payload = {
        categoryId: values.categoryId,
        name: values.name,
        sellingPoint: values.sellingPoint,
        detail: plainText(values.detail),
        status: values.status,
        sort: values.sort ?? 0,
        images: (values.images ?? []).filter((i) => i && i.trim()),
        skus: (values.skus ?? []).map((s) => ({
          id: s.id,
          specName: s.specName,
          price: yuanToFen(s.price),
          listPrice: s.listPrice === undefined || s.listPrice === null ? undefined : yuanToFen(s.listPrice),
          stock: s.stock ?? 0,
          skuCode: s.skuCode,
          weightGram: s.weightGram,
        })),
        version,
      };
      if (editingId) {
        await productApi.update(shopId, editingId, payload);
        toast.success('商品已更新');
      } else {
        await productApi.create(shopId, payload);
        toast.success('商品已创建');
      }
      setOpen(false);
      await load();
    } catch {
      /* 拦截器已提示 */
    } finally {
      setSaving(false);
    }
  };

  const remove = async (record: ProductAdmin) => {
    try {
      await productApi.remove(shopId, String(record.id));
      toast.success('商品已删除，规格与图片一并移除');
      // 删掉本页最后一条时回退一页，别停在空白页
      if (rows.length === 1 && pageNum > 1) setPageNum(pageNum - 1);
      else await load();
    } catch {
      /* 拦截器已提示（例如还有未完结订单） */
    }
  };

  const toggleStatus = async (record: ProductAdmin, next: string, tip: string) => {
    try {
      await productApi.setStatus(shopId, String(record.id), next);
      toast.success(tip);
      await load();
    } catch {
      /* 拦截器已提示（例如缺图 / 无可售规格） */
    }
  };

  return (
    <div>
      <h2 className="mall-section-title">商品管理</h2>

      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input.Search
            allowClear
            placeholder="名称 / 卖点"
            style={{ width: 220 }}
            onSearch={(value) => {
              setKeyword(value);
              setPageNum(1);
            }}
          />
          <Select
            value={status}
            style={{ width: 130 }}
            onChange={(value) => {
              setStatus(value);
              setPageNum(1);
            }}
            options={[
              { value: '', label: '全部状态' },
              { value: 'DRAFT', label: '草稿' },
              { value: 'ON_SALE', label: '在售' },
              { value: 'OFF_SHELF', label: '下架' },
            ]}
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新建商品
          </Button>
        </Space>
      </Card>

      <Table
        rowKey="id"
        scroll={{ x: 'max-content' }}
        loading={loading}
        dataSource={rows}
        pagination={{ current: pageNum, pageSize: 10, total, showSizeChanger: false, onChange: setPageNum }}
        columns={[
          {
            title: '商品',
            render: (_, r) => (
              <Space>
                <SafeImage
                  src={r.coverImage}
                  alt=""
                  style={{ width: 44, height: 44, borderRadius: 8, objectFit: 'cover', background: '#f5f5f5' }}
                />
                <div>
                  <div>{r.name}</div>
                  <div className="mall-card__meta">
                    {r.categoryName ?? '-'} · {r.skuCount} 个规格
                  </div>
                </div>
              </Space>
            ),
          },
          {
            title: '价格',
            width: 140,
            render: (_, r) =>
              r.minPrice === r.maxPrice
                ? money(r.minPrice)
                : `${money(r.minPrice)} ~ ${money(r.maxPrice)}`,
          },
          {
            title: '库存',
            width: 130,
            render: (_, r) => `总 ${r.totalStock} / 可售 ${r.totalAvailable}`,
          },
          {
            title: '状态',
            width: 90,
            render: (_, r) => <StatusTag tone={productStatusTone(r.status)}>{r.statusLabel}</StatusTag>,
          },
          { title: '更新时间', width: 140, render: (_, r) => dt(r.updateTime) },
          {
            title: '操作',
            width: 300,
            render: (_, r) => (
              <Space>
                <Button size="small" onClick={() => void openEdit(r)}>
                  编辑
                </Button>
                {r.status === 'ON_SALE' ? (
                  <Popconfirm
                    title="下架后店面将不可见"
                    onConfirm={() => void toggleStatus(r, 'OFF_SHELF', '已下架')}
                  >
                    <Button size="small">下架</Button>
                  </Popconfirm>
                ) : (
                  <Tooltip title="需至少 1 张主图 + 1 个启用且有可售库存的规格">
                    <Button
                      size="small"
                      type="primary"
                      onClick={() => void toggleStatus(r, 'ON_SALE', '已上架')}
                    >
                      上架
                    </Button>
                  </Tooltip>
                )}
                <Popconfirm
                  title="删除这个商品？"
                  description="商品、规格、图片都会移除，店里不再显示；有未完结订单时删不掉"
                  okText="删除"
                  okButtonProps={{ danger: true }}
                  onConfirm={() => void remove(r)}
                >
                  <Button size="small" danger icon={<DeleteOutlined />}>
                    删除
                  </Button>
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />

      <Drawer
        size={1080}
        className="pf-drawer"
        destroyOnHidden
        open={open}
        onClose={() => setOpen(false)}
        title={
          <div className="pf-head">
            <span className="pf-head__badge">{editingId ? <EditOutlined /> : <PlusOutlined />}</span>
            <span>
              <span className="pf-head__title">{editingId ? '编辑商品' : '新建商品'}</span>
              <span className="pf-head__sub">
                {editingId ? '改动保存后立即生效' : '先填信息与规格，存草稿也能随时回来继续'}
              </span>
            </span>
          </div>
        }
        footer={
          <div className="pf-foot">
            <span className="pf-foot__hint">
              <InfoCircleOutlined />
              {watchStatus === 'ON_SALE'
                ? '保存后商品会立即在店面可见'
                : '当前不会出现在店面，需要上架或改为「在售」'}
            </span>
            <Space>
              <Button onClick={() => setOpen(false)}>取消</Button>
              <Button type="primary" loading={saving} onClick={() => void submit()}>
                {editingId ? '保存修改' : '创建商品'}
              </Button>
            </Space>
          </div>
        }
      >
        <Form form={form} layout="vertical" className="pf-form">
          <div className="pf-body">
            <div className="pf-main">
              {/* ---------- 01 基本信息 ---------- */}
              <section className="pf-block">
                <header className="pf-block__head">
                  <span className="pf-block__num">01</span>
                  <div>
                    <h3 className="pf-block__title">
                      <FileTextOutlined /> 基本信息
                    </h3>
                    <p className="pf-block__desc">顾客最先看到的部分，名称和卖点尽量写清楚「是什么、解决什么」</p>
                  </div>
                </header>

                <div className="pf-grid2">
                  <Form.Item
                    name="categoryId"
                    label="所属类目"
                    rules={[{ required: true, message: '请选择类目' }]}
                  >
                    <Select
                      showSearch
                      options={categoryOptions}
                      placeholder={categoryOptions.length ? '选择二级类目' : '请先到「店铺与分类」建类目'}
                    />
                  </Form.Item>
                  <Form.Item name="name" label="商品名称" rules={[{ required: true, message: '请输入名称' }]}>
                    <Input maxLength={60} showCount placeholder="例如 抽取式厨房纸巾 3 卷" />
                  </Form.Item>
                </div>

                <Form.Item name="sellingPoint" label="卖点" className="pf-span2">
                  <Input maxLength={60} showCount placeholder="一句话说明，例如「三层加厚 不掉屑」" />
                </Form.Item>

                <div className="pf-grid2">
                  <Form.Item name="status" label="状态">
                    <Select
                      options={[
                        { value: 'DRAFT', label: '草稿（店面不可见）' },
                        { value: 'ON_SALE', label: '在售（店面可见）' },
                        { value: 'OFF_SHELF', label: '下架' },
                      ]}
                    />
                  </Form.Item>
                  <Form.Item name="sort" label="排序" extra="数字越小越靠前，0 为默认">
                    <InputNumber min={0} max={9999} style={{ width: '100%' }} />
                  </Form.Item>
                </div>
              </section>

              {/* ---------- 02 商品图片 ---------- */}
              <section className="pf-block">
                <header className="pf-block__head">
                  <span className="pf-block__num">02</span>
                  <div>
                    <h3 className="pf-block__title">
                      <PictureOutlined /> 商品图片
                    </h3>
                    <p className="pf-block__desc">第一张作为主图用在列表、购物车和订单里，建议 1:1 白底或场景图</p>
                  </div>
                  <span className="pf-block__count">{preview.imageCount} / 8</span>
                </header>
                <Form.Item
                  name="images"
                  rules={[{ required: true, message: '请至少上传 1 张商品图' }]}
                  style={{ marginBottom: 0 }}
                >
                  <ImageUploader folder="product" max={8} tip="单张不超过 5 MB，支持 jpg / png / webp" />
                </Form.Item>
              </section>

              {/* ---------- 03 图文详情 ---------- */}
              <section className="pf-block">
                <header className="pf-block__head">
                  <span className="pf-block__num">03</span>
                  <div>
                    <h3 className="pf-block__title">
                      <FileTextOutlined /> 图文详情
                    </h3>
                    <p className="pf-block__desc">
                      纯文本，换行会被保留；不用写 HTML 标签，写成 <code>&lt;p&gt;文字&lt;/p&gt;</code> 会原样显示
                    </p>
                  </div>
                </header>
                <Form.Item name="detail" style={{ marginBottom: 0 }}>
                  <Input.TextArea rows={6} maxLength={2000} showCount placeholder="例如：材质、尺寸、适用场景、注意事项……" />
                </Form.Item>
              </section>

              {/* ---------- 04 规格与库存 ---------- */}
              <section className="pf-block">
                <header className="pf-block__head">
                  <span className="pf-block__num">04</span>
                  <div>
                    <h3 className="pf-block__title">
                      <TagsOutlined /> 规格与库存
                    </h3>
                    <p className="pf-block__desc">一种商品多个规格时分别定价与库存；只有「在售且可售库存 &gt; 0」才卖得动</p>
                  </div>
                  <span className="pf-block__count">{preview.skuCount} 个规格</span>
                </header>

                <Form.List name="skus">
                  {(fields, { add, remove }) => (
                    <div className="pf-skus">
                      {fields.map((field, index) => (
                        <div className="pf-sku" key={field.key}>
                          <div className="pf-sku__head">
                            <span className="pf-sku__index">规格 {index + 1}</span>
                            <Button
                              type="text"
                              size="small"
                              danger
                              icon={<DeleteOutlined />}
                              disabled={fields.length <= 1}
                              onClick={() => remove(field.name)}
                            >
                              删除
                            </Button>
                          </div>
                          <Form.Item name={[field.name, 'id']} hidden>
                            <Input />
                          </Form.Item>
                          <div className="pf-sku__grid">
                            <Form.Item
                              name={[field.name, 'specName']}
                              label="规格名"
                              rules={[{ required: true, message: '必填' }]}
                            >
                              <Input placeholder="例如 500g/卷" />
                            </Form.Item>
                            <Form.Item
                              name={[field.name, 'price']}
                              label="售价（元）"
                              rules={[{ required: true, message: '必填' }]}
                            >
                              <InputNumber min={0} precision={2} style={{ width: '100%' }} placeholder="0.00" />
                            </Form.Item>
                            <Form.Item name={[field.name, 'listPrice']} label="划线价（元）">
                              <InputNumber min={0} precision={2} style={{ width: '100%' }} placeholder="选填" />
                            </Form.Item>
                            <Form.Item name={[field.name, 'stock']} label="库存">
                              <InputNumber min={0} style={{ width: '100%' }} placeholder="0" />
                            </Form.Item>
                            <Form.Item name={[field.name, 'skuCode']} label="货号">
                              <Input placeholder="选填，店内唯一" />
                            </Form.Item>
                            <Form.Item name={[field.name, 'weightGram']} label="重量（g）">
                              <InputNumber min={0} style={{ width: '100%' }} placeholder="选填" />
                            </Form.Item>
                          </div>
                        </div>
                      ))}
                      <Button
                        type="dashed"
                        block
                        icon={<PlusOutlined />}
                        className="pf-add-sku"
                        onClick={() => add({ specName: '' })}
                      >
                        添加规格
                      </Button>
                    </div>
                  )}
                </Form.List>
              </section>
            </div>

            {/* ---------------------------- 右侧：预览 + 检查 ---------------------------- */}
            <aside className="pf-side">
              <div className="pf-side__label">店面预览</div>
              <div className="pf-preview">
                <div className={`pf-preview__media${preview.cover ? '' : ' is-empty'}`}>
                  {preview.cover ? (
                    <SafeImage src={preview.cover} alt="" />
                  ) : (
                    <span className="pf-preview__empty">
                      <PictureOutlined />
                      主图待上传
                    </span>
                  )}
                  {watchStatus === 'ON_SALE' && <em className="pf-preview__flag">在售</em>}
                </div>
                <div className="pf-preview__body">
                  <div className="pf-preview__name">{watchName?.trim() || '商品名称'}</div>
                  <div className="pf-preview__point">
                    {watchPoint?.trim() || '一句话卖点会显示在这里'}
                  </div>
                  <div className="pf-preview__row">
                    <span className="pf-preview__price">{preview.priceText}</span>
                    <span className="pf-preview__meta">可售 {preview.stock}</span>
                  </div>
                </div>
              </div>

              <div className="pf-checks">
                <div className="pf-checks__title">上架检查</div>
                <ul>
                  {preview.checks.map((item) => (
                    <li key={item.label} className={item.done ? 'is-done' : ''}>
                      <CheckCircleFilled />
                      {item.label}
                    </li>
                  ))}
                </ul>
                <p className="pf-checks__tip">五项齐全后把状态改为「在售」即可上架</p>
              </div>
            </aside>
          </div>
        </Form>
      </Drawer>
    </div>
  );
}
