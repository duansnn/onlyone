// WebSocket 端到端验证脚本（Node 22 内置 WebSocket）
const BASE = 'http://localhost:41452';
const WS = 'ws://localhost:41452/ws/portal?clientId=node-test-client';

const received = [];
let pongOk = false;

const ws = new WebSocket(WS);

const done = (code) => {
  try { ws.close(); } catch { /* ignore */ }
  setTimeout(() => process.exit(code), 100);
};

ws.addEventListener('open', () => {
  console.log('[WS] 已连接:', WS);
  ws.send('ping');
  ws.send('online');
});

ws.addEventListener('message', (ev) => {
  const text = ev.data;
  received.push(text);
  console.log('[WS] 收到:', text);
  if (text.includes('"type":"pong"')) { pongOk = true; }

  // 收到 pong 后触发一次 HTTP 广播，验证服务端 -> 客户端推送
  if (text.includes('"type":"pong"') && received.length === 1) {
    fetch(`${BASE}/api/portal/websocket/broadcast?content=来自HTTP的广播`, { method: 'POST' })
      .then((r) => r.text())
      .then((t) => console.log('[HTTP] 广播响应:', t));
  }
});

ws.addEventListener('error', (e) => {
  console.error('[WS] 错误:', e.message ?? e);
});

setTimeout(async () => {
  const info = await fetch(`${BASE}/api/portal/info`).then((r) => r.json());
  console.log('[INFO] onlineCount =', info.data.websocketOnlineCount,
              '| sendMode =', info.data.websocketSendMode);

  const ok = pongOk && received.some((m) => m.includes('来自HTTP的广播'));
  console.log(ok ? '[RESULT] WebSocket 端到端验证通过 ✅' : '[RESULT] 验证失败 ❌');
  done(ok ? 0 : 1);
}, 3000);
