import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Card,
  Drawer,
  Empty,
  Form,
  Input,
  List,
  Modal,
  Popconfirm,
  Select,
  Space,
  Spin,
  Tag,
  Typography,
} from 'antd';
import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import { kanbanArticleApi } from '@/api/kanban/modules';
import type { Article } from '@/types/kanban';
import { toast } from '@/utils/feedback';

const { Title, Text, Paragraph } = Typography;

export default function KanbanBlog() {
  const [articles, setArticles] = useState<Article[]>([]);
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState<string | undefined>();
  const [keyword, setKeyword] = useState('');

  const [editorOpen, setEditorOpen] = useState(false);
  const [editing, setEditing] = useState<Article | null>(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm<{ title: string; category?: string; tags?: string; content: string }>();

  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState<Article | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setArticles(await kanbanArticleApi.list({ status, keyword: keyword || undefined }));
    } catch {
      // 已提示
    } finally {
      setLoading(false);
    }
  }, [status, keyword]);

  useEffect(() => {
    void load();
  }, [load]);

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    setEditorOpen(true);
  };

  const openEdit = (a: Article) => {
    setEditing(a);
    form.setFieldsValue({ title: a.title, category: a.category, tags: a.tags, content: a.content });
    setEditorOpen(true);
  };

  const handleSave = async (publish: boolean) => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      const id = await kanbanArticleApi.save({
        id: editing?.id,
        ...values,
        status: publish ? 'published' : 'draft',
      });
      toast.success(publish ? '已发布' : '已保存草稿');
      setEditorOpen(false);
      void id;
      await load();
    } catch {
      // 已提示
    } finally {
      setSaving(false);
    }
  };

  const openDetail = async (a: Article) => {
    setDetailOpen(true);
    setDetail(a);
    try {
      setDetail(await kanbanArticleApi.detail(a.id));
    } catch {
      // 已提示
    }
  };

  return (
    <div className="kb-page">
      <div className="kb-page__head">
        <div>
          <Title level={2} style={{ margin: 0 }}>博客</Title>
          <Text type="secondary">记录思考，输出内容</Text>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          写文章
        </Button>
      </div>

      <div className="kb-filterbar">
        <Input
          prefix={<SearchOutlined />}
          placeholder="搜索标题"
          allowClear
          style={{ width: 240 }}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
        <Select
          placeholder="状态"
          allowClear
          style={{ width: 120 }}
          value={status}
          onChange={setStatus}
          options={[
            { value: 'published', label: '已发布' },
            { value: 'draft', label: '草稿' },
          ]}
        />
      </div>

      {loading ? (
        <div className="kb-loading"><Spin /></div>
      ) : articles.length === 0 ? (
        <Card className="kb-card"><Empty description="还没有文章，点右上角写一篇" style={{ padding: '48px 0' }} /></Card>
      ) : (
        <List
          itemLayout="vertical"
          dataSource={articles}
          renderItem={(a) => (
            <List.Item
              style={{ background: '#fff', borderRadius: 16, padding: 20, marginBottom: 12, boxShadow: '0 6px 18px rgba(70,50,40,0.06)' }}
              actions={[
                <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEdit(a)}>编辑</Button>,
                <Popconfirm
                  key="del"
                  title="删除这篇文章？"
                  okText="删除"
                  cancelText="取消"
                  okButtonProps={{ danger: true }}
                  onConfirm={async () => {
                    await kanbanArticleApi.remove(a.id);
                    toast.success('已删除');
                    await load();
                  }}
                >
                  <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
                </Popconfirm>,
              ]}
            >
              <List.Item.Meta
                title={
                  <Space>
                    <a onClick={() => void openDetail(a)}>{a.title}</a>
                    {a.status === 'draft' ? <Tag>草稿</Tag> : <Tag color="green">已发布</Tag>}
                  </Space>
                }
                description={
                  <Space wrap>
                    {a.category && <Tag className="owner-chip">{a.category}</Tag>}
                    {(a.tags ?? '').split(',').filter(Boolean).map((t) => <Tag key={t}>{t}</Tag>)}
                    <Text type="secondary" style={{ fontSize: 12 }}>{a.updateTime ?? a.createTime}</Text>
                  </Space>
                }
              />
              <Paragraph type="secondary" ellipsis={{ rows: 2 }} style={{ marginBottom: 0 }}>
                {a.summary || a.content}
              </Paragraph>
            </List.Item>
          )}
        />
      )}

      <Modal
        open={editorOpen}
        title={editing ? '编辑文章' : '写文章'}
        okText="发布"
        cancelText="取消"
        confirmLoading={saving}
        onOk={() => void handleSave(true)}
        onCancel={() => setEditorOpen(false)}
        width={720}
        footer={[
          <Button key="draft" loading={saving} onClick={() => void handleSave(false)}>存草稿</Button>,
          <Button key="cancel" onClick={() => setEditorOpen(false)}>取消</Button>,
          <Button key="pub" type="primary" loading={saving} onClick={() => void handleSave(true)}>发布</Button>,
        ]}
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}>
            <Input placeholder="文章标题" maxLength={200} />
          </Form.Item>
          <Space style={{ display: 'flex' }} align="start">
            <Form.Item name="category" label="分类" style={{ width: 200 }}>
              <Input placeholder="如 技术 / 随笔" maxLength={50} />
            </Form.Item>
            <Form.Item name="tags" label="标签" style={{ flex: 1 }}>
              <Input placeholder="逗号分隔，如 React,后端" maxLength={200} />
            </Form.Item>
          </Space>
          <Form.Item name="content" label="正文" rules={[{ required: true, message: '请输入正文' }]}>
            <Input.TextArea rows={14} placeholder="支持 Markdown 语法" />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        width={560}
        title={detail?.title ?? '文章详情'}
      >
        <Space wrap style={{ marginBottom: 12 }}>
          {detail?.category && <Tag className="owner-chip">{detail.category}</Tag>}
          {(detail?.tags ?? '').split(',').filter(Boolean).map((t) => <Tag key={t}>{t}</Tag>)}
        </Space>
        <Paragraph style={{ whiteSpace: 'pre-wrap' }}>{detail?.content}</Paragraph>
      </Drawer>
    </div>
  );
}
