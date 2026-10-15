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

// 看板模块（独立登录态）
import KanbanLogin from '@/pages/kanban/KanbanLogin';
import KanbanLayout from '@/pages/kanban/KanbanLayout';
import KanbanHome from '@/pages/kanban/KanbanHome';
import KanbanBoard from '@/pages/kanban/KanbanBoard';
import KanbanStats from '@/pages/kanban/KanbanStats';
import KanbanNotes from '@/pages/kanban/KanbanNotes';
import KanbanFocus from '@/pages/kanban/KanbanFocus';
import KanbanBlog from '@/pages/kanban/KanbanBlog';
import { useKanbanAuthStore } from '@/store/kanbanAuth';

/** 门户登录态守卫：未登录时跳转登录页并记住来源路径 */
function RequireAuth({ children }: { children: ReactElement }) {
  const token = useAuthStore((state) => state.token);
  const location = useLocation();

  if (!token) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return children;
}

/** 看板登录态守卫（独立 JWT） */
function RequireKanbanAuth({ children }: { children: ReactElement }) {
  const token = useKanbanAuthStore((state) => state.token);
  const location = useLocation();

  if (!token) {
    return <Navigate to="/kanban/login" replace state={{ from: location.pathname }} />;
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
    path: '/kanban',
    children: [
      { path: 'login', element: <KanbanLogin /> },
      {
        element: (
          <RequireKanbanAuth>
            <KanbanLayout />
          </RequireKanbanAuth>
        ),
        children: [
          { index: true, element: <Navigate to="/kanban/projects" replace /> },
          { path: 'projects', element: <KanbanHome /> },
          { path: 'projects/:projectId/board', element: <KanbanBoard /> },
          { path: 'projects/:projectId/stats', element: <KanbanStats /> },
          { path: 'notes', element: <KanbanNotes /> },
          { path: 'focus', element: <KanbanFocus /> },
          { path: 'blog', element: <KanbanBlog /> },
        ],
      },
    ],
  },
  {
    path: '*',
    element: <NotFound />,
  },
]);

export default router;
