const { get, post } = require('../../utils/request');
const { money, dt } = require('../../utils/format');

const TIPS = {
  PENDING_PAYMENT: '订单已提交，商家确认收款后安排发货',
  PENDING_SHIPMENT: '商家正在备货，发货后可在这里看到物流单号',
  PENDING_RECEIPT: '商品已发出，收到后请确认收货',
  FINISHED: '订单已完成',
  CANCELLED: '订单已取消',
  REFUNDING: '售后处理中',
  REFUNDED: '退款已完成',
};

Page({
  data: {
    order: null,
    summary: null,
    items: [],
    logs: [],
    refund: null,
    tip: '',
    goodsText: '',
    freightText: '',
    discountText: '',
    payText: '',
    timeText: '',
    sheet: '',
    reasons: [],
    reason: '',
    companies: [],
    company: '',
    shipNo: '',
    description: '',
    content: '',
    score: 5,
    refundType: '',
    refundTitle: '',
    canShipBack: false,
  },

  onLoad(query) {
    this.orderId = query.id || '';
  },

  onShow() {
    if (this.orderId) this.load();
  },

  load() {
    return get('/order/' + this.orderId).then((order) => {
      const summary = order.summary || {};
      const refund = order.currentRefund || null;
      this.setData({
        order,
        summary: Object.assign({}, summary, { id: String(summary.id) }),
        items: (order.items || []).map((item) => Object.assign({}, item, {
          id: String(item.id),
          priceText: money(item.price),
        })),
        logs: (order.logs || []).map((item) => Object.assign({}, item, {
          id: String(item.id),
          timeText: dt(item.createTime),
        })),
        refund,
        tip: TIPS[summary.status] || '',
        goodsText: money(summary.goodsAmount),
        freightText: money(summary.freightAmount),
        discountText: money(summary.discountAmount),
        payText: money(summary.payAmount),
        timeText: dt(summary.createTime),
        canShipBack: Boolean(refund && refund.status === 'WAITING_RETURN'),
      });
    });
  },

  closeSheet() {
    this.setData({ sheet: '' });
  },

  openCancel() {
    get('/order/options').then((options) => {
      const reasons = (options && options.cancelReasons) || ['不想要了', '信息填错', '重新下单'];
      this.setData({ sheet: 'cancel', reasons, reason: reasons[0] || '' });
    });
  },

  openRefund() {
    const type = this.data.summary.status === 'PENDING_SHIPMENT' ? 'REFUND_ONLY' : 'RETURN_REFUND';
    get('/refund/options').then((options) => {
      const reasons = type === 'REFUND_ONLY'
        ? (options.refundOnlyReasons || [])
        : (options.returnRefundReasons || []);
      this.setData({
        sheet: 'refund',
        refundType: type,
        refundTitle: type === 'REFUND_ONLY' ? '申请仅退款' : '申请退货退款',
        reasons: reasons.length ? reasons : ['不想要了', '其他'],
        reason: (reasons.length ? reasons : ['不想要了'])[0],
        description: '',
      });
    });
  },

  openReview() {
    this.setData({ sheet: 'review', score: 5, content: '' });
  },

  openShip() {
    get('/order/options').then((options) => {
      const companies = (options && options.shipCompanies) || ['顺丰', '中通', '圆通', '韵达', '申通', '京东', '邮政', '其他'];
      this.setData({ sheet: 'ship', companies, company: companies[0], shipNo: '' });
    });
  },

  pickReason(event) {
    this.setData({ reason: event.currentTarget.dataset.reason });
  },

  pickCompany(event) {
    this.setData({ company: event.currentTarget.dataset.company });
  },

  setScore(event) {
    this.setData({ score: Number(event.currentTarget.dataset.score) || 5 });
  },

  onDescription(event) { this.setData({ description: event.detail.value }); },
  onContent(event) { this.setData({ content: event.detail.value }); },
  onShipNo(event) { this.setData({ shipNo: event.detail.value }); },

  submitCancel() {
    post('/order/' + this.orderId + '/cancel', { reason: this.data.reason }).then(() => {
      this.setData({ sheet: '' });
      wx.showToast({ title: '已取消', icon: 'success' });
      this.load();
    });
  },

  submitRefund() {
    post('/refund/orders/' + this.orderId, {
      type: this.data.refundType,
      reason: this.data.reason,
      description: (this.data.description || '').trim(),
    }).then(() => {
      this.setData({ sheet: '' });
      wx.showToast({ title: '已提交', icon: 'success' });
      this.load();
    });
  },

  submitReview() {
    const content = (this.data.content || '').trim();
    if (content.length < 10) {
      wx.showToast({ title: '评价至少 10 个字', icon: 'none' });
      return;
    }
    post('/review', {
      orderId: this.orderId,
      score: this.data.score,
      content,
    }).then(() => {
      this.setData({ sheet: '' });
      wx.showToast({ title: '已评价', icon: 'success' });
      this.load();
    });
  },

  submitShip() {
    const shipNo = (this.data.shipNo || '').trim();
    if (!/^[A-Za-z0-9-]{6,40}$/.test(shipNo)) {
      wx.showToast({ title: '运单号格式不对', icon: 'none' });
      return;
    }
    post('/refund/' + this.data.refund.id + '/return-ship', {
      returnCompany: this.data.company,
      returnShipNo: shipNo,
    }).then(() => {
      this.setData({ sheet: '' });
      wx.showToast({ title: '已提交', icon: 'success' });
      this.load();
    });
  },

  confirmReceipt() {
    wx.showModal({
      title: '确认收货',
      content: '确认已经收到商品？',
      success: (res) => {
        if (!res.confirm) return;
        post('/order/' + this.orderId + '/confirm').then(() => {
          wx.showToast({ title: '已确认收货', icon: 'success' });
          this.load();
        });
      },
    });
  },

  copyNo() {
    wx.setClipboardData({ data: String(this.data.summary.orderNo) });
  },

  copyShip() {
    wx.setClipboardData({ data: String(this.data.summary.shipNo) });
  },
});
