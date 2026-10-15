import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Drawer,
  Empty,
  Input,
  Popconfirm,
  Space,
  Spin,
  Tag,
  Typography,
} from 'antd';
import { DeleteOutlined, SendOutlined } from '@ant-design/icons';
import { kanbanTaskApi } from '@/api/kanban';
import type { KanbanComment, KanbanTask, TaskPriority, TaskStatus } from '@/types/kanban';
import { toast } from '@/utils/feedback';

const { Title, Text, Paragraph } = Typography;

const PRIORITY_META: Record<TaskPriority, { label: string; color: string }> = {
  high: { label: '高', color: 'red' },
  medium: { label: '中', color: 'gold' },
  low: { label: '低', color: 'blue' },
};

const STATUS_META: Record<TaskStatus, { label: string; color: string }> = {
  todo: { label: '待办', color: 'default' },
  doing: { label: '进行中', color: 'purple' },
  done: { label: '已完成', color: 'green' },
};

interface TaskDetailDrawerProps {
  open: boolean;
  task: KanbanTask | null;
  onClose: () => void;
  /** 编辑入口（沿用现有编辑弹窗） */
  onEdit?: (task: KanbanTask) => void;
}

/** 任务详情抽屉：完整描述 + 评论列表 + 发表评论 */
export default function TaskDetailDrawer({ open, task, onClose, onEdit }: TaskDetailDrawerProps) {
  const [comments, setComments] = useState<KanbanComment[]>([]);
  const [loading, setLoading] = useState(false);
  const [content, setContent] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const loadComments = useCallback(async (taskId: string) => {
    setLoading(true);
    try {
      setComments(await kanbanTaskApi.listComments(taskId));
    } catch {
      // 错误已提示
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (open && task) {
      setContent('');
      void loadComments(task.id);
    } else {
      setComments([]);
      setContent('');
    }
  }, [open, task, loadComments]);

  const handleAdd = async () => {
    if (!task || !content.trim()) return;
    setSubmitting(true);
    try {
      await kanbanTaskApi.addComment(task.id, content);
      setContent('');
      await loadComments(task.id);
    } catch {
      // 错误已提示
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (comment: KanbanComment) => {
    if (!task) return;
    try {
      await kanbanTaskApi.removeComment(comment.id);
      toast.success('评论已删除');
      await loadComments(task.id);
    } catch {
      // 错误已提示
    }
  };

  const priority = task?.priority && PRIORITY_META[task.priority as TaskPriority];
  const status = task?.status ? STATUS_META[task.status as TaskStatus] : undefined;

  return (
    <Drawer
      open={open}
      onClose={onClose}
      width={480}
      title={task ? `任务详情：${task.title}` : '任务详情'}
      destroyOnClose
      extra={
        task && onEdit ? (
          <Button size="small" onClick={() => onEdit(task)}>
            编辑任务
          </Button>
        ) : undefined
      }
    >
      {!task ? null : (
        <>
          <Space size={8} wrap style={{ marginBottom: 12 }}>
            {priority && <Tag color={priority.color}>优先级：{priority.label}</Tag>}
            {status && <Tag color={status.color}>{status.label}</Tag>}
            {task.overdue && <Tag color="volcano">已逾期</Tag>}
            {task.dueDate && <Text type="secondary">截止 {task.dueDate}</Text>}
          </Space>

          {(task.tags ?? '').trim() && (
            <div style={{ marginBottom: 12 }}>
              {task.tags!.split(',').map((tag) =>
                tag.trim() ? (
                  <Tag key={tag} className="owner-chip">
                    {tag.trim()}
                  </Tag>
                ) : null,
              )}
            </div>
          )}

          <Title level={5} style={{ marginTop: 4 }}>
            描述
          </Title>
          <Paragraph
            type="secondary"
            style={{ whiteSpace: 'pre-wrap', background: 'rgba(0,0,0,0.03)', padding: 12, borderRadius: 8 }}
          >
            {task.description?.trim() || '（暂无描述）'}
          </Paragraph>

          <Title level={5}>评论（{comments.length}）</Title>
          <div className="kb-comments">
            {loading ? (
              <div style={{ textAlign: 'center', padding: 16 }}>
                <Spin />
              </div>
            ) : comments.length === 0 ? (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="还没有评论" />
            ) : (
              comments.map((c) => (
                <div key={c.id} className="kb-comment">
                  <div className="kb-comment__head">
                    <Text strong>{c.author}</Text>
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      {c.createTime ?? ''}
                    </Text>
                    <Popconfirm
                      title="删除这条评论？"
                      okText="删除"
                      cancelText="取消"
                      okButtonProps={{ danger: true }}
                      onConfirm={() => void handleDelete(c)}
                    >
                      <Button type="text" size="small" danger icon={<DeleteOutlined />} />
                    </Popconfirm>
                  </div>
                  <Paragraph style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}>{c.content}</Paragraph>
                </div>
              ))
            )}
          </div>

          <Space.Compact style={{ width: '100%', marginTop: 12 }}>
            <Input
              placeholder="写点评论…（Ctrl+Enter 发送）"
              value={content}
              maxLength={1000}
              onChange={(e) => setContent(e.target.value)}
              onPressEnter={() => void handleAdd()}
            />
            <Button type="primary" icon={<SendOutlined />} loading={submitting} onClick={() => void handleAdd()} />
          </Space.Compact>
        </>
      )}
    </Drawer>
  );
}
