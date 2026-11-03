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
    keyword: '',
    sort: '',
    categoryId: '',
    categoryName: '',
    rows: [],
    pageNum: 1,
    pages: 1,
    hasMore: false,
    loading: false,
    emptyText: '这个分类还没有商品',
  },

  onShow() {
    const query = wx.getStorageSync('listQuery');
    if (query) {
      wx.removeStorageSync('listQuery');
      this.setData({
        categoryId: query.categoryId || '',
        categoryName: query.name || '',
        keyword: '',
        pageNum: 1,
        rows: [],
      });
    }
    this.load(true);
  },

  onPullDownRefresh() {
    this.load(true).finally(() => wx.stopPullDownRefresh());
  },

  onInput(event) {
    this.setData({ keyword: event.detail.value });
  },

  onSearch() {
    const keyword = (this.data.keyword || '').trim();
    if (keyword && keyword.length < 2) {
      wx.showToast({ title: '至少两个字', icon: 'none' });
      return;
    }
    this.setData({ keyword, pageNum: 1 });
    this.load(true);
  },

  setSort(event) {
    this.setData({ sort: event.currentTarget.dataset.sort || '', pageNum: 1 });
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
    const keyword = (this.data.keyword || '').trim();
    return ensureShop()
      .then((shopId) => {
        const sort = this.data.sort;
        if (keyword) {
          return get('/storefront/shops/' + shopId + '/search', {
            keyword,
            sort,
            pageNum,
            pageSize: 12,
          });
        }
        const params = { sort, pageNum, pageSize: 12 };
        if (this.data.categoryId) params.categoryId = this.data.categoryId;
        return get('/storefront/shops/' + shopId + '/products', params);
      })
      .then((page) => {
        const incoming = (page.rows || []).map(decorate);
        const rows = reset ? incoming : this.data.rows.concat(incoming);
        const pages = Number(page.pages) || 1;
        this.setData({
          rows,
          pageNum,
          pages,
          hasMore: pageNum < pages,
          loading: false,
          emptyText: keyword ? '没有找到，换个词试试' : '这个分类还没有商品',
        });
      })
      .catch(() => this.setData({ loading: false }));
  },

  openProduct(event) {
    wx.navigateTo({
      url: '/pages/product/index?id=' + datasetId(event.currentTarget.dataset, 'sid'),
    });
  },
});
