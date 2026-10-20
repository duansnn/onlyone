import type { ReactNode } from 'react';
import type { StatusTone } from '@/utils/format';

/**
 * 状态标签。
 *
 * 后台与店面的订单/退款/商品状态统一用它：
 * 底色一律浅暖色、文字统一深色，只有左侧小圆点带语义色，
 * 这样蓝色/青色/紫色这类冷色预设不会破坏暖橙主题的整体观感。
 */
export default function StatusTag({ tone, children }: { tone: StatusTone; children: ReactNode }) {
  return (
    <span className={`status-tag status-tag--${tone}`}>
      <i aria-hidden />
      {children}
    </span>
  );
}
