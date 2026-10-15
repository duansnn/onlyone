import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Button, Card, Col, InputNumber, Row, Space, Spin, Typography } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import * as echarts from 'echarts';
import { useNavigate, useParams } from 'react-router-dom';
import { kanbanProjectApi, kanbanStatsApi } from '@/api/kanban';
import type { KanbanProject, StatsSummary } from '@/types/kanban';

const { Title, Text } = Typography;

/** 看板统计页（ECharts） */
export default function KanbanStats() {
  const { projectId } = useParams();
  const navigate = useNavigate();

  const [project, setProject] = useState<KanbanProject | null>(null);
  const [summary, setSummary] = useState<StatsSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [days, setDays] = useState(14);

  const pieRef = useRef<HTMLDivElement>(null);
  const trendRef = useRef<HTMLDivElement>(null);
  const pieChart = useRef<echarts.ECharts | null>(null);
  const trendChart = useRef<echarts.ECharts | null>(null);

  const load = useCallback(async () => {
    if (!projectId) return;
    setLoading(true);
    try {
      const [projectList, stats] = await Promise.all([
        kanbanProjectApi.list(),
        kanbanStatsApi.summary(projectId, days),
      ]);
      setProject(projectList.find((p) => p.id === projectId) ?? null);
      setSummary(stats);
    } catch {
      // 错误已提示
    } finally {
      setLoading(false);
    }
  }, [projectId, days]);

  useEffect(() => {
    load();
  }, [load]);

  // 渲染饼图
  useEffect(() => {
    if (!summary || !pieRef.current) return;
    if (!pieChart.current) {
      pieChart.current = echarts.init(pieRef.current);
    }
    pieChart.current.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [
        {
          name: '任务状态',
          type: 'pie',
          radius: ['40%', '68%'],
          avoidLabelOverlap: false,
          itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
          label: { show: false },
          emphasis: {
            label: { show: true, fontSize: 16, fontWeight: 'bold' },
          },
          data: [
            { value: summary.todoCount, name: '待办', itemStyle: { color: '#ec6541' } },
            { value: summary.doingCount, name: '进行中', itemStyle: { color: '#3a6b8a' } },
            { value: summary.doneCount, name: '已完成', itemStyle: { color: '#f9815e' } },
          ],
        },
      ],
    });
  }, [summary]);

  // 渲染趋势图
  useEffect(() => {
    if (!summary || !trendRef.current) return;
    if (!trendChart.current) {
      trendChart.current = echarts.init(trendRef.current);
    }
    const dates = summary.trend.map((t) => t.date);
    const newCounts = summary.trend.map((t) => t.newCount);
    const doneCounts = summary.trend.map((t) => t.doneCount);

    trendChart.current.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['新增任务', '完成任务'], bottom: 0 },
      grid: { left: 40, right: 20, top: 30, bottom: 40 },
      xAxis: {
        type: 'category',
        data: dates,
        boundaryGap: false,
        axisLabel: { rotate: dates.length > 14 ? 45 : 0 },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
      },
      series: [
        {
          name: '新增任务',
          type: 'line',
          smooth: true,
          data: newCounts,
          itemStyle: { color: '#ec6541' },
          areaStyle: { color: 'rgba(28, 28, 30, 0.08)' },
        },
        {
          name: '完成任务',
          type: 'line',
          smooth: true,
          data: doneCounts,
          itemStyle: { color: '#3a6b8a' },
          areaStyle: { color: 'rgba(107, 143, 78, 0.10)' },
        },
      ],
    });
  }, [summary]);

  // 响应式 resize
  useEffect(() => {
    const handler = () => {
      pieChart.current?.resize();
      trendChart.current?.resize();
    };
    window.addEventListener('resize', handler);
    return () => {
      window.removeEventListener('resize', handler);
      pieChart.current?.dispose();
      trendChart.current?.dispose();
      pieChart.current = null;
      trendChart.current = null;
    };
  }, []);

  const statCards = useMemo(() => {
    if (!summary) return [];
    return [
      { label: '任务总数', value: summary.total, color: '#ffffff' },
      { label: '待办', value: summary.todoCount, color: 'linear-gradient(135deg, #f9815e, #ec6541)' },
      { label: '进行中', value: summary.doingCount, color: '#3a6b8a' },
      { label: '已完成', value: summary.doneCount, color: '#f9815e' },
      { label: '逾期', value: summary.overdueCount, color: '#d64545' },
    ];
  }, [summary]);

  return (
    <div className="kb-page kb-stats">
      <div className="kb-page__head">
        <div>
          <Button type="link" style={{ padding: 0 }} onClick={() => navigate(`/kanban/projects/${projectId}/board`)}>
            ← 返回看板
          </Button>
          <Title level={2} style={{ margin: '4px 0 0' }}>
            {project?.name ?? '统计'} · 数据看板
          </Title>
        </div>
        <Space>
          <Text type="secondary">趋势天数：</Text>
          <InputNumber min={1} max={90} value={days} onChange={(v) => setDays(v ?? 14)} style={{ width: 100 }} />
          <Button icon={<ReloadOutlined />} onClick={load} />
        </Space>
      </div>

      {loading ? (
        <div className="kb-loading">
          <Spin />
        </div>
      ) : (
        <>
          <Row gutter={[14, 14]}>
            {statCards.map((card) => (
              <Col key={card.label} xs={12} sm={8} lg={4} flex="1 1 0">
                <div className="kb-stat-card" style={{ background: card.color }}>
                  <span className="kb-stat-card__label">{card.label}</span>
                  <span className="kb-stat-card__value">{card.value}</span>
                </div>
              </Col>
            ))}
          </Row>

          <Row gutter={[14, 14]} style={{ marginTop: 14 }}>
            <Col xs={24} lg={10}>
              <Card className="kb-card" title="任务状态分布" style={{ height: '100%' }}>
                <div ref={pieRef} style={{ height: 360 }} />
              </Card>
            </Col>
            <Col xs={24} lg={14}>
              <Card className="kb-card" title="近 N 天任务趋势" style={{ height: '100%' }}>
                <div ref={trendRef} style={{ height: 360 }} />
              </Card>
            </Col>
          </Row>
        </>
      )}
    </div>
  );
}
