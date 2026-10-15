import { createRoot } from 'react-dom/client';
import App from '@/App';
import '@/styles/global.css';

const container = document.getElementById('root');
if (!container) {
  throw new Error('未找到挂载节点 #root');
}

/**
 * 这里刻意不启用 <StrictMode>：
 * 严格模式在开发环境会重复挂载组件，导致 WebSocket 演示页出现一次多余的重连。
 * 如需开启，请保证 src/hooks/useWebSocket.ts 的清理逻辑可重复执行（当前已满足）。
 */
createRoot(container).render(<App />);
