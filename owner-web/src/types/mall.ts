/**
 * 日用铺电商系统 —— 与后端 com.owner.system.mall.domain 对应的类型。
 *
 * 重要：后端把「包装类型 Long」统一序列化成字符串（雪花 ID 会丢精度），
 * 「基本类型 long」仍是数字。因此：
 *   - 所有 id / 金额字段（Long）声明为 string | number，一律用 num() 转换后再运算；
 *   - 统计类字段（long）是 number。
 */
export type Id = string;
/** Long 包装类型：可能是字符串也可能是数字，用 num() 归一 */
export type Money = string | number;

export interface Paged<T> {
  rows: T[];
  total: number;
  pageNum: number;
  pageSize: number;
  pages: number;
}

export interface PageQuery {
  pageNum?: number;
  pageSize?: number;
}

/* ------------------------------- 账号 ------------------------------- */

/** 全站角色：MERCHANT 店主（能进后台），CUSTOMER 顾客（只能逛店面） */
export type MallRole = 'MERCHANT' | 'CUSTOMER';

export interface AuthUser {
  id: Id;
  username: string;
  nickname: string;
  mobile?: string;
  email?: string;
  avatarUrl?: string;
  /** 角色。旧缓存里可能没有该字段，用 store/auth 的 isMerchant() 判断 */
  role?: MallRole;
  roleLabel?: string;
  /** 旧字段，等价于 role === 'MERCHANT' */
  platformAdmin: boolean;
}

export interface ShopBrief {
  shopId: Id;
  shopName: string;
  role: string;
  roleLabel: string;
}

export interface LoginResult {
  token: string;
  tokenType: string;
  expireSeconds: number;
  /** 登录响应里就带了当前用户，不要再额外调 /auth/me（那时 token 还没入 store，会 401） */
  user: AuthUser;
  shops: ShopBrief[];
}

export interface MobileCode {
  mobile: string;
  expireSeconds: number;
  /** 本地联调时后端回显的验证码，生产为 null */
  devCode?: string | null;
}

/* ------------------------------ 店面 ------------------------------ */

export interface Category {
  id: Id;
  parentId: Id;
  name: string;
  sort: number;
  status: number;
  statusLabel: string;
  children?: Category[];
}

export interface ProductCard {
  id: Id;
  name: string;
  sellingPoint?: string;
  coverImage?: string;
  minPrice: number;
  maxPrice: number;
  totalAvailable: number;
}

export interface Sku {
  id: Id;
  specName: string;
  price: Money;
  listPrice?: Money;
  stock: number;
  lockedStock: number;
  availableStock: number;
  skuCode?: string;
  weightGram?: number;
  status: number;
  statusLabel: string;
}

export interface ProductDetail {
  id: Id;
  shopId: Id;
  name: string;
  sellingPoint?: string;
  detail?: string;
  images: string[];
  skus: Sku[];
  minPrice: number;
  maxPrice: number;
  totalAvailable: number;
  reviewCount: number;
}

export interface StoreHome {
  shopId: Id;
  shopName: string;
  intro?: string;
  notice?: string;
  bannerUrl?: string;
  businessHours?: string;
  categories: Category[];
  products: ProductCard[];
  productTotal: number;
}

/* ------------------------------ 地址 ------------------------------ */

export interface Address {
  id: Id;
  receiver: string;
  mobile: string;
  province: string;
  city: string;
  district: string;
  detail: string;
  fullAddress: string;
  isDefault: number;
}

export interface AddressSave {
  id?: Id;
  receiver: string;
  mobile: string;
  province: string;
  city: string;
  district: string;
  detail: string;
  isDefault?: boolean;
}

/* ------------------------------ 购物车 ------------------------------ */

export interface CartItem {
  id: Id;
  skuId: Id;
  productId: Id;
  productName: string;
  specName: string;
  imageUrl?: string;
  price: Money;
  quantity: number;
  amount: Money;
  availableStock: number;
  valid: boolean;
  invalidReason?: string;
}

export interface CartShopGroup {
  shopId: Id;
  shopName: string;
  shopStatus: number;
  items: CartItem[];
  invalidItems: CartItem[];
}

export interface Cart {
  shops: CartShopGroup[];
  itemCount: number;
  validItemCount: number;
}

/* ------------------------------ 订单 ------------------------------ */

export type OrderStatus =
  | 'PENDING_PAYMENT'
  | 'PENDING_SHIPMENT'
  | 'PENDING_RECEIPT'
  | 'FINISHED'
  | 'CANCELLED'
  | 'REFUNDING'
  | 'REFUNDED';

export interface OrderItem {
  id: Id;
  orderId: Id;
  productId: Id;
  skuId: Id;
  productName: string;
  specName: string;
  imageUrl?: string;
  price: Money;
  quantity: number;
  amount: Money;
}

export interface OrderLog {
  id: Id;
  fromStatus?: OrderStatus;
  fromStatusLabel?: string;
  toStatus: OrderStatus;
  toStatusLabel: string;
  operatorType: string;
  operatorTypeLabel: string;
  operatorId?: Id;
  remark?: string;
  createTime: string;
}

export interface OrderSummary {
  id: Id;
  orderNo: string;
  shopId: Id;
  shopName: string;
  status: OrderStatus;
  statusLabel: string;
  goodsAmount: number;
  freightAmount: number;
  discountAmount: number;
  payAmount: number;
  itemCount: number;
  productSummary: string;
  coverImage?: string;
  receiverMobileMasked?: string;
  shipCompany?: string;
  shipNo?: string;
  cancelReason?: string;
  createTime: string;
  payTime?: string;
  shipTime?: string;
  finishTime?: string;
  cancelTime?: string;
  expireTime?: string;
  canCancel: boolean;
  canConfirmReceipt: boolean;
  canReview: boolean;
  canApplyRefund: boolean;
  canShip: boolean;
}

export interface OrderDetail {
  summary: OrderSummary;
  receiverName: string;
  receiverMobile: string;
  receiverAddress: string;
  buyerRemark?: string;
  payChannel?: string;
  payNo?: string;
  paidAmount?: Money;
  items: OrderItem[];
  logs: OrderLog[];
  reviewed?: boolean;
  canReview?: boolean;
  /** 后端字段名就是 currentRefund（OrderDetailVO.currentRefund） */
  currentRefund?: Refund | null;
}

export interface OrderPreview {
  shopId: Id;
  shopName: string;
  items: OrderItem[];
  invalidItems: CartItem[];
  goodsAmount: number;
  freightAmount: number;
  discountAmount: number;
  payAmount: number;
  couponUserId?: Id | null;
  couponName?: string;
  usableCoupons: CouponUser[];
  message?: string;
}

export interface OrderSubmitResult {
  orderId: Id;
  orderNo: string;
  payAmount: number;
  expireTime: string;
}

/* ------------------------------ 售后 ------------------------------ */

export type RefundStatus =
  | 'PENDING'
  | 'AGREED'
  | 'WAITING_RETURN'
  | 'RECEIVED'
  | 'REJECTED'
  | 'SUCCESS';

export interface Refund {
  id: Id;
  orderId: Id;
  orderNo: string;
  shopId: Id;
  shopName: string;
  userId: Id;
  type: 'REFUND_ONLY' | 'RETURN_REFUND';
  typeLabel: string;
  reason: string;
  description?: string;
  status: RefundStatus;
  statusLabel: string;
  refundAmount: Money;
  refuseReason?: string;
  returnCompany?: string;
  returnShipNo?: string;
  returnAddress?: string;
  returnTime?: string;
  finishTime?: string;
  createTime: string;
}

/* ------------------------------ 评价 ------------------------------ */

export interface Review {
  id: Id;
  orderId: Id;
  userId: Id;
  nickname: string;
  avatarUrl?: string;
  score: number;
  content?: string;
  images: string[];
  createTime: string;
}

/* ------------------------------ 优惠券 ------------------------------ */

export interface Coupon {
  id: Id;
  shopId: Id;
  name: string;
  thresholdAmount: Money;
  reduceAmount: Money;
  total: number;
  received: number;
  perUserLimit: number;
  startTime?: string;
  endTime?: string;
  status: number;
  statusLabel: string;
  expired: boolean;
  receivable: boolean;
  createTime?: string;
}

export interface CouponUser {
  id: Id;
  couponId: Id;
  shopId: Id;
  name: string;
  thresholdAmount: Money;
  reduceAmount: Money;
  perUserLimit: number;
  startTime?: string;
  endTime?: string;
  status: string;
  statusLabel: string;
  orderId?: Id | null;
  expired: boolean;
  receiveTime?: string;
  useTime?: string;
}

export interface CouponSave {
  id?: Id;
  name: string;
  thresholdAmount: number;
  reduceAmount: number;
  total: number;
  perUserLimit: number;
  startTime?: string;
  endTime?: string;
  status?: string;
}

/* ---------------------------- 店铺后台 ---------------------------- */

export interface Shop {
  id: Id;
  name: string;
  intro?: string;
  notice?: string;
  contactName?: string;
  contactMobile?: string;
  shipAddress?: string;
  bannerUrl?: string;
  businessHours?: string;
  payNote?: string;
  status: number;
  statusLabel: string;
  version: number;
  createTime?: string;
  updateTime?: string;
}

export interface ShopDashboard {
  todayOrderCount: number;
  todayPaidAmount: number;
  pendingShipmentCount: number;
  pendingRefundCount: number;
  onSaleProductCount: number;
}

export interface ShopMember {
  id: Id;
  userId: Id;
  username: string;
  nickname: string;
  mobile?: string;
  role: string;
  roleLabel: string;
  status: number;
  statusLabel: string;
  createTime?: string;
}

export interface ProductAdmin {
  id: Id;
  categoryId: Id;
  categoryName?: string;
  name: string;
  status: string;
  statusLabel: string;
  sort: number;
  coverImage?: string;
  minPrice: number;
  maxPrice: number;
  totalStock: number;
  totalAvailable: number;
  skuCount: number;
  version: number;
  updateTime?: string;
}

export interface SkuSave {
  id?: Id;
  specName: string;
  price: number;
  listPrice?: number;
  stock: number;
  skuCode?: string;
  weightGram?: number;
  status?: string;
}

export interface ProductSave {
  categoryId: Id;
  name: string;
  sellingPoint?: string;
  detail?: string;
  status: string;
  sort?: number;
  images: string[];
  skus: SkuSave[];
  version?: number;
}

export interface ProductAdminDetail {
  id: Id;
  categoryId: Id;
  name: string;
  sellingPoint?: string;
  detail?: string;
  status: string;
  statusLabel: string;
  sort: number;
  images: string[];
  skus: Sku[];
  version: number;
  createTime?: string;
  updateTime?: string;
}

export interface SkuStock {
  skuId: Id;
  productId: Id;
  productName: string;
  specName: string;
  skuCode?: string;
  stock: number;
  lockedStock: number;
  availableStock: number;
  status: number;
  statusLabel: string;
}

export interface StockLog {
  id: Id;
  skuId: Id;
  productName: string;
  specName: string;
  changeQty: number;
  afterStock: number;
  afterLocked: number;
  reason: string;
  reasonLabel: string;
  orderId?: Id;
  operatorId?: Id;
  remark?: string;
  createTime: string;
}

/* ----------------------------- 平台端 ----------------------------- */

export interface PlatformOverview {
  shopCount: number;
  activeShopCount: number;
  pendingShipmentCount: number;
  pendingRefundCount: number;
  todayOrderCount: number;
  todayPaidAmount: number;
}

export interface PlatformShop {
  shopId: Id;
  name: string;
  contactName?: string;
  contactMobileMasked?: string;
  status: number;
  statusLabel: string;
  onSaleProductCount: number;
  pendingShipmentCount: number;
  todayPaidAmount: number;
}

/**
 * 顾客（后台「顾客管理」列表行）。
 * 单商家模式下店主看的是自己的顾客，所以手机号给全号，并带消费统计。
 */
export interface PlatformUser {
  id: Id;
  username: string;
  nickname: string;
  mobile?: string;
  email?: string;
  /** 是否店主本人（顾客列表里恒为 false） */
  merchant: boolean;
  status: number;
  statusLabel: string;
  passwordSet: boolean;
  /** 订单数（不含已取消） */
  orderCount: number;
  /** 累计消费（分） */
  paidAmount: number;
  /** 最后下单时间，从未下单为 null */
  lastOrderTime?: string | null;
  addressCount: number;
  createTime?: string;
}

/** 顾客详情里的订单行 */
export interface CustomerOrder {
  id: Id;
  orderNo: string;
  status: string;
  statusLabel: string;
  payAmount: number;
  itemCount: number;
  productSummary: string;
  coverImage?: string | null;
  createTime?: string;
}

export interface CustomerDetail {
  profile: PlatformUser;
  addresses: Address[];
  orders: CustomerOrder[];
}
