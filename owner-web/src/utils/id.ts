/**
 * 提交幂等键。
 *
 * 进结算页时生成一次，随下单请求发到后端；后端用 Redis + `order_info.uk_order_idem`
 * 唯一索引挡住重复提交。连点、弱网重发、超时重试都会带同一个值，所以只会成一笔单。
 *
 * `crypto.randomUUID()` 只在安全上下文（https / localhost）可用，
 * 用 http + 局域网 IP 打开时会没有，降级成时间戳 + 随机串。
 */
export function newRequestId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`;
}
