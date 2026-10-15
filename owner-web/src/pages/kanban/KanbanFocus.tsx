import { useCallback, useEffect, useRef, useState } from 'react';
import { Button, Card, Col, Row, Statistic, Typography } from 'antd';
import { PauseOutlined, PlayCircleOutlined, RedoOutlined } from '@ant-design/icons';
import * as echarts from 'echarts';
import { kanbanFocusApi } from '@/api/kanban/modules';
import type { FocusToday } from '@/types/kanban';
import { toast } from '@/utils/feedback';

const { Title, Text } = Typography;

const WORK_SECONDS = 25 * 60;
const BREAK_SECONDS = 5 * 60;

export default function KanbanFocus() {
  const [mode, setMode] = useState<'work' | 'break'>('work');
  const [secondsLeft, setSecondsLeft] = useState(WORK_SECONDS);
  const [running, setRunning] = useState(false);
  const [today, setToday] = useState<FocusToday>({ todayCount: 0, todayMinutes: 0, totalCount: 0 });
  const timerRef = useRef<number | null>(null);
  const chartRef = useRef<HTMLDivElement>(null);
  const chartInstanceRef = useRef<echarts.ECharts | null>(null);

  const loadStats = useCallback(async () => {
    try {
      setToday(await kanbanFocusApi.today());
    } catch {
      // 已提示
    }
  }, []);

  useEffect(() => {
    void loadStats();
    void loadTrend();
    return () => {
      if (timerRef.current) window.clearInterval(timerRef.current);
      chartInstanceRef.current?.dispose();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loadStats]);

  const loadTrend = async () => {
    try {
      const trend = await kanbanFocusApi.trend();
      if (chartRef.current) {
        if (!chartInstanceRef.current) chartInstanceRef.current = echarts.init(chartRef.current);
        chartInstanceRef.current.setOption({
          tooltip: { trigger: 'axis' },
          grid: { left: 36, right: 16, top: 24, bottom: 28 },
          xAxis: { type: 'category', data: trend.map((t) => t.date.slice(5)) },
          yAxis: { type: 'value', name: '分钟' },
          series: [
            {
              type: 'bar',
              data: trend.map((t) => t.minutes),
              itemStyle: { color: '#ec6541', borderRadius: [6, 6, 0, 0] },
              barWidth: '50%',
            },
          ],
        });
      }
    } catch {
      // 已提示
    }
  };

  const stopTimer = () => {
    if (timerRef.current) {
      window.clearInterval(timerRef.current);
      timerRef.current = null;
    }
  };

  const start = () => {
    if (running) return;
    setRunning(true);
    timerRef.current = window.setInterval(() => {
      setSecondsLeft((s) => {
        if (s <= 1) {
          stopTimer();
          setRunning(false);
          void finish(mode);
          return mode === 'work' ? WORK_SECONDS : BREAK_SECONDS;
        }
        return s - 1;
      });
    }, 1000);
  };

  const finish = async (finishedMode: 'work' | 'break') => {
    if (finishedMode === 'work') {
      try {
        await kanbanFocusApi.add({ minutes: 25 });
        toast.success('完成一次专注，已记录 25 分钟');
        await loadStats();
        await loadTrend();
      } catch {
        // 已提示
      }
    }
    setMode((m) => (m === 'work' ? 'break' : 'work'));
  };

  const pause = () => {
    stopTimer();
    setRunning(false);
  };

  const reset = () => {
    stopTimer();
    setRunning(false);
    setSecondsLeft(mode === 'work' ? WORK_SECONDS : BREAK_SECONDS);
  };

  const switchMode = (m: 'work' | 'break') => {
    stopTimer();
    setRunning(false);
    setMode(m);
    setSecondsLeft(m === 'work' ? WORK_SECONDS : BREAK_SECONDS);
  };

  const mm = String(Math.floor(secondsLeft / 60)).padStart(2, '0');
  const ss = String(secondsLeft % 60).padStart(2, '0');
  const total = mode === 'work' ? WORK_SECONDS : BREAK_SECONDS;
  const progress = ((total - secondsLeft) / total) * 100;

  return (
    <div className="kb-page">
      <div className="kb-page__head">
        <div>
          <Title level={2} style={{ margin: 0 }}>番茄专注</Title>
          <Text type="secondary">25 分钟专注 + 5 分钟休息，保持高效</Text>
        </div>
      </div>

      <Row gutter={16}>
        <Col xs={24} md={10}>
          <Card className="kb-card" style={{ textAlign: 'center' }}>
            <div style={{ marginBottom: 16 }}>
              <Button type={mode === 'work' ? 'primary' : 'default'} onClick={() => switchMode('work')}>
                专注 25 分钟
              </Button>
              <Button style={{ marginLeft: 8 }} type={mode === 'break' ? 'primary' : 'default'} onClick={() => switchMode('break')}>
                休息 5 分钟
              </Button>
            </div>

            <div
              style={{
                width: 220,
                height: 220,
                margin: '0 auto 16px',
                borderRadius: '50%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                background: `conic-gradient(#ec6541 ${progress}%, #f0f0f0 ${progress}%)`,
              }}
            >
              <div
                style={{
                  width: 190,
                  height: 190,
                  borderRadius: '50%',
                  background: '#fff',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <span style={{ fontSize: 56, fontWeight: 700, color: '#131313', fontVariantNumeric: 'tabular-nums' }}>
                  {mm}:{ss}
                </span>
                <Text type="secondary">{mode === 'work' ? '专注中' : '休息中'}</Text>
              </div>
            </div>

            <div>
              {running ? (
                <Button icon={<PauseOutlined />} onClick={pause}>暂停</Button>
              ) : (
                <Button type="primary" icon={<PlayCircleOutlined />} onClick={start}>开始</Button>
              )}
              <Button style={{ marginLeft: 8 }} icon={<RedoOutlined />} onClick={reset}>重置</Button>
            </div>
          </Card>
        </Col>

        <Col xs={24} md={14}>
          <Card className="kb-card" style={{ marginBottom: 16 }}>
            <Row gutter={16}>
              <Col span={8}><Statistic title="今日专注" value={today.todayCount} suffix="次" /></Col>
              <Col span={8}><Statistic title="今日时长" value={today.todayMinutes} suffix="分钟" /></Col>
              <Col span={8}><Statistic title="累计专注" value={today.totalCount} suffix="次" /></Col>
            </Row>
          </Card>
          <Card className="kb-card" title="近 7 天专注时长">
            <div ref={chartRef} style={{ width: '100%', height: 240 }} />
          </Card>
        </Col>
      </Row>
    </div>
  );
}
