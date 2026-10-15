import { App as AntdApp, Button, Dropdown, Tag, Typography } from 'antd';
import type { MenuProps } from 'antd';
import {
  CalendarOutlined,
  DeleteOutlined,
  EditOutlined,
  ExperimentOutlined,
  MoreOutlined,
} from '@ant-design/icons';
import type { KanbanTask } from '@/types/kanban';

const { Text, Paragraph } = Typography;

const PRIORITY_META: Record<string, { label: string; color: string }> = {
  high: { label: '高', color: 'error' },
  medium: { label: '中', color: 'warning' },
  low: { label: '低', color: 'default' },
};

interface TaskCardProps {
  task: KanbanTask;
  onEdit: (task: KanbanTask) => void;
  onDelete: (task: KanbanTask) => void;
  onOptimize: (task: KanbanTask) => void;
  /** 点击卡片主体打开详情抽屉 */
  onOpen?: (task: KanbanTask) => void;
}

/** 任务卡片 */
export default function TaskCard({ task, onEdit, onDelete, onOptimize, onOpen }: TaskCardProps) {
  const { modal } = AntdApp.useApp();
  const priority = PRIORITY_META[task.priority] ?? PRIORITY_META.medium;

  const handleDelete = () => {
    modal.confirm({
      title: `删除任务「${task.title}」？`,
      content: '删除后不可恢复。',
      okText: '删除',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: () => onDelete(task),
    });
  };

  const menuItems: MenuProps['items'] = [
    { key: 'edit', icon: <EditOutlined />, label: '编辑' },
    { key: 'optimize', icon: <ExperimentOutlined />, label: 'AI 润色描述' },
    { type: 'divider' },
    { key: 'delete', icon: <DeleteOutlined />, label: '删除', danger: true },
  ];

  const tags = (task.tags ?? '')
    .split(',')
    .map((t) => t.trim())
    .filter(Boolean);

  return (
    <div
      className={`kb-task${task.overdue ? ' is-overdue' : ''}`}
      onClick={() => onOpen?.(task)}
      style={{ cursor: onOpen ? 'pointer' : undefined }}
    >
      <div className="kb-task__head">
        <Tag color={priority.color} className="kb-task__priority">
          {priority.label}优先级
        </Tag>
        <Dropdown
          trigger={['click']}
          menu={{
            items: menuItems,
            onClick: ({ key }) => {
              if (key === 'edit') onEdit(task);
              else if (key === 'delete') handleDelete();
              else if (key === 'optimize') onOptimize(task);
            },
          }}
        >
          <Button
            type="text"
            size="small"
            icon={<MoreOutlined />}
            className="kb-task__more"
            onClick={(e) => e.stopPropagation()}
          />
        </Dropdown>
      </div>

      <Paragraph className="kb-task__title" ellipsis={{ rows: 2 }} style={{ marginBottom: 8 }}>
        {task.title}
      </Paragraph>

      {task.description && (
        <Paragraph type="secondary" ellipsis={{ rows: 2 }} className="kb-task__desc" style={{ marginBottom: 8 }}>
          {task.description}
        </Paragraph>
      )}

      {tags.length > 0 && (
        <div className="kb-task__tags">
          {tags.map((tag) => (
            <Tag key={tag} className="kb-tag">
              {tag}
            </Tag>
          ))}
        </div>
      )}

      <div className="kb-task__foot">
        {task.dueDate && (
          <Text type="secondary" className={task.overdue ? 'kb-task__due is-overdue' : 'kb-task__due'}>
            <CalendarOutlined /> {task.dueDate}
          </Text>
        )}
        {task.overdue && (
          <Tag color="error" className="kb-task__overdue-flag">
            已逾期
          </Tag>
        )}
      </div>
    </div>
  );
}
