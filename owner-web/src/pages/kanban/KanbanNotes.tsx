import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Card,
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
  PushpinFilled,
  PushpinOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import { kanbanNoteApi } from '@/api/kanban/modules';
import type { KanbanNote } from '@/types/kanban';
import { toast } from '@/utils/feedback';

const { Title, Text, Paragraph } = Typography;

export default function KanbanNotes() {
  const [notes, setNotes] = useState<KanbanNote[]>([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [category, setCategory] = useState<string | undefined>();

  const [editorOpen, setEditorOpen] = useState(false);
  const [editing, setEditing] = useState<KanbanNote | null>(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm<{ title: string; category?: string; content: string }>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setNotes(await kanbanNoteApi.list(keyword || undefined, category));
    } catch {
      // 已提示
    } finally {
      setLoading(false);
    }
  }, [keyword, category]);

  useEffect(() => {
    void load();
  }, [load]);

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    setEditorOpen(true);
  };

  const openEdit = (note: KanbanNote) => {
    setEditing(note);
    form.setFieldsValue({ title: note.title, category: note.category, content: note.content });
    setEditorOpen(true);
  };

  const handleSave = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      if (editing) {
        await kanbanNoteApi.update(editing.id, values);
        toast.success('笔记已更新');
      } else {
        await kanbanNoteApi.create({ ...values, category: values.category });
        toast.success('笔记已创建');
      }
      setEditorOpen(false);
      await load();
    } catch {
      // 已提示
    } finally {
      setSaving(false);
    }
  };

  const handleTogglePin = async (note: KanbanNote) => {
    try {
      await kanbanNoteApi.update(note.id, { pinned: note.pinned ? 0 : 1 });
      await load();
    } catch {
      // 已提示
    }
  };

  const categories = Array.from(new Set(notes.map((n) => n.category).filter(Boolean))) as string[];

  return (
    <div className="kb-page">
      <div className="kb-page__head">
        <div>
          <Title level={2} style={{ margin: 0 }}>知识库</Title>
          <Text type="secondary">记录灵感、学习笔记与工作沉淀</Text>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          新建笔记
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
          placeholder="分类"
          allowClear
          style={{ width: 140 }}
          value={category}
          onChange={setCategory}
          options={categories.map((c) => ({ value: c, label: c }))}
        />
      </div>

      {loading ? (
        <div className="kb-loading"><Spin /></div>
      ) : notes.length === 0 ? (
        <Card className="kb-card"><Empty description="还没有笔记，点右上角新建" style={{ padding: '48px 0' }} /></Card>
      ) : (
        <List
          grid={{ gutter: 16, xs: 1, sm: 2, lg: 3 }}
          dataSource={notes}
          renderItem={(note) => (
            <List.Item>
              <Card
                className="kb-project-card"
                actions={[
                  <span
                    key="pin"
                    onClick={() => void handleTogglePin(note)}
                    title={note.pinned ? '取消置顶' : '置顶'}
                  >
                    {note.pinned ? <PushpinFilled style={{ color: '#ec6541' }} /> : <PushpinOutlined />}
                  </span>,
                  <EditOutlined key="edit" onClick={() => openEdit(note)} />,
                  <Popconfirm
                    key="del"
                    title="删除这篇笔记？"
                    okText="删除"
                    cancelText="取消"
                    okButtonProps={{ danger: true }}
                    onConfirm={async () => {
                      await kanbanNoteApi.remove(note.id);
                      toast.success('已删除');
                      await load();
                    }}
                  >
                    <DeleteOutlined style={{ color: '#d64545' }} />
                  </Popconfirm>,
                ]}
              >
                <Space direction="vertical" size={6} style={{ width: '100%' }}>
                  <Space>
                    <Title level={5} style={{ margin: 0 }}>{note.title}</Title>
                    {note.pinned ? <Tag color="orange">置顶</Tag> : null}
                  </Space>
                  {note.category && <Tag className="owner-chip">{note.category}</Tag>}
                  <Paragraph type="secondary" ellipsis={{ rows: 3 }} style={{ marginBottom: 4 }}>
                    {note.content || '（无正文）'}
                  </Paragraph>
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    更新于 {note.updateTime ?? note.createTime ?? '-'}
                  </Text>
                </Space>
              </Card>
            </List.Item>
          )}
        />
      )}

      <Modal
        open={editorOpen}
        title={editing ? '编辑笔记' : '新建笔记'}
        okText="保存"
        cancelText="取消"
        confirmLoading={saving}
        onOk={() => void handleSave()}
        onCancel={() => setEditorOpen(false)}
        destroyOnClose
        width={640}
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}>
            <Input placeholder="笔记标题" maxLength={200} />
          </Form.Item>
          <Form.Item name="category" label="分类">
            <Input placeholder="如 学习 / 工作 / 生活（可选）" maxLength={50} />
          </Form.Item>
          <Form.Item name="content" label="正文">
            <Input.TextArea rows={10} placeholder="支持 Markdown 语法" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
