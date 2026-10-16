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

/** 订单状态 → antd Tag 颜色 */
export function orderStatusColor(status: string): string {
  switch (status) {
    case 'PENDING_PAYMENT':
      return 'orange';
    case 'PENDING_SHIPMENT':
      return 'blue';
    case 'PENDING_RECEIPT':
      return 'cyan';
    case 'FINISHED':
      return 'green';
    case 'REFUNDING':
      return 'purple';
    case 'REFUNDED':
      return 'default';
    default:
      return 'default';
  }
}

export function refundStatusColor(status: string): string {
  switch (status) {
    case 'PENDING':
      return 'orange';
    case 'AGREED':
    case 'WAITING_RETURN':
    case 'RECEIVED':
      return 'blue';
    case 'SUCCESS':
      return 'green';
    case 'REJECTED':
      return 'red';
    default:
      return 'default';
  }
}

export function productStatusColor(status: string): string {
  switch (status) {
    case 'ON_SALE':
      return 'green';
    case 'DRAFT':
      return 'default';
    default:
      return 'orange';
  }
}

/** 首图兜底：没有图片时给一个浅灰占位 */
export const PLACEHOLDER =
  'data:image/svg+xml;charset=utf-8,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200"><rect width="200" height="200" fill="#F5F5F5"/><text x="100" y="105" font-size="13" fill="#B4B2A9" text-anchor="middle" font-family="sans-serif">暂无图片</text></svg>',
  );
