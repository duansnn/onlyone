const { get, post } = require('../../utils/request');
const { money, datasetId, num } = require('../../utils/format');

Page({
  data: {
    shopId: '',
    shopName: '',
    lines: [],
    addresses: [],
    addressId: '',
    address: null,
    coupons: [],
    couponUserId: '',
    couponLabel: '不使用',
    goodsText: '¥0.00',
    freightText: '¥0.00',
    payText: '¥0.00',
    message: '',
    invalidText: '',
    remark: '',
    sheet: '',
    canSubmit: false,
    busy: false,
  },

  onLoad() {
    const payload = wx.getStorageSync('checkout');
    if (!payload || !payload.items || !payload.items.length) {
      wx.showToast({ title: '没有要结算的商品', icon: 'none' });
      setTimeout(() => wx.navigateBack(), 400);
      return;
    }
    this.payload = payload;
    this.setData({ shopId: String(payload.shopId) });
  },

  onShow() {
    if (!this.payload) return;
    this.loadAddresses().then(() => this.preview());
  },

  loadAddresses() {
    return get('/address').then((list) => {
      const addresses = (list || []).map((item) => Object.assign({}, item, { id: String(item.id) }));
      let addressId = this.data.addressId;
      if (!addresses.some((item) => item.id === addressId)) {
        const fallback = addresses.find((item) => item.isDefault === 1) || addresses[0];
        addressId = fallback ? fallback.id : '';
      }
      const address = addresses.find((item) => item.id === addressId) || null;
      this.setData({ addresses, addressId, address });
    });
  },

  preview() {
    const couponUserId = this.data.couponUserId || null;
    return post('/order/preview', {
      shopId: this.data.shopId,
      items: this.payload.items,
      couponUserId,
    }).then((preview) => {
      const lines = (preview.items || []).map((item) => ({
        skuId: String(item.skuId),
        quantity: item.quantity,
        price: num(item.price),
        productName: item.productName,
        specName: item.specName,
        priceText: money(item.price),
      }));
      this.payload.items = lines.map((item) => ({
        skuId: item.skuId,
        quantity: item.quantity,
        price: item.price,
      }));
      const coupons = (preview.usableCoupons || []).map((item) => Object.assign({}, item, {
        id: String(item.id),
        thresholdText: money(item.thresholdAmount),
        reduceText: money(item.reduceAmount),
      }));
      const invalid = preview.invalidItems || [];
      const couponUserIdNext = preview.couponUserId ? String(preview.couponUserId) : '';
      this.setData({
        shopName: preview.shopName || '',
        lines,
        coupons,
        couponUserId: couponUserIdNext,
        couponLabel: preview.couponName || (coupons.length ? coupons.length + ' 张可用' : '不使用'),
        goodsText: money(preview.goodsAmount),
        freightText: money(preview.freightAmount),
        payText: money(preview.payAmount),
        message: preview.message || '',
        invalidText: invalid.length ? '以下商品已失效：' + invalid.map((item) => item.productName).join('、') : '',
        canSubmit: lines.length > 0 && !invalid.length && Boolean(this.data.addressId),
      });
    });
  },

  onRemark(event) {
    this.setData({ remark: event.detail.value });
  },

  pickAddress() {
    this.setData({ sheet: 'address' });
  },

  chooseAddress(event) {
    const addressId = datasetId(event.currentTarget.dataset, 'sid');
    const address = this.data.addresses.find((item) => item.id === addressId) || null;
    this.setData({
      addressId,
      address,
      sheet: '',
      canSubmit: this.data.lines.length > 0 && !this.data.invalidText && Boolean(addressId),
    });
  },

  addAddress() {
    this.setData({ sheet: '' });
    wx.navigateTo({ url: '/pages/address-edit/index' });
  },

  openCoupons() {
    this.setData({ sheet: 'coupon' });
  },

  chooseCoupon(event) {
    const couponUserId = datasetId(event.currentTarget.dataset, 'sid');
    this.data.couponUserId = couponUserId;
    this.setData({ couponUserId, sheet: '' });
    this.preview();
  },

  closeSheet() {
    this.setData({ sheet: '' });
  },

  submit() {
    if (!this.data.canSubmit || this.data.busy) return;
    if (!this.data.addressId) {
      wx.showToast({ title: '请选择收货地址', icon: 'none' });
      return;
    }
    this.setData({ busy: true });
    post('/order/submit', {
      shopId: this.data.shopId,
      items: this.payload.items,
      addressId: this.data.addressId,
      couponUserId: this.data.couponUserId || null,
      buyerRemark: (this.data.remark || '').trim(),
    })
      .then((result) => {
        wx.removeStorageSync('checkout');
        wx.showToast({ title: '下单成功', icon: 'success' });
        setTimeout(() => {
          wx.redirectTo({ url: '/pages/order/index?id=' + result.orderId });
        }, 400);
      })
      .catch(() => this.preview())
      .finally(() => this.setData({ busy: false }));
  },
});
