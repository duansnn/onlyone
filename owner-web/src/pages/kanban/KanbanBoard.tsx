import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { App as AntdApp, Button, Input, Select, Space, Spin, Tag, Typography } from 'antd';
import {
  ExperimentOutlined,
  PlusOutlined,
  ReloadOutlined,
  SwapOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router-dom';
import { kanbanProjectApi, kanbanTaskApi } from '@/api/kanban';
import type {
  KanbanProject,
  KanbanTask,
  TaskPriority,
  TaskSavePayload,
  TaskStatus,
} from '@/types/kanban';
import { toast } from '@/utils/feedback';
import TaskCard from './TaskCard';
import TaskEditor from './TaskEditor';
import TaskDetailDrawer from './TaskDetailDrawer';
import AiPanel, { type AiMode } from './AiPanel';
import ImportExportPanel from './ImportExportPanel';

const { Title, Text } = Typography;

const COLUMNS: { status: TaskStatus; title: string; color: string }[] = [
  { status: 'todo', title: '待办', color: '#e3c15a' },
  { status: 'doing', title: '进行中', color: '#b9a6e6' },
  { status: 'done', title: '已完成', color: '#a3be72' },
];

const STATUS_LABEL: Record<TaskStatus, string> = {
  todo: '待办',
  doing: '进行中',
  done: '已完成',
};

/** 任务看板核心页面 */
export default function KanbanBoard() {
  const { projectId } = useParams();
  const navigate = useNavigate();

  const [project, setProject] = useState<KanbanProject | null>(null);
  const [tasks, setTasks] = useState<KanbanTask[]>([]);
  const [loading, setLoading] = useState(true);

  // 筛选与搜索
  const [keyword, setKeyword] = useState('');
  const [priorityFilter, setPriorityFilter] = useState<TaskPriority | undefined>();
  const [tagFilter, setTagFilter] = useState('');
  const [overdueOnly, setOverdueOnly] = useState(false);

  // 任务编辑器
  const [editorOpen, setEditorOpen] = useState(false);
  const [editingTask, setEditingTask] = useState<KanbanTask | null>(null);
  const [defaultStatus, setDefaultStatus] = useState<TaskStatus>('todo');
  const [saving, setSaving] = useState(false);

  // AI 弹窗
  const [aiOpen, setAiOpen] = useState(false);
  const [aiMode, setAiMode] = useState<AiMode>('split');
  const [aiTask, setAiTask] = useState<KanbanTask | null>(null);

  // 导入导出
  const [ioOpen, setIoOpen] = useState(false);

  // 拖拽中的任务 id
  const [draggingId, setDraggingId] = useState<string | null>(null);
  const [dragOverStatus, setDragOverStatus] = useState<TaskStatus | null>(null);

  // 详情抽屉
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailTask, setDetailTask] = useState<KanbanTask | null>(null);

  const { notification } = AntdApp.useApp();
  const overdueNotifiedRef = useRef(false);

  const loadProject = useCallback(async () => {
    try {
      const list = await kanbanProjectApi.list();
      const current = list.find((p) => p.id === projectId) ?? null;
      setProject(current);
    } catch {
      // 错误已提示
    }
  }, [projectId]);

  const loadTasks = useCallback(async () => {
    if (!projectId) return;
    setLoading(true);
    try {
      const list = await kanbanTaskApi.list({
        projectId,
        keyword: keyword || undefined,
        priority: priorityFilter,
        tag: tagFilter || undefined,
        overdue: overdueOnly || undefined,
      });
      setTasks(list);
    } catch {
      // 错误已提示
    } finally {
      setLoading(false);
    }
  }, [projectId, keyword, priorityFilter, tagFilter, overdueOnly]);

  useEffect(() => {
    loadProject();
  }, [loadProject]);

  useEffect(() => {
    loadTasks();
  }, [loadTasks]);

  // 进入看板后若有逾期任务，弹一次提醒（同一项目只提醒一次）
  useEffect(() => {
    if (loading || overdueNotifiedRef.current) return;
    const overdueTasks = tasks.filter((t) => t.overdue);
    if (overdueTasks.length > 0) {
      overdueNotifiedRef.current = true;
      notification.warning({
        message: `有 ${overdueTasks.length} 个任务已逾期`,
        description: overdueTasks.slice(0, 3).map((t) => `· ${t.title}`).join('\n') + (overdueTasks.length > 3 ? '\n…' : ''),
        placement: 'topRight',
        duration: 5,
      });
    }
  }, [loading, tasks, notification]);

  // 详情抽屉里点「编辑任务」：关闭抽屉并打开编辑器
  const openDetail = (task: KanbanTask) => {
    setDetailTask(task);
    setDetailOpen(true);
  };

  const openEditFromDetail = (task: KanbanTask) => {
    setDetailOpen(false);
    openEdit(task);
  };

  const tasksByStatus = useMemo(() => {
    const map: Record<TaskStatus, KanbanTask[]> = { todo: [], doing: [], done: [] };
    for (const task of tasks) {
      if (map[task.status]) map[task.status].push(task);
    }
    return map;
  }, [tasks]);

  // —— 任务 CRUD ——
  const openCreate = (status: TaskStatus) => {
    setEditingTask(null);
    setDefaultStatus(status);
    setEditorOpen(true);
  };

  const openEdit = (task: KanbanTask) => {
    setEditingTask(task);
    setDefaultStatus(task.status);
    setEditorOpen(true);
  };

  const handleSubmitTask = async (payload: TaskSavePayload) => {
    if (!projectId) return;
    setSaving(true);
    try {
      if (editingTask) {
        await kanbanTaskApi.update(editingTask.id, payload);
        toast.success('任务已更新');
      } else {
        await kanbanTaskApi.create(projectId, payload);
        toast.success('任务已创建');
      }
      setEditorOpen(false);
      await loadTasks();
    } catch {
      // 错误已提示
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteTask = async (task: KanbanTask) => {
    try {
      await kanbanTaskApi.remove(task.id);
      toast.success('任务已删除');
      await loadTasks();
    } catch {
      // 错误已提示
    }
  };

  // —— 拖拽改状态 ——
  const handleDragStart = (task: KanbanTask) => {
    setDraggingId(task.id);
  };

  const handleDragOver = (status: TaskStatus) => {
    setDragOverStatus(status);
  };

  const handleDrop = async (status: TaskStatus) => {
    setDragOverStatus(null);
    if (draggingId === null) return;
    const task = tasks.find((t) => t.id === draggingId);
    setDraggingId(null);
    if (!task || task.status === status) return;

    // 乐观更新
    setTasks((prev) => prev.map((t) => (t.id === task.id ? { ...t, status } : t)));
    try {
      await kanbanTaskApi.update(task.id, { status });
    } catch {
      // 失败回滚
      await loadTasks();
    }
  };

  // —— AI ——
  const openAi = (mode: AiMode, task?: KanbanTask) => {
    setAiMode(mode);
    setAiTask(task ?? null);
    setAiOpen(true);
  };

  const handleTaskOptimized = async (taskId: string, description: string) => {
    try {
      await kanbanTaskApi.update(taskId, { description });
      toast.success('描述已更新');
      await loadTasks();
    } catch {
      // 错误已提示
    }
  };

  return (
    <div className="kb-page kb-board">
      <div className="kb-page__head">
        <div>
          <Button type="link" style={{ padding: 0 }} onClick={() => navigate('/kanban/projects')}>
            ← 返回项目列表
          </Button>
          <Title level={2} style={{ margin: '4px 0 0' }}>
            {project?.name ?? '看板'}
          </Title>
        </div>
        <Space wrap>
          <Button icon={<SwapOutlined />} onClick={() => setIoOpen(true)}>
            导入 / 导出
          </Button>
          <Button icon={<ThunderboltOutlined />} onClick={() => openAi('split')}>
            AI 拆解
          </Button>
          <Button icon={<ExperimentOutlined />} onClick={() => openAi('report')}>
            AI 周报
          </Button>
        </Space>
      </div>

      {/* 筛选工具栏 */}
      <div className="kb-filterbar">
        <Input.Search
          placeholder="搜索任务标题或描述"
          allowClear
          style={{ width: 240 }}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onSearch={() => loadTasks()}
        />
        <Select
          placeholder="优先级"
          allowClear
          style={{ width: 120 }}
          value={priorityFilter}
          onChange={(v) => setPriorityFilter(v)}
          options={[
            { value: 'high', label: '高' },
            { value: 'medium', label: '中' },
            { value: 'low', label: '低' },
          ]}
        />
        <Input
          placeholder="标签"
          allowClear
          style={{ width: 140 }}
          value={tagFilter}
          onChange={(e) => setTagFilter(e.target.value)}
        />
        <Button
          type={overdueOnly ? 'primary' : 'default'}
          onClick={() => setOverdueOnly((v) => !v)}
        >
          仅看逾期
        </Button>
        <Button icon={<ReloadOutlined />} onClick={() => loadTasks()} />
      </div>

      {/* 三列看板 */}
      {loading ? (
        <div className="kb-loading">
          <Spin />
        </div>
      ) : (
        <div className="kb-columns">
          {COLUMNS.map((col) => {
            const columnTasks = tasksByStatus[col.status];
            return (
              <div
                key={col.status}
                className={`kb-column${dragOverStatus === col.status ? ' is-drag-over' : ''}`}
                onDragOver={(e) => {
                  e.preventDefault();
                  handleDragOver(col.status);
                }}
                onDragLeave={() => setDragOverStatus(null)}
                onDrop={() => handleDrop(col.status)}
              >
                <div className="kb-column__head">
                  <span className="kb-column__dot" style={{ background: col.color }} />
                  <Text strong>{col.title}</Text>
                  <Tag className="kb-column__count">{columnTasks.length}</Tag>
                  <Button
                    type="text"
                    size="small"
                    icon={<PlusOutlined />}
                    onClick={() => openCreate(col.status)}
                  />
                </div>

                <div className="kb-column__body">
                  {columnTasks.length === 0 ? (
                    <div className="kb-column__empty">暂无任务</div>
                  ) : (
                    columnTasks.map((task) => (
                      <div
                        key={task.id}
                        draggable
                        onDragStart={() => handleDragStart(task)}
                        className={draggingId === task.id ? 'kb-task-wrap is-dragging' : 'kb-task-wrap'}
                      >
                        <TaskCard
                          task={task}
                          onEdit={openEdit}
                          onDelete={handleDeleteTask}
                          onOptimize={(t) => openAi('optimize', t)}
                          onOpen={openDetail}
                        />
                      </div>
                    ))
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* 任务详情抽屉（含评论） */}
      <TaskDetailDrawer
        open={detailOpen}
        task={detailTask}
        onClose={() => setDetailOpen(false)}
        onEdit={openEditFromDetail}
      />

      {/* 任务编辑器 */}
      <TaskEditor
        open={editorOpen}
        task={editingTask}
        defaultStatus={defaultStatus}
        saving={saving}
        onCancel={() => setEditorOpen(false)}
        onSubmit={handleSubmitTask}
      />

      {/* AI 弹窗 */}
      <AiPanel
        open={aiOpen}
        mode={aiMode}
        projectId={projectId ?? ''}
        task={aiTask}
        onClose={() => setAiOpen(false)}
        onTasksCreated={() => loadTasks()}
        onTaskOptimized={handleTaskOptimized}
      />

      {/* 导入导出 */}
      <ImportExportPanel
        open={ioOpen}
        projectId={projectId ?? ''}
        projectName={project?.name}
        onClose={() => setIoOpen(false)}
        onImported={() => loadTasks()}
      />
    </div>
  );
}
