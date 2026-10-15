import { useCallback, useEffect, useRef, useState } from 'react';
import { Alert, Button, Input, Tag, Typography } from 'antd';
import { ClearOutlined, RobotOutlined, SendOutlined, UserOutlined } from '@ant-design/icons';
import { aiChat, fetchPortalInfo } from '@/api/portal';
import { toast } from '@/utils/feedback';

const { Text, Paragraph } = Typography;

interface ChatItem {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  time: string;
}

const PRESET_PROMPTS = [
  '用一句话介绍这个门户用到的技术栈',
  '给出三条 MySQL 慢查询优化建议',
  '写一个 Java 25 的虚拟线程示例',
];

export default function AiChat() {
  const [items, setItems] = useState<ChatItem[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [aiAvailable, setAiAvailable] = useState<boolean | null>(null);
  const listRef = useRef<HTMLDivElement>(null);

  const loadCapability = useCallback(async () => {
    try {
      const info = await fetchPortalInfo();
      setAiAvailable(info.aiAvailable);
    } catch {
      setAiAvailable(null);
    }
  }, []);

  useEffect(() => {
    void loadCapability();
  }, [loadCapability]);

  useEffect(() => {
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight, behavior: 'smooth' });
  }, [items]);

  const handleSend = async (preset?: string) => {
    const prompt = (preset ?? input).trim();
    if (!prompt) {
      toast.warning('请输入提问内容');
      return;
    }

    const now = new Date().toLocaleTimeString('zh-CN');
    setItems((prev) => [
      ...prev,
      { id: `u-${Date.now()}`, role: 'user', content: prompt, time: now },
    ]);
    setInput('');
    setLoading(true);

    try {
      const answer = await aiChat(prompt);
      setItems((prev) => [
        ...prev,
        {
          id: `a-${Date.now()}`,
          role: 'assistant',
          content: answer || '（模型返回了空内容）',
          time: new Date().toLocaleTimeString('zh-CN'),
        },
      ]);
    } catch {
      setItems((prev) => [
        ...prev,
        {
          id: `e-${Date.now()}`,
          role: 'assistant',
          content: '调用失败。请确认后端已配置 spring.ai.openai.api-key 与 OPENAI_BASE_URL。',
          time: new Date().toLocaleTimeString('zh-CN'),
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="owner-page">
      {aiAvailable === false && (
        <Alert
          type="warning"
          showIcon
          message="后端尚未配置 Spring AI 密钥"
          description={
            <span>
              当前 <Text code>aiAvailable = false</Text>。请设置环境变量 <Text code>OPENAI_API_KEY</Text> 与{' '}
              <Text code>OPENAI_BASE_URL</Text>（可用任意 OpenAI 兼容端点），重启 owner-server 后即可对话。
            </span>
          }
        />
      )}

      <article className="owner-panel">
        <div className="owner-panel__head">
          <h2 className="owner-panel__title">
            对话{' '}
            <Tag className="owner-chip">{aiAvailable ? '已启用' : aiAvailable === false ? '未配置' : 'Spring AI'}</Tag>
          </h2>
          <Button icon={<ClearOutlined />} disabled={!items.length} onClick={() => setItems([])}>
            清空
          </Button>
        </div>

        <div className="owner-chat" ref={listRef}>
          {items.length === 0 ? (
            <div className="owner-prompts">
              {PRESET_PROMPTS.map((prompt, index) => (
                <button key={prompt} type="button" className="owner-prompt-card" onClick={() => void handleSend(prompt)}>
                  <small>示例 0{index + 1}</small>
                  {prompt}
                </button>
              ))}
            </div>
          ) : (
            items.map((item) => (
              <div key={item.id} className={`owner-chat__row owner-chat__row--${item.role}`}>
                <span className={`owner-chat__avatar owner-chat__avatar--${item.role}`}>
                  {item.role === 'user' ? <UserOutlined /> : <RobotOutlined />}
                </span>
                <div
                  className={`owner-chat__bubble ${
                    item.role === 'user' ? 'owner-chat__bubble--user' : 'owner-chat__bubble--assistant'
                  }`}
                >
                  {item.content}
                  <div className="owner-chat__time">{item.time}</div>
                </div>
              </div>
            ))
          )}
        </div>

        <div className="owner-composer">
          <Input.TextArea
            rows={2}
            value={input}
            onChange={(event) => setInput(event.target.value)}
            placeholder="输入问题后按 Ctrl/⌘ + Enter 发送"
            onKeyDown={(event) => {
              if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
                event.preventDefault();
                void handleSend();
              }
            }}
          />
          <Button className="owner-send" type="primary" icon={<SendOutlined />} loading={loading} onClick={() => void handleSend()}>
            发送
          </Button>
        </div>

        <Paragraph type="secondary" style={{ marginTop: 12, marginBottom: 0 }}>
          接口 GET /api/portal/ai/chat，由 AiChatService 基于 Spring AI ChatClient 调用。
        </Paragraph>
      </article>
    </div>
  );
}
