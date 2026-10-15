import { kanbanGet } from './request';
import type { StatsSummary } from '@/types/kanban';

/** 看板统计接口：/api/stats */
export const kanbanStatsApi = {
  /** 统计汇总：状态分布 + 逾期 + 近 N 天趋势 */
  summary(projectId: string, days?: number): Promise<StatsSummary> {
    return kanbanGet<StatsSummary>('/stats/summary', { projectId, days });
  },
};
