import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Descriptions,
  Empty,
  Input,
  Radio,
  Row,
  Select,
  Space,
  Tag,
  Tooltip,
  Typography,
} from 'antd';
import {
  ApiOutlined,
  ClearOutlined,
  DisconnectOutlined,
  LinkOutlined,
  ReloadOutlined,
  SendOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import { useWebSocket } from '@/hooks/useWebSocket';
import { broadcastMessage, fetchPortalInfo, sendMessageToClient } from '@/api/portal';
import { useAuthStore } from '@/store/auth';
import { toast } from '@/utils/feedback';

const { Text, Paragraph } = Typography;

function randomClientId(): string {
  return `web-${Math.random().toString(36).slice(2, 8)}`;
}

const STATUS_META: Record<string, { color: string; text: string }> = {
  connecting: { color: 'processing', text: '连接中' },
  open: { color: 'success', text: '已连接' },
  closed: { color: 'default', text: '已断开' },
  error: { color: 'error', text: '连接异常' },
};

export default function WebSocketDemo() {
  const username = useAuthStore((state) => state.username);
  const [clientId, setClientId] = useState(() => `${username ?? 'guest'}-${randomClientId()}`);
  const [text, setText] = useState('Hello Owner IntelliPortal');
  const [target, setTarget] = useState<string>();
  const [onlineClients, setOnlineClients] = useState<string[]>([]);
  const [sendMode, setSendMode] = useState('-');
  const [sending, setSending] = useState(false);

  const { status, messages, connected, url, connect, disconnect, send, clearMessages } = useWebSocket({
    clientId,
    autoConnect: true,
  });

  const refreshOnline = useCallback(async () => {
    try {
      const info = await fetchPortalInfo();
      setOnlineClients(info.websocketOnlineClients ?? []);
      setSendMode(info.websocketSendMode);
    } catch {
      // 拦截器已提示
    }
  }, []);

  useEffect(() => {
    void refreshOnline();
    const timer = window.setInterval(() => void refreshOnline(), 10000);
    return () => window.clearInterval(timer);
  }, [refreshOnline]);

  const handleSend = () => {
    if (!text.trim()) {
      toast.warning('请输入要发送的内容');
      return;
    }
    if (send(text.trim())) {
      toast.success('已通过 WebSocket 发送');
      setText('');
    }
  };

  const handleBroadcastViaHttp = async () => {
    setSending(true);
    try {
      const result = await broadcastMessage(text.trim() || 'HTTP 广播消息');
      toast.success(`广播已提交（send-mode=${result.sendMode}）`);
    } catch {
      // 拦截器已提示
    } finally {
      setSending(false);
    }
  };

  const handleTargetedSend = async () => {
    if (!target) {
      toast.warning('请选择或输入目标客户端 ID');
      return;
    }
    setSending(true);
    try {
      await sendMessageToClient(target, text.trim() || '定向推送消息');
      toast.success(`已向 ${target} 推送`);
    } catch {
      // 拦截器已提示
    } finally {
      setSending(false);
    }
  };

  const meta = STATUS_META[status] ?? STATUS_META.closed;

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Alert
        type="info"
        showIcon
        message="WebSocket 端点：/ws/portal?clientId=xxx"
        description="支持 send-mode 为 local / redis / rocketmq / kafka / rabbitmq。发送纯文本 ping 可获得 pong 回复，发送 online 可查询在线列表。"
      />

      <Row gutter={16}>
        <Col xs={24} lg={10}>
          <Card
            title="连接控制"
            extra={
              <Tag color={connected ? 'green' : 'default'} icon={<ApiOutlined />}>
                {meta.text}
              </Tag>
            }
          >
            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <div>
                <Text type="secondary">客户端 ID</Text>
                <Space.Compact style={{ width: '100%', marginTop: 6 }}>
                  <Input
                    value={clientId}
                    onChange={(event) => setClientId(event.target.value)}
                    placeholder="clientId"
                  />
                  <Tooltip title="换一个随机 clientId（会断开并重连）">
                    <Button icon={<ReloadOutlined />} onClick={() => setClientId(randomClientId())} />
                  </Tooltip>
                </Space.Compact>
              </div>

              <Descriptions column={1} size="small" bordered>
                <Descriptions.Item label="握手地址">
                  <Text code style={{ fontSize: 11 }}>
                    {url}
                  </Text>
                </Descriptions.Item>
                <Descriptions.Item label="连接状态">
                  <Badge status={meta.color as 'success' | 'processing' | 'default' | 'error'} text={meta.text} />
                </Descriptions.Item>
                <Descriptions.Item label="后端发送方式">
                  <Tag className="owner-chip">{sendMode}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="当前在线数">
                  <Badge count={onlineClients.length} showZero color="green" />
                </Descriptions.Item>
              </Descriptions>

              <Space>
                <Button type="primary" icon={<LinkOutlined />} disabled={connected} onClick={connect}>
                  建立连接
                </Button>
                <Button danger icon={<DisconnectOutlined />} disabled={!connected} onClick={disconnect}>
                  断开连接
                </Button>
              </Space>
            </Space>
          </Card>
        </Col>

        <Col xs={24} lg={14}>
          <Card title="发送消息">
            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Input.TextArea
                rows={3}
                value={text}
                onChange={(event) => setText(event.target.value)}
                placeholder="输入要发送给服务端的文本"
                maxLength={500}
                showCount
              />

              <Space wrap>
                <Button type="primary" icon={<SendOutlined />} disabled={!connected} onClick={handleSend}>
                  WebSocket 发送
                </Button>
                <Button disabled={!connected} onClick={() => send('ping')}>
                  发送 ping
                </Button>
                <Button disabled={!connected} onClick={() => send('online')}>
                  查询 online
                </Button>
              </Space>

              <Card size="small" title="通过 HTTP 接口触发服务端推送" variant="outlined">
                <Space direction="vertical" size={12} style={{ width: '100%' }}>
                  <Radio.Group value="broadcast" disabled>
                    <Radio.Button value="broadcast">广播给所有客户端</Radio.Button>
                  </Radio.Group>
                  <Button
                    block
                    icon={<ThunderboltOutlined />}
                    loading={sending}
                    onClick={() => void handleBroadcastViaHttp()}
                  >
                    调用 POST /api/portal/websocket/broadcast
                  </Button>

                  <Space.Compact style={{ width: '100%' }}>
                    <Select
                      style={{ width: '60%' }}
                      placeholder="选择目标客户端"
                      value={target}
                      onChange={setTarget}
                      options={onlineClients.map((id) => ({ label: id, value: id }))}
                      notFoundContent="暂无在线客户端"
                      showSearch
                      allowClear
                    />
                    <Button loading={sending} onClick={() => void handleTargetedSend()}>
                      定向推送
                    </Button>
                  </Space.Compact>
                </Space>
              </Card>
            </Space>
          </Card>
        </Col>
      </Row>

      <Card
        title={<Space>消息记录<Tag>{messages.length}</Tag></Space>}
        extra={
          <Space>
            <Button size="small" icon={<ReloadOutlined />} onClick={() => void refreshOnline()}>
              刷新在线数
            </Button>
            <Button size="small" icon={<ClearOutlined />} onClick={clearMessages}>
              清空
            </Button>
          </Space>
        }
      >
        {messages.length === 0 ? (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无消息，连接后发送 ping 试试" />
        ) : (
          <div className="owner-msg-list">
            {messages.map((msg, index) => (
              <div className="owner-msg" key={`${msg.timestamp ?? index}-${index}`}>
                <div className="owner-msg__meta">
                  <Space size={8}>
                    <Tag
                      color={
                        msg.type === 'system'
                          ? 'default'
                          : msg.type === 'pong'
                            ? 'green'
                            : msg.type === 'broadcast' || msg.type === 'notice'
                              ? 'orange'
                              : 'blue'
                      }
                    >
                      {msg.type}
                    </Tag>
                    {msg.clientId && <Text type="secondary">clientId={msg.clientId}</Text>}
                    <span>{msg.timestamp ? new Date(msg.timestamp).toLocaleTimeString('zh-CN') : ''}</span>
                  </Space>
                </div>
                <div className="owner-msg__payload">{msg.payload}</div>
              </div>
            ))}
          </div>
        )}
      </Card>

      <Paragraph type="secondary" style={{ margin: 0 }}>
        提示：打开本页多个浏览器标签即可模拟多客户端，再使用「广播」或「定向推送」观察实时下发效果。
      </Paragraph>
    </Space>
  );
}
