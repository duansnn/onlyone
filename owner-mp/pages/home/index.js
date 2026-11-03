const { get } = require('../../utils/request');
const { money, imageUrl, datasetId } = require('../../utils/format');
const { ensureShop } = require('../../utils/session');

function decorate(product) {
  return Object.assign({}, product, {
    id: String(product.id),
    cover: imageUrl(product.coverImage),
    priceText: money(product.minPrice),
  });
}

Page({
  data: {
    loading: true,
    home: null,
  },

  onShow() {
    this.load();
  },

  onPullDownRefresh() {
    const app = getApp();
    app.globalData.shopReady = false;
    this.load().finally(() => wx.stopPullDownRefresh());
  },

  load() {
    this.setData({ loading: true });
    return ensureShop()
      .then((shopId) => get('/storefront/shops/' + shopId + '/home'))
      .then((home) => {
        const categories = (home.categories || []).map((item) => Object.assign({}, item, { id: String(item.id) }));
        const products = (home.products || []).map(decorate);
        wx.setNavigationBarTitle({ title: home.shopName || '日用铺' });
        this.setData({
          loading: false,
          home: Object.assign({}, home, { categories, products }),
        });
      })
      .catch(() => this.setData({ loading: false }));
  },

  openCategory(event) {
    wx.setStorageSync('listQuery', {
      categoryId: datasetId(event.currentTarget.dataset, 'sid'),
      name: event.currentTarget.dataset.name || '',
    });
    wx.switchTab({ url: '/pages/list/index' });
  },

  openAll() {
    wx.setStorageSync('listQuery', { categoryId: '', name: '' });
    wx.switchTab({ url: '/pages/list/index' });
  },

  openProduct(event) {
    wx.navigateTo({
      url: '/pages/product/index?id=' + datasetId(event.currentTarget.dataset, 'sid'),
    });
  },
});
