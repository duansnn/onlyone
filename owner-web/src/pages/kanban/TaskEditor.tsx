import { useEffect } from 'react';
import { DatePicker, Form, Input, Modal, Select } from 'antd';
import dayjs from 'dayjs';
import type { Dayjs } from 'dayjs';
import type { KanbanTask, TaskPriority, TaskSavePayload } from '@/types/kanban';

const PRIORITY_OPTIONS = [
  { value: 'high', label: '高' },
  { value: 'medium', label: '中' },
  { value: 'low', label: '低' },
];

const STATUS_OPTIONS = [
  { value: 'todo', label: '待办' },
  { value: 'doing', label: '进行中' },
  { value: 'done', label: '已完成' },
];

interface TaskFormValues {
  title: string;
  description?: string;
  dueDate?: Dayjs;
  priority: TaskPriority;
  status: TaskPriority extends never ? never : 'todo' | 'doing' | 'done';
  tags?: string;
}

interface TaskEditorProps {
  open: boolean;
  /** 编辑目标任务；为 null 表示新增 */
  task: KanbanTask | null;
  /** 新增时的默认状态 */
  defaultStatus?: 'todo' | 'doing' | 'done';
  saving: boolean;
  onCancel: () => void;
  onSubmit: (payload: TaskSavePayload) => void;
}

/** 任务新增/编辑弹窗 */
export default function TaskEditor({ open, task, defaultStatus, saving, onCancel, onSubmit }: TaskEditorProps) {
  const [form] = Form.useForm<TaskFormValues>();

  useEffect(() => {
    if (!open) return;
    if (task) {
      form.setFieldsValue({
        title: task.title,
        description: task.description,
        dueDate: task.dueDate ? dayjs(task.dueDate) : undefined,
        priority: task.priority,
        status: task.status,
        tags: task.tags,
      });
    } else {
      form.resetFields();
      form.setFieldsValue({
        priority: 'medium',
        status: defaultStatus ?? 'todo',
      });
    }
  }, [open, task, defaultStatus, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    const payload: TaskSavePayload = {
      title: values.title,
      description: values.description,
      dueDate: values.dueDate ? values.dueDate.format('YYYY-MM-DD') : undefined,
      priority: values.priority,
      status: values.status,
      tags: values.tags,
    };
    onSubmit(payload);
  };

  return (
    <Modal
      title={task ? '编辑任务' : '新建任务'}
      open={open}
      onOk={handleOk}
      onCancel={onCancel}
      okText="保存"
      cancelText="取消"
      confirmLoading={saving}
      destroyOnClose
      width={560}
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item
          name="title"
          label="任务标题"
          rules={[
            { required: true, message: '请输入任务标题' },
            { max: 200, message: '任务标题最长 200 字' },
          ]}
        >
          <Input placeholder="要做什么？" />
        </Form.Item>

        <Form.Item
          name="description"
          label="任务详情"
          rules={[{ max: 5000, message: '任务详情最长 5000 字' }]}
        >
          <Input.TextArea rows={3} placeholder="补充任务细节（可选）" />
        </Form.Item>

        <div className="kb-form-row">
          <Form.Item name="priority" label="优先级" style={{ flex: 1 }}>
            <Select options={PRIORITY_OPTIONS} />
          </Form.Item>
          <Form.Item name="status" label="状态" style={{ flex: 1 }}>
            <Select options={STATUS_OPTIONS} />
          </Form.Item>
          <Form.Item name="dueDate" label="截止日期" style={{ flex: 1.2 }}>
            <DatePicker style={{ width: '100%' }} placeholder="选择日期" />
          </Form.Item>
        </div>

        <Form.Item
          name="tags"
          label="标签"
          rules={[{ max: 200, message: '标签最长 200 字' }]}
          extra="多个标签用逗号分隔，如：前端,紧急,设计"
        >
          <Input placeholder="如：前端,紧急" />
        </Form.Item>
      </Form>
    </Modal>
  );
}
