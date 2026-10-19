/**
 * 顾客侧接口：账号、店面、地址、购物车、订单、支付、售后、评价、优惠券。
 * baseURL 为 /api，因此这里的路径都不带 /api 前缀。
 */
import { del, get, post, put, uploadFile } from './request';
import type {
  Address,
  AddressSave,
  AuthUser,
  Cart,
  Coupon,
  CouponUser,
  LoginResult,
  MobileCode,
  OrderDetail,
  OrderPreview,
  OrderSubmitResult,
  OrderSummary,
  Paged,
  PageQuery,
  ProductCard,
  ProductDetail,
  Refund,
  Review,
  ShopBrief,
  StoreHome,
} from '@/types/mall';

/* ------------------------------- 上传 ------------------------------- */

/** 图片上传：返回可直接用于 img src 的相对地址（如 /files/product/2026/10/19/xxx.jpg） */
export const uploadApi = {
  image: (file: File, folder: 'product' | 'shop' | 'review' | 'avatar' = 'product') =>
    uploadFile<string>('/upload/image', file, { folder }),
};

/* ------------------------------- 账号 ------------------------------- */

export const authApi = {
  register: (data: { mobile: string; password: string; nickname: string }) =>
    post<LoginResult>('/auth/register', data),
  login: (data: { account: string; password: string }) => post<LoginResult>('/auth/login', data),
  logout: () => post<void>('/auth/logout'),
  me: () => get<AuthUser>('/auth/me'),
  updateProfile: (data: { nickname?: string; email?: string; avatarUrl?: string }) =>
    put<AuthUser>('/auth/profile', data),
  changePassword: (data: { currentPassword: string; newPassword: string }) =>
    put<void>('/auth/password', data),
  sendMobileCode: (mobile: string) => post<MobileCode>('/auth/mobile/code', undefined, { mobile }),
  changeMobile: (data: { code: string; mobile: string }) => put<void>('/auth/mobile', data),
};

/* ------------------------------- 店面 ------------------------------- */

export const storeApi = {
  /** 营业中的店铺（公开），未登录时用来确定默认店铺 */
  shops: () => get<ShopBrief[]>('/storefront/shops'),
  home: (shopId: string) => get<StoreHome>(`/storefront/shops/${shopId}/home`),
  products: (
    shopId: string,
    params?: PageQuery & { categoryId?: string; sort?: string; pageSize?: number },
  ) => get<Paged<ProductCard>>(`/storefront/shops/${shopId}/products`, params),
  search: (shopId: string, params: { keyword: string; sort?: string } & PageQuery) =>
    get<Paged<ProductCard>>(`/storefront/shops/${shopId}/search`, params),
  detail: (shopId: string, productId: string) =>
    get<ProductDetail>(`/storefront/shops/${shopId}/products/${productId}`),
  reviews: (shopId: string, productId: string, params?: PageQuery) =>
    get<Paged<Review>>(`/storefront/shops/${shopId}/products/${productId}/reviews`, params),
};

/* ------------------------------- 地址 ------------------------------- */

export const addressApi = {
  list: () => get<Address[]>('/address'),
  create: (data: AddressSave) => post<Address>('/address', data),
  update: (data: AddressSave) => put<Address>('/address', data),
  setDefault: (addressId: string) => put<void>(`/address/${addressId}/default`),
  remove: (addressId: string) => del<void>(`/address/${addressId}`),
};

/* ------------------------------ 购物车 ------------------------------ */

export const cartApi = {
  list: () => get<Cart>('/cart'),
  count: () => get<number>('/cart/count'),
  add: (data: { skuId: string; quantity: number }) => post<Cart>('/cart', data),
  merge: (items: { skuId: string; quantity: number }[]) => post<Cart>('/cart/merge', { items }),
  updateQuantity: (data: { skuId: string; quantity: number }) => put<Cart>('/cart/quantity', data),
  remove: (skuId: string) => del<void>(`/cart/${skuId}`),
  clear: () => del<void>('/cart'),
};

/* ------------------------------- 订单 ------------------------------- */

export interface OrderLine {
  skuId: string;
  quantity: number;
  price: number;
}

export const orderApi = {
  preview: (data: { shopId: string; items: OrderLine[]; couponUserId?: string | null }) =>
    post<OrderPreview>('/order/preview', data),
  submit: (data: {
    shopId: string;
    items: OrderLine[];
    addressId: string;
    couponUserId?: string | null;
    buyerRemark?: string;
  }) => post<OrderSubmitResult>('/order/submit', data),
  mine: (params?: { status?: string } & PageQuery) =>
    get<Paged<OrderSummary>>('/order/mine', params),
  detail: (orderId: string) => get<OrderDetail>(`/order/${orderId}`),
  cancel: (orderId: string, reason: string) => post<void>(`/order/${orderId}/cancel`, { reason }),
  confirmReceipt: (orderId: string) => post<void>(`/order/${orderId}/confirm`),
  options: () =>
    get<{
      cancelReasons: string[];
      shipCompanies: string[];
      payTimeoutMinutes: number;
      autoFinishDays: number;
      refundWindowDays: number;
    }>('/order/options'),
};

/* ------------------------------- 售后 ------------------------------- */

export const refundApi = {
  apply: (
    orderId: string,
    data: { type: 'REFUND_ONLY' | 'RETURN_REFUND'; reason: string; description?: string },
  ) => post<Refund>(`/refund/orders/${orderId}`, data),
  mine: (params?: { status?: string } & PageQuery) => get<Paged<Refund>>('/refund/mine', params),
  detail: (refundId: string) => get<Refund>(`/refund/${refundId}`),
  returnShip: (refundId: string, data: { returnCompany: string; returnShipNo: string }) =>
    post<Refund>(`/refund/${refundId}/return-ship`, data),
  options: () =>
    get<{
      refundOnlyReasons: string[];
      returnRefundReasons: string[];
      statusFilters: { value: string; label: string }[];
      refundWindowDays: number;
    }>('/refund/options'),
};

/* ------------------------------- 评价 ------------------------------- */

export const reviewApi = {
  submit: (data: { orderId: string; score: number; content?: string; imageUrls?: string[] }) =>
    post<void>('/review', data),
  ofOrder: (orderId: string) => get<Review | null>(`/review/orders/${orderId}`),
};

/* ------------------------------ 优惠券 ------------------------------ */

export const couponApi = {
  center: (shopId: string, params?: PageQuery) =>
    get<Paged<Coupon>>('/coupon/center', { shopId, ...params }),
  receive: (couponId: string) => post<CouponUser>(`/coupon/${couponId}/receive`),
  mine: (params?: { shopId?: string; status?: string }) =>
    get<CouponUser[]>('/coupon/mine', params),
  usable: (shopId: string, goodsAmount: number) =>
    get<CouponUser[]>('/coupon/usable', { shopId, goodsAmount }),
};
