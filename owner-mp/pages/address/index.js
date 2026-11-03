const { get, put, del } = require('../../utils/request');
const { datasetId } = require('../../utils/format');

Page({
  data: { rows: [] },

  onShow() {
    this.load();
  },

  load() {
    get('/address').then((list) => {
      this.setData({
        rows: (list || []).map((item) => Object.assign({}, item, { id: String(item.id) })),
      });
    });
  },

  create() {
    wx.navigateTo({ url: '/pages/address-edit/index' });
  },

  edit(event) {
    wx.navigateTo({ url: '/pages/address-edit/index?id=' + datasetId(event.currentTarget.dataset, 'sid') });
  },

  makeDefault(event) {
    put('/address/' + datasetId(event.currentTarget.dataset, 'sid') + '/default').then(() => this.load());
  },

  remove(event) {
    const id = datasetId(event.currentTarget.dataset, 'sid');
    wx.showModal({
      title: '删除地址',
      content: '删除后不可恢复',
      success: (res) => {
        if (!res.confirm) return;
        del('/address/' + id).then(() => this.load());
      },
    });
  },
});
