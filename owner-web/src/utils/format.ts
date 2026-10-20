/** 金额 / 数量 / 时间等展示工具 */

/** Long 包装类型可能是字符串，统一转数字 */
export function num(value: unknown): number {
  if (value === null || value === undefined || value === '') return 0;
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

/** 分 → 元，保留两位 */
export function yuan(fen: unknown): string {
  return (num(fen) / 100).toFixed(2);
}

/** 分 → 元（带 ¥ 前缀） */
export function money(fen: unknown): string {
  return `¥${yuan(fen)}`;
}

/** 分 → 元，整数时不显示小数（用于筛选/输入框回填） */
export function fenToYuanInput(fen: unknown): number {
  return Math.round(num(fen)) / 100;
}

/** 元 → 分 */
export function yuanToFen(y: unknown): number {
  const n = Number(y);
  return Number.isFinite(n) ? Math.round(n * 100) : 0;
}

/** 时间展示：2026-10-16 17:20 */
export function dt(value?: string | null): string {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 16);
}

export function d(value?: string | null): string {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 10);
}

/** 倒计时：剩余毫秒 → mm:ss */
export function countdown(expireTime?: string | null): string {
  if (!expireTime) return '';
  const end = new Date(String(expireTime).replace(' ', 'T')).getTime();
  const diff = end - Date.now();
  if (!Number.isFinite(diff) || diff <= 0) return '已超时';
  const total = Math.floor(diff / 1000);
  const m = Math.floor(total / 60);
  const s = total % 60;
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

/** 状态色语义：只决定圆点颜色，底色统一走浅暖色，避免与暖橙主题打架 */
export type StatusTone = 'confirm' | 'ship' | 'receive' | 'done' | 'after' | 'danger' | 'closed';

/** 订单状态 → 语义色 */
export function orderStatusTone(status: string): StatusTone {
  switch (status) {
    case 'PENDING_PAYMENT':
      return 'confirm';
    case 'PENDING_SHIPMENT':
      return 'ship';
    case 'PENDING_RECEIPT':
      return 'receive';
    case 'FINISHED':
      return 'done';
    case 'REFUNDING':
      return 'after';
    default:
      return 'closed';
  }
}

/** 退款状态 → 语义色 */
export function refundStatusTone(status: string): StatusTone {
  switch (status) {
    case 'PENDING':
      return 'confirm';
    case 'AGREED':
    case 'WAITING_RETURN':
    case 'RECEIVED':
      return 'ship';
    case 'SUCCESS':
      return 'done';
    case 'REJECTED':
      return 'danger';
    default:
      return 'closed';
  }
}

/** 商品状态 → 语义色 */
export function productStatusTone(status: string): StatusTone {
  switch (status) {
    case 'ON_SALE':
      return 'done';
    case 'DRAFT':
      return 'closed';
    default:
      return 'confirm';
  }
}

/** 首图兜底：没有图片（或图片地址失效）时给一张暖色底 + 相框图标的占位，不再是死灰块 */
export const PLACEHOLDER =
  'data:image/svg+xml;charset=utf-8,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="240" height="240" viewBox="0 0 240 240">' +
      '<defs><linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">' +
      '<stop offset="0" stop-color="#FCF8F3"/><stop offset="1" stop-color="#F0E7DC"/></linearGradient></defs>' +
      '<rect width="240" height="240" fill="url(#bg)"/>' +
      '<g fill="none" stroke="#DFD1BF" stroke-width="4" stroke-linecap="round" stroke-linejoin="round">' +
      '<rect x="86" y="82" width="68" height="56" rx="8"/>' +
      '<circle cx="104" cy="100" r="6"/>' +
      '<path d="M88 132l20-18 14 13 12-10 18 15"/>' +
      '</g>' +
      '<text x="120" y="172" font-size="14" fill="#BCAB98" text-anchor="middle" font-family="PingFang SC, Noto Sans SC, sans-serif">暂无图片</text>' +
      '</svg>',
  );
