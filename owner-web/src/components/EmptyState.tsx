import type { ReactNode } from 'react';
import { Button } from 'antd';

interface EmptyStateProps {
  /** 主文案，例如「还没有订单」 */
  title?: string;
  /** 副文案，补充说明，例如「去上架几个商品，让顾客看得到」 */
  description?: string;
  /** 可选：一个主操作按钮 */
  action?: { label: string; onClick: () => void };
  /** 可选：插入的图标，默认用品牌暖橙插画 */
  icon?: ReactNode;
  /** 内边距，紧凑场景可调小 */
  compact?: boolean;
}

/**
 * 统一空状态：品牌暖橙插画 + 主/副文案 + 可选操作。
 * 后台各列表、表格页在「无数据」时用它，替代 antd 默认的灰底小图。
 */
export default function EmptyState({ title, description, action, icon, compact }: EmptyStateProps) {
  return (
    <div className={`empty-state${compact ? ' empty-state--compact' : ''}`}>
      <div className="empty-state__art">{icon ?? <DefaultArt />}</div>
      {title && <div className="empty-state__title">{title}</div>}
      {description && <div className="empty-state__desc">{description}</div>}
      {action && (
        <Button type="primary" className="empty-state__action" onClick={action.onClick}>
          {action.label}
        </Button>
      )}
    </div>
  );
}

/** 内置品牌插画：一个打开的盒子，暖橙 + 米白，呼应「日用铺」 */
function DefaultArt() {
  return (
    <svg width="120" height="96" viewBox="0 0 120 96" fill="none" xmlns="http://www.w3.org/2000/svg">
      <ellipse cx="60" cy="82" rx="44" ry="7" fill="#f0e9df" />
      <rect x="28" y="48" width="64" height="34" rx="6" fill="#fff2ec" />
      <rect x="28" y="48" width="64" height="34" rx="6" stroke="#ffd6bf" strokeWidth="2" />
      <path d="M34 48 L52 34 L68 34 L86 48 Z" fill="#ffe3d6" />
      <path d="M34 48 L52 34 L68 34 L86 48" stroke="#ffb893" strokeWidth="2" strokeLinejoin="round" />
      <path d="M60 34 L60 62" stroke="#ffb893" strokeWidth="2" strokeLinecap="round" />
      <circle cx="46" cy="64" r="3" fill="#ff5a1f" />
      <circle cx="60" cy="64" r="3" fill="#f0a202" />
      <circle cx="74" cy="64" r="3" fill="#ff5a1f" />
    </svg>
  );
}
