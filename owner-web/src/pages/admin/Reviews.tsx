import { useCallback, useEffect, useState } from 'react';
import { Card, Empty, Rate, Select, Space, Skeleton, Table, Tag } from 'antd';
import { adminReviewApi } from '@/api/admin';
import { useAuthStore } from '@/store/auth';
import { dt } from '@/utils/format';
import type { Review } from '@/types/mall';

export default function AdminReviews() {
  const shopId = useAuthStore((state) => state.currentShopId) ?? '';
  const [score, setScore] = useState<number | undefined>(undefined);
  const [rows, setRows] = useState<Review[]>([]);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    if (!shopId) return;
    setLoading(true);
    try {
      const page = await adminReviewApi.page(shopId, {
        score,
        pageNum,
        pageSize: 10,
      });
      setRows(page.rows ?? []);
      setTotal(page.total ?? 0);
    } catch {
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [shopId, score, pageNum]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <div>
      <h2 className="mall-section-title">顾客评价</h2>

      <Card style={{ marginBottom: 16 }}>
        <Space>
          <span className="mall-card__meta">按星级筛选</span>
          <Select
            allowClear
            style={{ width: 160 }}
            placeholder="全部星级"
            value={score}
            onChange={(value) => {
              setScore(value);
              setPageNum(1);
            }}
            options={[5, 4, 3, 2, 1].map((s) => ({ value: s, label: `${s} 星` }))}
          />
        </Space>
      </Card>

      {loading ? (
        <Skeleton active />
      ) : rows.length === 0 ? (
        <Card>
          <Empty description="还没有顾客评价" />
        </Card>
      ) : (
        <Table
          rowKey="id"
          loading={loading}
          dataSource={rows}
          pagination={{
            current: pageNum,
            pageSize: 10,
            total,
            showSizeChanger: false,
            onChange: setPageNum,
          }}
          columns={[
            { title: '顾客', dataIndex: 'nickname', width: 140 },
            {
              title: '评分',
              width: 150,
              render: (_, r) => <Rate disabled value={r.score} style={{ fontSize: 13 }} />,
            },
            {
              title: '内容',
              render: (_, r) => r.content || <span className="mall-card__meta">（未填写）</span>,
            },
            {
              title: '图片',
              width: 140,
              render: (_, r) =>
                r.images?.length ? (
                  <Space>
                    {r.images.slice(0, 3).map((img) => (
                      <img
                        key={img}
                        src={img}
                        alt=""
                        style={{ width: 32, height: 32, borderRadius: 6, objectFit: 'cover' }}
                      />
                    ))}
                  </Space>
                ) : (
                  <span className="mall-card__meta">-</span>
                ),
            },
            { title: '时间', width: 150, render: (_, r) => dt(r.createTime) },
          ]}
        />
      )}

      <Card style={{ marginTop: 16 }}>
        <Tag color="blue">只读</Tag>
        <span className="mall-card__meta">
          评价一旦提交不可修改、不可删除，后台也没有删除入口；这是为了保住评价的真实性。
        </span>
      </Card>
    </div>
  );
}
