/**
 * 店铺后台 + 平台端接口。
 */
import { get, post, put } from './request';
import type {
  Category,
  Coupon,
  CouponSave,
  OrderDetail,
  OrderSummary,
  Paged,
  PageQuery,
  PlatformOverview,
  PlatformShop,
  PlatformUser,
  ProductAdmin,
  ProductAdminDetail,
  ProductSave,
  Refund,
  Review,
  Shop,
  ShopDashboard,
  ShopMember,
  SkuStock,
  StockLog,
} from '@/types/mall';

/* ---------------------------- 店铺资料 / 成员 ---------------------------- */

export const shopApi = {
  dashboard: (shopId: string) => get<ShopDashboard>(`/shop/${shopId}/dashboard`),
  profile: (shopId: string) => get<Shop>(`/shop/${shopId}/profile`),
  updateProfile: (shopId: string, data: Partial<Shop>) => put<Shop>(`/shop/${shopId}/profile`, data),
  members: (shopId: string) => get<ShopMember[]>(`/shop/${shopId}/members`),
  addMember: (
    shopId: string,
    data: { mobile: string; password: string; nickname: string; role: string },
  ) => post<ShopMember>(`/shop/${shopId}/members`, data),
  updateMember: (shopId: string, memberId: string, data: { role: string; status: number }) =>
    put<ShopMember>(`/shop/${shopId}/members/${memberId}`, data),
};

/* ------------------------------- 类目 ------------------------------- */

export const categoryApi = {
  tree: (shopId: string) => get<Category[]>(`/shop/${shopId}/categories`),
  save: (shopId: string, data: { id?: string; parentId: string; name: string; sort?: number }) =>
    post<Category>(`/shop/${shopId}/categories`, data),
  setStatus: (shopId: string, categoryId: string, status: number) =>
    put<void>(`/shop/${shopId}/categories/${categoryId}/status`, undefined, { status }),
};

/* ------------------------------- 商品 ------------------------------- */

export const productApi = {
  page: (
    shopId: string,
    params?: { keyword?: string; status?: string; categoryId?: string } & PageQuery,
  ) => get<Paged<ProductAdmin>>(`/shop/${shopId}/products`, params),
  detail: (shopId: string, productId: string) =>
    get<ProductAdminDetail>(`/shop/${shopId}/products/${productId}`),
  create: (shopId: string, data: ProductSave) => post<ProductAdminDetail>(`/shop/${shopId}/products`, data),
  update: (shopId: string, productId: string, data: ProductSave) =>
    put<ProductAdminDetail>(`/shop/${shopId}/products/${productId}`, data),
  setStatus: (shopId: string, productId: string, status: string) =>
    put<void>(`/shop/${shopId}/products/${productId}/status`, undefined, { status }),
};

/* ------------------------------- 库存 ------------------------------- */

export const stockApi = {
  skus: (shopId: string, params?: { keyword?: string; productId?: string } & PageQuery) =>
    get<Paged<SkuStock>>(`/shop/${shopId}/stock/skus`, params),
  adjust: (shopId: string, data: { skuId: string; stock: number; reason: string; remark?: string }) =>
    post<void>(`/shop/${shopId}/stock/adjust`, data),
  logs: (shopId: string, params?: { skuId?: string } & PageQuery) =>
    get<Paged<StockLog>>(`/shop/${shopId}/stock/logs`, params),
};

/* ------------------------------- 订单 ------------------------------- */

export const adminOrderApi = {
  page: (
    shopId: string,
    params?: {
      status?: string;
      orderNo?: string;
      receiverMobile?: string;
      dateFrom?: string;
      dateTo?: string;
    } & PageQuery,
  ) => get<Paged<OrderSummary>>(`/shop/${shopId}/orders`, params),
  detail: (shopId: string, orderId: string) =>
    get<OrderDetail>(`/shop/${shopId}/orders/${orderId}`),
  shipCompanies: (shopId: string) => get<string[]>(`/shop/${shopId}/orders/ship-companies`),
  /** 线下收款：商家确认收到钱，订单从「待确认」转「待发货」 */
  confirmPayment: (shopId: string, orderId: string) =>
    post<void>(`/shop/${shopId}/orders/${orderId}/confirm-payment`),
  ship: (shopId: string, orderId: string, data: { shipCompany: string; shipNo: string }) =>
    post<void>(`/shop/${shopId}/orders/${orderId}/ship`, data),
  updateShip: (shopId: string, orderId: string, data: { shipCompany: string; shipNo: string }) =>
    put<void>(`/shop/${shopId}/orders/${orderId}/ship`, data),
};

/* ------------------------------- 售后 ------------------------------- */

export const adminRefundApi = {
  page: (shopId: string, params?: { status?: string } & PageQuery) =>
    get<Paged<Refund>>(`/shop/${shopId}/refunds`, params),
  detail: (shopId: string, refundId: string) => get<Refund>(`/shop/${shopId}/refunds/${refundId}`),
  agree: (shopId: string, refundId: string) =>
    post<Refund>(`/shop/${shopId}/refunds/${refundId}/agree`),
  refuse: (shopId: string, refundId: string, refuseReason: string) =>
    post<Refund>(`/shop/${shopId}/refunds/${refundId}/refuse`, { refuseReason }),
  confirmReceived: (shopId: string, refundId: string) =>
    post<Refund>(`/shop/${shopId}/refunds/${refundId}/received`),
};

/* ------------------------------- 评价 ------------------------------- */

export const adminReviewApi = {
  page: (shopId: string, params?: { score?: number } & PageQuery) =>
    get<Paged<Review>>(`/shop/${shopId}/reviews`, params),
};

/* ------------------------------ 优惠券 ------------------------------ */

export const adminCouponApi = {
  page: (shopId: string, params?: { status?: string } & PageQuery) =>
    get<Paged<Coupon>>(`/shop/${shopId}/coupons`, params),
  save: (shopId: string, data: CouponSave) => post<Coupon>(`/shop/${shopId}/coupons`, data),
  setStatus: (shopId: string, couponId: string, status: number) =>
    put<void>(`/shop/${shopId}/coupons/${couponId}/status`, undefined, { status }),
  grant: (shopId: string, couponId: string, mobile: string) =>
    post<void>(`/shop/${shopId}/coupons/${couponId}/grant`, undefined, { mobile }),
};

/* ------------------------------- 平台 ------------------------------- */

export const platformApi = {
  overview: () => get<PlatformOverview>('/platform/shops/overview'),
  shops: (params?: { keyword?: string } & PageQuery) =>
    get<Paged<PlatformShop>>('/platform/shops', params),
  createShop: (data: {
    name: string;
    ownerUserId?: string;
    ownerMobile?: string;
    ownerPassword?: string;
    ownerNickname?: string;
    contactName?: string;
    contactMobile?: string;
    shipAddress?: string;
    intro?: string;
  }) => post<Shop>('/platform/shops', data),
  setShopStatus: (shopId: string, status: number) =>
    put<void>(`/platform/shops/${shopId}/status`, undefined, { status }),
  users: (params?: { keyword?: string; status?: number } & PageQuery) =>
    get<Paged<PlatformUser>>('/platform/users', params),
  setUserStatus: (userId: string, status: number) =>
    put<void>(`/platform/users/${userId}/status`, undefined, { status }),
  resetPassword: (userId: string, password: string) =>
    put<void>(`/platform/users/${userId}/password`, { password }),
};
