import type { ReactElement } from 'react';
import { createBrowserRouter, Navigate, useLocation } from 'react-router-dom';
import MainLayout from '@/layout/MainLayout';
import Login from '@/pages/Login';
import Dashboard from '@/pages/Dashboard';
import UserManage from '@/pages/UserManage';
import WebSocketDemo from '@/pages/WebSocketDemo';
import AiChat from '@/pages/AiChat';
import NotFound from '@/pages/NotFound';
import { useAuthStore } from '@/store/auth';

/** 登录态守卫：未登录时跳转登录页并记住来源路径 */
function RequireAuth({ children }: { children: ReactElement }) {
  const token = useAuthStore((state) => state.token);
  const location = useLocation();

  if (!token) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return children;
}

const router = createBrowserRouter([
  {
    path: '/login',
    element: <Login />,
  },
  {
    path: '/',
    element: (
      <RequireAuth>
        <MainLayout />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },
      { path: 'dashboard', element: <Dashboard /> },
      { path: 'users', element: <UserManage /> },
      { path: 'websocket', element: <WebSocketDemo /> },
      { path: 'ai', element: <AiChat /> },
      { path: '*', element: <NotFound /> },
    ],
  },
  {
    path: '*',
    element: <NotFound />,
  },
]);

export default router;
