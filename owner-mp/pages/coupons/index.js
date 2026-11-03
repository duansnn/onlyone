const { get, post } = require('../../utils/request');
const { money, dt, datasetId } = require('../../utils/format');
const { ensureShop } = require('../../utils/session');

function decorate(item) {
  const start = dt(item.startTime);
  const end = dt(item.endTime);
  return Object.assign({}, item, {
    id: String(item.id),
    thresholdText: money(item.thresholdAmount),
    reduceText: money(item.reduceAmount),
    timeText: start && end ? start.slice(0, 10) + ' 至 ' + end.slice(0, 10) : '',
  });
}

Page({
  data: {
    tab: 'center',
    center: [],
    mine: [],
  },

  onShow() {
    this.load();
  },

  setTab(event) {
    this.setData({ tab: event.currentTarget.dataset.tab });
  },

  load() {
    ensureShop().then((shopId) => Promise.all([
      get('/coupon/center', { shopId, pageNum: 1, pageSize: 20 }),
      get('/coupon/mine', { shopId }),
    ])).then(([centerPage, mine]) => {
      this.setData({
        center: ((centerPage && centerPage.rows) || []).map(decorate),
        mine: (mine || []).map(decorate),
      });
    });
  },

  receive(event) {
    const row = this.data.center.find((item) => item.id === datasetId(event.currentTarget.dataset, 'sid'));
    if (!row || !row.receivable) return;
    post('/coupon/' + row.id + '/receive').then(() => {
      wx.showToast({ title: '已领取', icon: 'success' });
      this.load();
    });
  },
});
