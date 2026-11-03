const { get, post } = require('../../utils/request');
const { money, imageUrl, datasetId, plainText, dt, stars, num } = require('../../utils/format');
const { ensureShop, requireCustomer, refreshCartBadge } = require('../../utils/session');

Page({
  data: {
    loading: true,
    detail: null,
    skus: [],
    sku: null,
    quantity: 1,
    sellable: false,
    priceText: '',
    detailText: '',
    reviews: [],
  },

  onLoad(query) {
    this.productId = query.id || '';
    this.shopId = '';
    this.load();
  },

  load() {
    this.setData({ loading: true });
    return ensureShop()
      .then((shopId) => {
        this.shopId = shopId;
        return Promise.all([
          get('/storefront/shops/' + shopId + '/products/' + this.productId),
          get('/storefront/shops/' + shopId + '/products/' + this.productId + '/reviews', {
            pageNum: 1,
            pageSize: 5,
          }, { quiet: true }).catch(() => null),
        ]);
      })
      .then(([detail, reviewPage]) => {
        const skus = (detail.skus || []).map((sku) => {
          const sellable = sku.status === 0 && num(sku.availableStock) > 0;
          return Object.assign({}, sku, {
            id: String(sku.id),
            sellable,
            on: false,
          });
        });
        const chosen = skus.find((sku) => sku.sellable) || skus[0] || null;
        if (chosen) chosen.on = true;
        const reviews = ((reviewPage && reviewPage.rows) || []).map((item) => ({
          id: String(item.id),
          nickname: item.nickname || '顾客',
          stars: stars(item.score),
          content: item.content || '',
          timeText: dt(item.createTime),
        }));
        this.setData({
          loading: false,
          detail: Object.assign({}, detail, {
            images: (detail.images || []).map(imageUrl),
          }),
          skus,
          reviews,
          detailText: plainText(detail.detail),
        });
        this.applySku(chosen, 1);
      })
      .catch(() => this.setData({ loading: false, detail: null }));
  },

  applySku(sku, quantity) {
    const max = sku && sku.sellable ? Math.min(99, num(sku.availableStock)) : 1;
    const next = Math.max(1, Math.min(max, quantity || 1));
    const skus = this.data.skus.map((item) => Object.assign({}, item, { on: sku && item.id === sku.id }));
    this.setData({
      skus,
      sku,
      quantity: next,
      sellable: Boolean(sku && sku.sellable),
      priceText: sku ? money(sku.price) : money(this.data.detail && this.data.detail.minPrice),
    });
  },

  pickSku(event) {
    const id = datasetId(event.currentTarget.dataset, 'sid');
    const sku = this.data.skus.find((item) => item.id === id);
    if (!sku) return;
    this.applySku(sku, 1);
  },

  minus() {
    this.applySku(this.data.sku, this.data.quantity - 1);
  },

  plus() {
    this.applySku(this.data.sku, this.data.quantity + 1);
  },

  addCart() {
    const sku = this.data.sku;
    if (!sku || !sku.sellable) {
      wx.showToast({ title: '请选择有货的规格', icon: 'none' });
      return;
    }
    if (!requireCustomer(this.shopId)) return;
    post('/cart', { skuId: sku.id, quantity: this.data.quantity }).then(() => {
      wx.showToast({ title: '已加入购物车', icon: 'success' });
      refreshCartBadge();
    });
  },

  buyNow() {
    const sku = this.data.sku;
    if (!sku || !sku.sellable) {
      wx.showToast({ title: '暂时缺货', icon: 'none' });
      return;
    }
    if (!requireCustomer(this.shopId)) return;
    wx.setStorageSync('checkout', {
      shopId: String(this.data.detail.shopId),
      items: [{ skuId: sku.id, quantity: this.data.quantity, price: num(sku.price) }],
    });
    wx.navigateTo({ url: '/pages/checkout/index' });
  },
});
