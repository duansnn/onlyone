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
import Welcome from '@/pages/store/Welcome';
import AdminDashboard from '@/pages/admin/Dashboard';
import AdminOrders from '@/pages/admin/Orders';
import AdminRefunds from '@/pages/admin/Refunds';
import AdminProducts from '@/pages/admin/Products';
import AdminStock from '@/pages/admin/Stock';
import AdminCoupons from '@/pages/admin/Coupons';
import AdminReviews from '@/pages/admin/Reviews';
import AdminShop from '@/pages/admin/Shop';
import AdminCustomers from '@/pages/admin/Customers';
import NotFound from '@/pages/NotFound';
import MobileLayout from '@/mobile/MobileLayout';
import MHome from '@/mobile/pages/MHome';
import MList from '@/mobile/pages/MList';
import MProduct from '@/mobile/pages/MProduct';
import MCart from '@/mobile/pages/MCart';
import MCheckout from '@/mobile/pages/MCheckout';
import MOrders from '@/mobile/pages/MOrders';
import MOrderDetail from '@/mobile/pages/MOrderDetail';
import MAccount from '@/mobile/pages/MAccount';
import MProfile from '@/mobile/pages/MProfile';
import MAddress from '@/mobile/pages/MAddress';
import MLogin from '@/mobile/pages/MLogin';
import { isMerchant, useAuthStore } from '@/store/auth';

/** 需要登录 */
function RequireAuth({ children }: { children: ReactElement }) {
  const token = useAuthStore((state) => state.token);
  const location = useLocation();
  if (!token) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  }
  return children;
}

/** 需要店主身份：顾客访问后台一律送回店面首页 */
function RequireShop({ children }: { children: ReactElement }) {
  const { token, user } = useAuthStore();
  const location = useLocation();
  if (!token) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (!isMerchant(user)) {
    return <Navigate to="/" replace />;
  }
  return children;
}

/** 手机端需要登录：拦下来时回手机端登录页，登录后回到原页 */
function RequireMAuth({ children }: { children: ReactElement }) {
  const token = useAuthStore((state) => state.token);
  const location = useLocation();
  if (!token) {
    return (
      <Navigate
        to="/m/login"
        replace
        state={{ from: location.pathname + location.search }}
      />
    );
  }
  return children;
}

const router = createBrowserRouter([
  { path: '/login', element: <Login /> },
  {
    // 注册后的资料完善页：独立布局，不套店面的页头页脚
    path: '/welcome',
    element: (
      <RequireAuth>
        <Welcome />
      </RequireAuth>
    ),
  },
  { path: '/m/login', element: <MLogin /> },
  {
    path: '/m',
    element: <MobileLayout />,
    children: [
      { index: true, element: <MHome /> },
      { path: 'list', element: <MList /> },
      { path: 'product/:productId', element: <MProduct /> },
      {
        path: 'cart',
        element: (
          <RequireMAuth>
            <MCart />
          </RequireMAuth>
        ),
      },
      {
        path: 'checkout',
        element: (
          <RequireMAuth>
            <MCheckout />
          </RequireMAuth>
        ),
      },
      {
        path: 'orders',
        element: (
          <RequireMAuth>
            <MOrders />
          </RequireMAuth>
        ),
      },
      {
        path: 'orders/:orderId',
        element: (
          <RequireMAuth>
            <MOrderDetail />
          </RequireMAuth>
        ),
      },
      {
        path: 'address',
        element: (
          <RequireMAuth>
            <MAddress />
          </RequireMAuth>
        ),
      },
      {
        path: 'account',
        element: (
          <RequireMAuth>
            <MAccount />
          </RequireMAuth>
        ),
      },
      {
        path: 'profile',
        element: (
          <RequireMAuth>
            <MProfile />
          </RequireMAuth>
        ),
      },
      { path: '*', element: <NotFound /> },
    ],
  },
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
      { path: 'customers', element: <AdminCustomers /> },
      // 旧链接兼容：多商家时代的两个入口，现在统一收进「顾客管理」与「店铺与分类」
      { path: 'users', element: <Navigate to="/admin/customers" replace /> },
      { path: 'shops', element: <Navigate to="/admin/shop" replace /> },
      { path: '*', element: <NotFound /> },
    ],
  },
  { path: '*', element: <NotFound /> },
]);

export default router;
