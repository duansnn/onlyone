import { useCallback, useEffect, useState } from 'react';
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
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { categoryApi, productApi } from '@/api/admin';
import { useAuthStore } from '@/store/auth';
import { toast } from '@/utils/feedback';
import { dt, fenToYuanInput, money, PLACEHOLDER, productStatusColor, yuanToFen } from '@/utils/format';
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
    price: number;
    listPrice?: number;
    stock: number;
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
      images: [''],
      skus: [{ specName: '默认规格', price: 0, stock: 0 }],
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
        detail: detail.detail,
        status: detail.status,
        sort: detail.sort,
        images: detail.images?.length ? detail.images : [''],
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
        detail: values.detail,
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
        loading={loading}
        dataSource={rows}
        pagination={{ current: pageNum, pageSize: 10, total, showSizeChanger: false, onChange: setPageNum }}
        columns={[
          {
            title: '商品',
            render: (_, r) => (
              <Space>
                <img
                  src={r.coverImage || PLACEHOLDER}
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
            render: (_, r) => <Tag color={productStatusColor(r.status)}>{r.statusLabel}</Tag>,
          },
          { title: '更新时间', width: 140, render: (_, r) => dt(r.updateTime) },
          {
            title: '操作',
            width: 220,
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
              </Space>
            ),
          },
        ]}
      />

      <Drawer
        width={680}
        open={open}
        title={editingId ? '编辑商品' : '新建商品'}
        onClose={() => setOpen(false)}
        extra={
          <Space>
            <Button onClick={() => setOpen(false)}>取消</Button>
            <Button type="primary" loading={saving} onClick={() => void submit()}>
              保存
            </Button>
          </Space>
        }
      >
        <Form form={form} layout="vertical">
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
            <Input maxLength={60} />
          </Form.Item>
          <Form.Item name="sellingPoint" label="卖点">
            <Input maxLength={60} placeholder="一句话说明，例如「三层加厚 不掉屑」" />
          </Form.Item>
          <Space size={12} style={{ display: 'flex' }}>
            <Form.Item name="status" label="状态" style={{ minWidth: 160 }}>
              <Select
                options={[
                  { value: 'DRAFT', label: '草稿' },
                  { value: 'ON_SALE', label: '在售' },
                  { value: 'OFF_SHELF', label: '下架' },
                ]}
              />
            </Form.Item>
            <Form.Item name="sort" label="排序（越小越靠前）" style={{ minWidth: 200 }}>
              <InputNumber min={0} max={9999} style={{ width: '100%' }} />
            </Form.Item>
          </Space>
          <Form.Item name="detail" label="图文详情">
            <Input.TextArea rows={4} maxLength={2000} />
          </Form.Item>

          <h3 style={{ fontSize: 14, fontWeight: 500, margin: '8px 0' }}>商品图片</h3>
          <Form.List name="images">
            {(fields, { add, remove }) => (
              <>
                {fields.map((field) => (
                  <Space key={field.key} style={{ display: 'flex', marginBottom: 8 }} align="start">
                    <Form.Item
                      name={field.name}
                      noStyle
                      rules={[{ required: true, message: '请填写图片地址' }]}
                    >
                      <Input placeholder="https://... 图片地址，第一张为主图" style={{ width: 480 }} />
                    </Form.Item>
                    <Button
                      icon={<DeleteOutlined />}
                      disabled={fields.length <= 1}
                      onClick={() => remove(field.name)}
                    />
                  </Space>
                ))}
                <Button icon={<PlusOutlined />} onClick={() => add('')} style={{ marginBottom: 16 }}>
                  添加图片
                </Button>
              </>
            )}
          </Form.List>

          <h3 style={{ fontSize: 14, fontWeight: 500, margin: '8px 0' }}>规格与库存</h3>
          <Form.List name="skus">
            {(fields, { add, remove }) => (
              <>
                {fields.map((field) => (
                  <Card key={field.key} size="small" style={{ marginBottom: 10 }}>
                    <Form.Item name={[field.name, 'id']} hidden>
                      <Input />
                    </Form.Item>
                    <Space wrap>
                      <Form.Item
                        name={[field.name, 'specName']}
                        label="规格名"
                        rules={[{ required: true, message: '必填' }]}
                        style={{ marginBottom: 8 }}
                      >
                        <Input placeholder="例如 500g/卷" style={{ width: 150 }} />
                      </Form.Item>
                      <Form.Item
                        name={[field.name, 'price']}
                        label="售价（元）"
                        rules={[{ required: true, message: '必填' }]}
                        style={{ marginBottom: 8 }}
                      >
                        <InputNumber min={0} precision={2} style={{ width: 120 }} />
                      </Form.Item>
                      <Form.Item name={[field.name, 'listPrice']} label="划线价（元）" style={{ marginBottom: 8 }}>
                        <InputNumber min={0} precision={2} style={{ width: 120 }} />
                      </Form.Item>
                      <Form.Item name={[field.name, 'stock']} label="库存" style={{ marginBottom: 8 }}>
                        <InputNumber min={0} style={{ width: 100 }} />
                      </Form.Item>
                      <Form.Item name={[field.name, 'skuCode']} label="货号" style={{ marginBottom: 8 }}>
                        <Input placeholder="选填，店内唯一" style={{ width: 130 }} />
                      </Form.Item>
                      <Form.Item name={[field.name, 'weightGram']} label="重量(g)" style={{ marginBottom: 8 }}>
                        <InputNumber min={0} style={{ width: 100 }} />
                      </Form.Item>
                      <Button
                        danger
                        icon={<DeleteOutlined />}
                        disabled={fields.length <= 1}
                        onClick={() => remove(field.name)}
                      />
                    </Space>
                  </Card>
                ))}
                <Button icon={<PlusOutlined />} onClick={() => add({ specName: '', price: 0, stock: 0 })}>
                  添加规格
                </Button>
              </>
            )}
          </Form.List>
        </Form>
      </Drawer>
    </div>
  );
}
