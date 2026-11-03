const { get } = require('../../utils/request');
const { money, dt, datasetId } = require('../../utils/format');

const FILTERS = [
  { value: '', label: '全部' },
  { value: 'PENDING_PAYMENT', label: '待确认' },
  { value: 'PENDING_SHIPMENT', label: '待发货' },
  { value: 'PENDING_RECEIPT', label: '待收货' },
  { value: 'FINISHED', label: '已完成' },
  { value: 'REFUNDING', label: '退款中' },
  { value: 'REFUNDED', label: '已退款' },
  { value: 'CANCELLED', label: '已取消' },
];

Page({
  data: {
    filters: FILTERS,
    status: '',
    rows: [],
    pageNum: 1,
    hasMore: false,
    loading: false,
  },

  onShow() {
    this.load(true);
  },

  onPullDownRefresh() {
    this.load(true).finally(() => wx.stopPullDownRefresh());
  },

  setStatus(event) {
    this.setData({ status: event.currentTarget.dataset.status || '' });
    this.load(true);
  },

  more() {
    if (!this.data.hasMore || this.data.loading) return;
    this.setData({ pageNum: this.data.pageNum + 1 });
    this.load(false);
  },

  load(reset) {
    const pageNum = reset ? 1 : this.data.pageNum;
    this.setData({ loading: true });
    const params = { pageNum, pageSize: 10 };
    if (this.data.status) params.status = this.data.status;
    return get('/order/mine', params)
      .then((page) => {
        const incoming = (page.rows || []).map((item) => Object.assign({}, item, {
          id: String(item.id),
          payText: money(item.payAmount),
          timeText: dt(item.createTime),
        }));
        const rows = reset ? incoming : this.data.rows.concat(incoming);
        const pages = Number(page.pages) || 1;
        this.setData({ rows, pageNum, hasMore: pageNum < pages, loading: false });
      })
      .catch(() => this.setData({ loading: false }));
  },

  open(event) {
    wx.navigateTo({ url: '/pages/order/index?id=' + datasetId(event.currentTarget.dataset, 'sid') });
  },
});
