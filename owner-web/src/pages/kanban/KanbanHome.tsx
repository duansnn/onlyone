import { useCallback, useEffect, useState } from 'react';
import {
  App as AntdApp,
  Button,
  Card,
  Col,
  Empty,
  Form,
  Input,
  Modal,
  Row,
  Space,
  Spin,
  Typography,
} from 'antd';
import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ProjectOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { kanbanProjectApi } from '@/api/kanban';
import type { KanbanProject, ProjectSavePayload } from '@/types/kanban';
import { toast } from '@/utils/feedback';

const { Title, Text, Paragraph } = Typography;

interface ProjectFormValues {
  name: string;
  description?: string;
}

/** 看板项目列表首页（含一键演示数据） */
export default function KanbanHome() {
  const navigate = useNavigate();
  const { modal } = AntdApp.useApp();
  const [form] = Form.useForm<ProjectFormValues>();

  const [projects, setProjects] = useState<KanbanProject[]>([]);
  const [loading, setLoading] = useState(true);

  const [editorOpen, setEditorOpen] = useState(false);
  const [editing, setEditing] = useState<KanbanProject | null>(null);
  const [saving, setSaving] = useState(false);
  const [seeding, setSeeding] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const list = await kanbanProjectApi.list();
      setProjects(list);
    } catch {
      // 错误已由拦截器提示
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    setEditorOpen(true);
  };

  const openEdit = (project: KanbanProject) => {
    setEditing(project);
    form.setFieldsValue({ name: project.name, description: project.description });
    setEditorOpen(true);
  };

  const handleSave = async () => {
    const values = await form.validateFields();
    const payload: ProjectSavePayload = {
      name: values.name,
      description: values.description,
    };
    setSaving(true);
    try {
      if (editing) {
        await kanbanProjectApi.update(editing.id, payload);
        toast.success('项目已更新');
      } else {
        await kanbanProjectApi.create(payload);
        toast.success('项目已创建');
      }
      setEditorOpen(false);
      await load();
    } catch {
      // 错误已提示
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = (project: KanbanProject) => {
    modal.confirm({
      title: `删除项目「${project.name}」？`,
      content: '删除后该项目下的全部任务将一并删除，此操作不可恢复。',
      okText: '删除',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: async () => {
        try {
          await kanbanProjectApi.remove(project.id);
          toast.success('项目已删除');
          await load();
        } catch {
          // 错误已提示
        }
      },
    });
  };

  const handleSeed = async () => {
    setSeeding(true);
    try {
      const count = await kanbanProjectApi.seed();
      toast.success(`已加载 ${count} 个演示项目`);
      await load();
    } catch {
      // 错误已提示
    } finally {
      setSeeding(false);
    }
  };

  return (
    <div className="kb-page">
      <div className="kb-page__head">
        <div>
          <Title level={2} style={{ margin: 0 }}>
            我的项目
          </Title>
          <Text type="secondary">点击项目卡片进入看板，管理你的任务</Text>
        </div>
        <Space>
          <Button loading={seeding} onClick={() => void handleSeed()}>
            一键加载演示数据
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新建项目
          </Button>
        </Space>
      </div>

      {loading ? (
        <div className="kb-loading">
          <Spin />
        </div>
      ) : projects.length === 0 ? (
        <Card className="kb-card">
          <Empty description="还没有项目。可以「新建项目」，或点击右上角「一键加载演示数据」快速体验" style={{ padding: '48px 0' }} />
        </Card>
      ) : (
        <Row gutter={[16, 16]}>
          {projects.map((project) => (
            <Col key={project.id} xs={24} sm={12} lg={8} xl={6}>
              <Card
                className="kb-project-card"
                hoverable
                onClick={() => navigate(`/kanban/projects/${project.id}/board`)}
                actions={[
                  <EditOutlined
                    key="edit"
                    onClick={(e) => {
                      e.stopPropagation();
                      openEdit(project);
                    }}
                  />,
                  <DeleteOutlined
                    key="delete"
                    style={{ color: '#c45c5c' }}
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDelete(project);
                    }}
                  />,
                ]}
              >
                <div className="kb-project-card__body">
                  <div className="kb-project-card__icon">
                    <ProjectOutlined />
                  </div>
                  <Title level={4} style={{ margin: '12px 0 6px' }}>
                    {project.name}
                  </Title>
                  <Paragraph
                    type="secondary"
                    ellipsis={{ rows: 2 }}
                    style={{ marginBottom: 16, minHeight: 44 }}
                  >
                    {project.description || '暂无描述'}
                  </Paragraph>
                  <Text type="secondary">{project.taskCount} 个任务</Text>
                </div>
              </Card>
            </Col>
          ))}
        </Row>
      )}

      <Modal
        title={editing ? '编辑项目' : '新建项目'}
        open={editorOpen}
        onOk={handleSave}
        onCancel={() => setEditorOpen(false)}
        okText="保存"
        cancelText="取消"
        confirmLoading={saving}
        destroyOnClose
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="name"
            label="项目名称"
            rules={[
              { required: true, message: '请输入项目名称' },
              { max: 100, message: '项目名称最长 100 字' },
            ]}
          >
            <Input placeholder="如：作品集网站开发" />
          </Form.Item>
          <Form.Item
            name="description"
            label="项目描述"
            rules={[{ max: 2000, message: '项目描述最长 2000 字' }]}
          >
            <Input.TextArea rows={4} placeholder="项目目标、范围等（可选）" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
