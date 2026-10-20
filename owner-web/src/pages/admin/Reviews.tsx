import { useCallback, useEffect, useState } from 'react';
import { Button, Card, Empty, Popconfirm, Rate, Select, Space, Skeleton, Table, Tag } from 'antd';
import { DeleteOutlined } from '@ant-design/icons';
import { adminReviewApi } from '@/api/admin';
import SafeImage from '@/components/SafeImage';
import { isShopOwner, useAuthStore } from '@/store/auth';
import { toast } from '@/utils/feedback';
import { dt } from '@/utils/format';
import type { Review } from '@/types/mall';

export default function AdminReviews() {
  const shopId = useAuthStore((state) => state.currentShopId) ?? '';
  const shops = useAuthStore((state) => state.shops);
  const owner = isShopOwner(shops, shopId);

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

  const remove = async (review: Review) => {
    try {
      await adminReviewApi.remove(shopId, String(review.id));
      toast.success('评价已删除，该订单可以重新评价');
      // 删掉最后一条时回到上一页，避免停在空白页
      if (rows.length === 1 && pageNum > 1) setPageNum(pageNum - 1);
      else await load();
    } catch {
      /* 拦截器已提示（例如不是店主） */
    }
  };

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
          scroll={{ x: 'max-content' }}
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
                      <SafeImage
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
            {
              title: '操作',
              width: 100,
              render: (_, r) => (
                <Popconfirm
                  title="删除这条评价？"
                  description="用于处理广告、辱骂、泄露隐私，删掉后该订单可以重新评价"
                  okText="删除"
                  okButtonProps={{ danger: true }}
                  disabled={!owner}
                  onConfirm={() => void remove(r)}
                >
                  <Button size="small" danger icon={<DeleteOutlined />} disabled={!owner}>
                    删除
                  </Button>
                </Popconfirm>
              ),
            },
          ]}
        />
      )}

      <Card style={{ marginTop: 16 }}>
        <Tag variant="filled" className="admin-tag admin-tag--muted">
          仅可删除
        </Tag>
        <span className="mall-card__meta">
          评价的内容和评分不能改，只能整条删除。删除记录会打日志，删错了让顾客重新评价即可；店员没有删除权限。
        </span>
      </Card>
    </div>
  );
}
