const { get, put, del } = require('../../utils/request');
const { money, imageUrl, datasetId, num } = require('../../utils/format');
const { requireCustomer, refreshCartBadge } = require('../../utils/session');

function mapItem(item, checked) {
  return Object.assign({}, item, {
    skuId: String(item.skuId),
    productId: String(item.productId),
    cover: imageUrl(item.imageUrl),
    priceText: money(item.price),
    checked: checked && item.valid !== false,
  });
}

Page({
  data: {
    loggedIn: false,
    loading: true,
    groups: [],
    selectedCount: 0,
    totalText: '¥0.00',
  },

  onShow() {
    const loggedIn = Boolean(wx.getStorageSync('token'));
    this.setData({ loggedIn });
    if (loggedIn) this.load();
  },

  onPullDownRefresh() {
    if (!this.data.loggedIn) {
      wx.stopPullDownRefresh();
      return;
    }
    this.load().finally(() => wx.stopPullDownRefresh());
  },

  load() {
    this.setData({ loading: true });
    const previous = {};
    this.data.groups.forEach((group) => {
      (group.items || []).forEach((item) => {
        previous[item.skuId] = item.checked;
      });
    });
    return get('/cart')
      .then((cart) => {
        const groups = (cart.shops || []).map((group) => ({
          shopId: String(group.shopId),
          shopName: group.shopName,
          items: (group.items || []).map((item) => mapItem(item, previous[String(item.skuId)] !== false)),
          invalidItems: (group.invalidItems || []).map((item) => mapItem(item, false)),
        }));
        this.setData({ groups, loading: false });
        this.recalc();
        refreshCartBadge();
      })
      .catch(() => this.setData({ loading: false }));
  },

  recalc() {
    let count = 0;
    let amount = 0;
    this.data.groups.forEach((group) => {
      group.items.forEach((item) => {
        if (!item.checked) return;
        count += item.quantity;
        amount += num(item.price) * item.quantity;
      });
    });
    this.setData({ selectedCount: count, totalText: money(amount) });
  },

  toggle(event) {
    const skuId = datasetId(event.currentTarget.dataset, 'sid');
    const groups = this.data.groups.map((group) => Object.assign({}, group, {
      items: group.items.map((item) => (item.skuId === skuId ? Object.assign({}, item, { checked: !item.checked }) : item)),
    }));
    this.setData({ groups });
    this.recalc();
  },

  changeQty(skuId, quantity) {
    put('/cart/quantity', { skuId, quantity }).then(() => this.load());
  },

  minus(event) {
    const skuId = datasetId(event.currentTarget.dataset, 'sid');
    const quantity = Number(event.currentTarget.dataset.qty) || 1;
    if (quantity <= 1) {
      this.removeById(skuId);
      return;
    }
    this.changeQty(skuId, quantity - 1);
  },

  plus(event) {
    const skuId = datasetId(event.currentTarget.dataset, 'sid');
    const quantity = Number(event.currentTarget.dataset.qty) || 1;
    const max = Math.min(99, Number(event.currentTarget.dataset.max) || 99);
    if (quantity >= max) {
      wx.showToast({ title: '不能超过可售数量', icon: 'none' });
      return;
    }
    this.changeQty(skuId, quantity + 1);
  },

  remove(event) {
    this.removeById(datasetId(event.currentTarget.dataset, 'sid'));
  },

  removeById(skuId) {
    del('/cart/' + skuId).then(() => this.load());
  },

  checkout() {
    const chosen = [];
    let shopId = '';
    this.data.groups.forEach((group) => {
      group.items.forEach((item) => {
        if (!item.checked) return;
        shopId = shopId || group.shopId;
        chosen.push({
          shopId: group.shopId,
          skuId: item.skuId,
          quantity: item.quantity,
          price: num(item.price),
        });
      });
    });
    if (!chosen.length) {
      wx.showToast({ title: '请先勾选商品', icon: 'none' });
      return;
    }
    const mixed = chosen.some((item) => item.shopId !== shopId);
    if (mixed) {
      wx.showToast({ title: '请按店铺分开结算', icon: 'none' });
      return;
    }
    if (!requireCustomer(shopId)) return;
    wx.setStorageSync('checkout', {
      shopId,
      items: chosen.map((item) => ({
        skuId: item.skuId,
        quantity: item.quantity,
        price: item.price,
      })),
    });
    wx.navigateTo({ url: '/pages/checkout/index' });
  },

  openProduct(event) {
    wx.navigateTo({ url: '/pages/product/index?id=' + datasetId(event.currentTarget.dataset, 'sid') });
  },

  goList() {
    wx.switchTab({ url: '/pages/list/index' });
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/index' });
  },
});
