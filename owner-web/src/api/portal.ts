import { get } from '@/api/request';
import type { PortalOverview } from '@/types/api';

/** 仪表盘总览 */
export function fetchPortalOverview(trendDays = 14): Promise<PortalOverview> {
  return get<PortalOverview>('/portal/overview', { trendDays });
}
