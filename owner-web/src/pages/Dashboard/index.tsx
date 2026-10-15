import { useCallback, useEffect, useState } from 'react';
import { Button, Input, Space, Switch, Tooltip, Typography } from 'antd';
import { ReloadOutlined, ThunderboltOutlined } from '@ant-design/icons';
import { broadcastMessage, fetchPortalInfo } from '@/api/portal';
import type { PortalInfo } from '@/types/api';
import { toast } from '@/utils/feedback';

const { Text, Paragraph } = Typography;

export default function Dashboard() {
  const [info, setInfo] = useState<PortalInfo | null>(null);
  const [loading, setLoading] = useState(false);
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [broadcastText, setBroadcastText] = useState('这是一条来自 Owner IntelliPortal 前端的广播消息');
  const [sending, setSending] = useState(false);

  const load = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    try {
      setInfo(await fetchPortalInfo());
    } catch {
      // 拦截器已提示，这里保持上一次数据
    } finally {
      if (!silent) setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (!autoRefresh) return;
    const timer = window.setInterval(() => void load(true), 15000);
    return () => window.clearInterval(timer);
  }, [autoRefresh, load]);

  const handleBroadcast = async () => {
    if (!broadcastText.trim()) {
      toast.warning('请输入广播内容');
      return;
    }
    setSending(true);
    try {
      const result = await broadcastMessage(broadcastText.trim());
      toast.success(`广播成功（发送方式：${result.sendMode}）`);
      void load(true);
    } catch {
      // 拦截器已提示
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="owner-page">
      <div className="owner-toolbar">
        <Tooltip title="每 15 秒自动刷新">
          <Space size={6}>
            <Text type="secondary">自动刷新</Text>
            <Switch size="small" checked={autoRefresh} onChange={setAutoRefresh} />
          </Space>
        </Tooltip>
        <Button icon={<ReloadOutlined />} onClick={() => void load()}>
          刷新
        </Button>
      </div>

      <section className="owner-stat-grid">
        <article className="owner-tile owner-tile--olive">
          <span className="owner-tile__label">在线连接</span>
          <strong className="owner-tile__value">{info?.websocketOnlineCount ?? 0}</strong>
          <span className="owner-tile__note">WebSocket</span>
        </article>
        <article className="owner-tile owner-tile--orange">
          <span className="owner-tile__label">心跳次数</span>
          <strong className="owner-tile__value">{info?.quartzHeartbeatExecutions ?? 0}</strong>
          <span className="owner-tile__note">Quartz</span>
        </article>
        <article className="owner-tile owner-tile--brown">
          <span className="owner-tile__label">发送方式</span>
          <strong className="owner-tile__value">{info?.websocketSendMode ?? '-'}</strong>
          <span className="owner-tile__note">消息通道</span>
        </article>
        <article className="owner-tile owner-tile--slate">
          <span className="owner-tile__label">Spring AI</span>
          <strong className="owner-tile__value">{info?.aiAvailable ? '已启用' : '未配置'}</strong>
          <span className="owner-tile__note">{loading && !info ? '正在读取' : '模型能力'}</span>
        </article>
      </section>

      <section className="owner-split">
        <article className="owner-panel">
          <div className="owner-panel__head">
            <h2 className="owner-panel__title">服务信息</h2>
            <span className="owner-panel__extra">{info ? '已连通' : '等待数据'}</span>
          </div>
          {info ? (
            <dl className="owner-facts">
              <div>
                <dt>应用名称</dt>
                <dd>{info.applicationName}</dd>
              </div>
              <div>
                <dt>服务端口</dt>
                <dd>{info.port}</dd>
              </div>
              <div>
                <dt>激活 Profile</dt>
                <dd>{info.activeProfiles || '-'}</dd>
              </div>
              <div>
                <dt>JDK 版本</dt>
                <dd>{info.javaVersion}</dd>
              </div>
              <div>
                <dt>服务器时间</dt>
                <dd>{info.serverTime}</dd>
              </div>
              <div>
                <dt>前端构建</dt>
                <dd>React 19 + Vite</dd>
              </div>
            </dl>
          ) : (
            <div className="owner-empty">暂无数据</div>
          )}
        </article>

        <article className="owner-panel">
          <div className="owner-panel__head">
            <h2 className="owner-panel__title">在线客户端</h2>
            <span className="owner-panel__extra">{info?.websocketOnlineClients?.length ?? 0} 个</span>
          </div>
          {info?.websocketOnlineClients?.length ? (
            <div className="owner-clients">
              {info.websocketOnlineClients.map((clientId) => (
                <div key={clientId} className="owner-client">
                  <span className="owner-client__dot" />
                  <Text code>{clientId}</Text>
                </div>
              ))}
            </div>
          ) : (
            <div className="owner-empty">暂无在线连接，可前往 WebSocket 演示建立连接</div>
          )}
        </article>
      </section>

      <article className="owner-panel">
        <div className="owner-panel__head">
          <h2 className="owner-panel__title">消息广播</h2>
          <span className="owner-panel__extra">当前通道 {info?.websocketSendMode ?? '-'}</span>
        </div>
        <div className="owner-broadcast">
          <Input
            value={broadcastText}
            onChange={(event) => setBroadcastText(event.target.value)}
            placeholder="输入要广播给所有在线客户端的内容"
            onPressEnter={() => void handleBroadcast()}
          />
          <Button type="primary" icon={<ThunderboltOutlined />} loading={sending} onClick={() => void handleBroadcast()}>
            广播
          </Button>
        </div>
        <Paragraph type="secondary" style={{ marginTop: 12, marginBottom: 0 }}>
          接口 POST /api/portal/websocket/broadcast，可选 local / redis / rocketmq / kafka / rabbitmq。
        </Paragraph>
      </article>
    </div>
  );
}
