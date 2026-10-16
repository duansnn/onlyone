import { createRoot } from 'react-dom/client';
import App from '@/App';
import '@/styles/global.css';

const container = document.getElementById('root');
if (!container) {
  throw new Error('未找到挂载节点 #root');
}

/**
 * 开发环境不启用 StrictMode，避免重复挂载带来多余请求。
 */
createRoot(container).render(<App />);
