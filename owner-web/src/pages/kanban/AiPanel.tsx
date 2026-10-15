import { useState } from 'react';
import { Button, Form, Input, InputNumber, Modal, Radio, Space, Typography } from 'antd';
import dayjs from 'dayjs';
import { kanbanAiApi, kanbanTaskApi } from '@/api/kanban';
import type { AiSubTask, KanbanTask, TaskImportItem } from '@/types/kanban';
import { toast } from '@/utils/feedback';

const { Text, Paragraph } = Typography;

export type AiMode = 'split' | 'report' | 'optimize';

interface AiPanelProps {
  open: boolean;
  mode: AiMode;
  projectId: string;
  /** 润色模式下的目标任务 */
  task?: KanbanTask | null;
  onClose: () => void;
  /** 拆解任务批量落库后回调刷新 */
  onTasksCreated?: () => void;
  /** 润色结果应用后回调 */
  onTaskOptimized?: (taskId: string, description: string) => void;
}

/** AI 能力弹窗：任务拆解 / 项目周报 / 描述润色 */
export default function AiPanel({
  open,
  mode,
  projectId,
  task,
  onClose,
  onTasksCreated,
  onTaskOptimized,
}: AiPanelProps) {
  const [loading, setLoading] = useState(false);

  // 拆解结果
  const [subTasks, setSubTasks] = useState<AiSubTask[]>([]);
  // 周报 / 润色结果文本
  const [resultText, setResultText] = useState('');

  const reset = () => {
    setSubTasks([]);
    setResultText('');
  };

  const title = mode === 'split' ? 'AI 任务拆解' : mode === 'report' ? 'AI 生成项目周报' : 'AI 任务描述润色';

  // —— 拆解 ——
  const [splitForm] = Form.useForm<{ goal: string; maxCount: number }>();
  const handleSplit = async () => {
    const values = await splitForm.validateFields();
    setLoading(true);
    try {
      const list = await kanbanAiApi.splitTask({
        projectId,
        goal: values.goal,
        maxCount: values.maxCount,
      });
      setSubTasks(list);
    } catch {
      // 错误已提示
    } finally {
      setLoading(false);
    }
  };

  const handleBatchCreate = async () => {
    const items: TaskImportItem[] = subTasks.map((s) => ({
      title: s.title,
      description: s.description,
      priority: s.priority,
      tags: s.tags,
    }));
    setLoading(true);
    try {
      const count = await kanbanTaskApi.batchCreate(projectId, items);
      toast.success(`已创建 ${count} 个任务到「待办」`);
      onTasksCreated?.();
      onClose();
    } catch {
      // 错误已提示
    } finally {
      setLoading(false);
    }
  };

  // —— 周报 ——
  const [reportForm] = Form.useForm<{ range: [dayjs.Dayjs, dayjs.Dayjs] }>();
  const handleReport = async () => {
    const values = await reportForm.validateFields();
    setLoading(true);
    try {
      const text = await kanbanAiApi.generateReport({
        projectId,
        startDate: values.range[0].format('YYYY-MM-DD'),
        endDate: values.range[1].format('YYYY-MM-DD'),
      });
      setResultText(text);
    } catch {
      // 错误已提示
    } finally {
      setLoading(false);
    }
  };

  // —— 润色 ——
  const handleOptimize = async () => {
    if (!task) return;
    setLoading(true);
    try {
      const text = await kanbanAiApi.optimizeTask({ taskId: task.id });
      setResultText(text);
    } catch {
      // 错误已提示
    } finally {
      setLoading(false);
    }
  };

  const handleApplyOptimize = async () => {
    if (!task || !resultText) return;
    onTaskOptimized?.(task.id, resultText);
    onClose();
  };

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(resultText);
      toast.success('已复制到剪贴板');
    } catch {
      toast.error('复制失败，请手动选择复制');
    }
  };

  const renderSplit = () => (
    <div>
      <Form form={splitForm} layout="vertical" onFinish={handleSplit}>
        <Form.Item
          name="goal"
          label="目标描述"
          rules={[
            { required: true, message: '请输入目标描述' },
            { max: 1000, message: '目标描述最长 1000 字' },
          ]}
        >
          <Input.TextArea rows={4} placeholder="例如：开发一个个人作品集网站，包含首页、项目展示、关于我、联系我四个页面" />
        </Form.Item>
        <Form.Item name="maxCount" label="子任务数量上限" initialValue={8}>
          <InputNumber min={1} max={20} style={{ width: '100%' }} />
        </Form.Item>
        <Button type="primary" htmlType="submit" block loading={loading}>
          生成子任务
        </Button>
      </Form>

      {subTasks.length > 0 && (
        <div className="kb-ai-result">
          <Text strong>拆解结果（{subTasks.length} 条）</Text>
          <div className="kb-ai-subtasks">
            {subTasks.map((s, idx) => (
              <div key={idx} className="kb-ai-subtask">
                <Text strong>{idx + 1}. {s.title}</Text>
                {s.description && <Paragraph type="secondary" style={{ margin: '4px 0 0' }}>{s.description}</Paragraph>}
                {s.priority && <Text type="secondary" style={{ fontSize: 12 }}>优先级：{s.priority}</Text>}
              </div>
            ))}
          </div>
          <Space style={{ marginTop: 12 }}>
            <Button type="primary" loading={loading} onClick={handleBatchCreate}>
              一键创建到待办
            </Button>
            <Button onClick={() => setSubTasks([])}>重新生成</Button>
          </Space>
        </div>
      )}
    </div>
  );

  const renderReport = () => (
    <div>
      <Form form={reportForm} layout="vertical" onFinish={handleReport}>
        <Form.Item
          name="range"
          label="时间范围"
          rules={[{ required: true, message: '请选择时间范围' }]}
          initialValue={[dayjs().subtract(7, 'day'), dayjs()]}
        >
          <Radio.Group style={{ marginBottom: 8 }}>
            <Radio.Button value="7">近 7 天</Radio.Button>
            <Radio.Button value="30">近 30 天</Radio.Button>
          </Radio.Group>
        </Form.Item>
        <Button type="primary" htmlType="submit" block loading={loading}>
          生成周报
        </Button>
      </Form>

      {resultText && (
        <div className="kb-ai-result">
          <div className="kb-ai-text">{resultText}</div>
          <Button style={{ marginTop: 12 }} onClick={handleCopy}>
            复制周报
          </Button>
        </div>
      )}
    </div>
  );

  const renderOptimize = () => (
    <div>
      {!resultText ? (
        <div>
          <Paragraph type="secondary">
            将调用 AI 润色任务「{task?.title}」的描述文案，使表达更清晰专业。
          </Paragraph>
          <Paragraph type="secondary" style={{ marginTop: 4 }}>
            当前描述：{task?.description || '（无）'}
          </Paragraph>
          <Button type="primary" block loading={loading} onClick={handleOptimize}>
            开始润色
          </Button>
        </div>
      ) : (
        <div className="kb-ai-result">
          <div className="kb-ai-text">{resultText}</div>
          <Space style={{ marginTop: 12 }}>
            <Button type="primary" onClick={handleApplyOptimize}>
              应用润色结果
            </Button>
            <Button onClick={handleCopy}>复制</Button>
            <Button onClick={() => setResultText('')}>重新生成</Button>
          </Space>
        </div>
      )}
    </div>
  );

  return (
    <Modal
      title={title}
      open={open}
      onCancel={() => {
        reset();
        onClose();
      }}
      footer={null}
      destroyOnClose
      width={560}
    >
      <div style={{ marginTop: 16 }}>
        {mode === 'split' && renderSplit()}
        {mode === 'report' && renderReport()}
        {mode === 'optimize' && renderOptimize()}
      </div>
    </Modal>
  );
}
