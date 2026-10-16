import type { ReactElement } from 'react';
import { createBrowserRouter, Navigate, useLocation } from 'react-router-dom';
import StoreLayout from '@/layout/StoreLayout';
import AdminLayout from '@/layout/AdminLayout';
import Login from '@/pages/Login';
import Home from '@/pages/store/Home';
import ProductList from '@/pages/store/ProductList';
import ProductDetail from '@/pages/store/ProductDetail';
import Cart from '@/pages/store/Cart';
import Checkout from '@/pages/store/Checkout';
import Orders from '@/pages/store/Orders';
import Account from '@/pages/store/Account';
import AdminDashboard from '@/pages/admin/Dashboard';
import AdminOrders from '@/pages/admin/Orders';
import AdminRefunds from '@/pages/admin/Refunds';
import AdminProducts from '@/pages/admin/Products';
import AdminStock from '@/pages/admin/Stock';
import AdminCoupons from '@/pages/admin/Coupons';
import AdminReviews from '@/pages/admin/Reviews';
import AdminShop from '@/pages/admin/Shop';
import PlatformShops from '@/pages/platform/Shops';
import PlatformUsers from '@/pages/platform/Users';
import NotFound from '@/pages/NotFound';
import { useAuthStore } from '@/store/auth';

/** 需要登录 */
function RequireAuth({ children }: { children: ReactElement }) {
  const token = useAuthStore((state) => state.token);
  const location = useLocation();
  if (!token) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  }
  return children;
}

/** 需要有店铺身份（店主或店员） */
function RequireShop({ children }: { children: ReactElement }) {
  const { token, shops } = useAuthStore();
  const location = useLocation();
  if (!token) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (!shops.length) {
    return <Navigate to="/" replace />;
  }
  return children;
}

/** 需要平台管理员 */
function RequirePlatform({ children }: { children: ReactElement }) {
  const user = useAuthStore((state) => state.user);
  if (!user?.platformAdmin) {
    return <Navigate to="/admin" replace />;
  }
  return children;
}

const router = createBrowserRouter([
  { path: '/login', element: <Login /> },
  {
    path: '/',
    element: <StoreLayout />,
    children: [
      { index: true, element: <Home /> },
      { path: 'products', element: <ProductList /> },
      { path: 'product/:productId', element: <ProductDetail /> },
      {
        path: 'cart',
        element: (
          <RequireAuth>
            <Cart />
          </RequireAuth>
        ),
      },
      {
        path: 'checkout',
        element: (
          <RequireAuth>
            <Checkout />
          </RequireAuth>
        ),
      },
      {
        path: 'orders',
        element: (
          <RequireAuth>
            <Orders />
          </RequireAuth>
        ),
      },
      {
        path: 'account',
        element: (
          <RequireAuth>
            <Account />
          </RequireAuth>
        ),
      },
      { path: '*', element: <NotFound /> },
    ],
  },
  {
    path: '/admin',
    element: (
      <RequireShop>
        <AdminLayout />
      </RequireShop>
    ),
    children: [
      { index: true, element: <AdminDashboard /> },
      { path: 'orders', element: <AdminOrders /> },
      { path: 'refunds', element: <AdminRefunds /> },
      { path: 'products', element: <AdminProducts /> },
      { path: 'stock', element: <AdminStock /> },
      { path: 'coupons', element: <AdminCoupons /> },
      { path: 'reviews', element: <AdminReviews /> },
      { path: 'shop', element: <AdminShop /> },
      {
        path: 'shops',
        element: (
          <RequirePlatform>
            <PlatformShops />
          </RequirePlatform>
        ),
      },
      {
        path: 'users',
        element: (
          <RequirePlatform>
            <PlatformUsers />
          </RequirePlatform>
        ),
      },
      { path: '*', element: <NotFound /> },
    ],
  },
  { path: '*', element: <NotFound /> },
]);

export default router;
