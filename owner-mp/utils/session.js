const { get } = require('./request');

function ensureShop() {
  const app = getApp();
  if (app.globalData.shopReady && app.globalData.shopId) {
    return Promise.resolve(app.globalData.shopId);
  }
  return get('/storefront/shops', null, { quiet: true })
    .then((shops) => {
      const shop = shops && shops.length ? shops[0] : null;
      if (!shop) {
        wx.showModal({ title: '提示', content: '还没有营业中的店铺', showCancel: false });
        return Promise.reject(new Error('no shop'));
      }
      const shopId = String(shop.shopId);
      app.globalData.shopId = shopId;
      app.globalData.shopName = shop.shopName || '';
      app.globalData.shopReady = true;
      wx.setStorageSync('shopId', shopId);
      wx.setStorageSync('shopName', shop.shopName || '');
      return shopId;
    })
    .catch((error) => {
      const cached = wx.getStorageSync('shopId');
      if (cached) {
        app.globalData.shopId = cached;
        app.globalData.shopName = wx.getStorageSync('shopName') || '';
        app.globalData.shopReady = true;
        return cached;
      }
      if (!error || error.message !== 'no shop') {
        wx.showModal({
          title: '提示',
          content: (error && error.message) || '连不上服务器。请用开发者工具重新编译并预览，手机扫新的预览码。电脑需要开着。',
          showCancel: false,
        });
      }
      return Promise.reject(error);
    });
}

/** 未登录去登录页；店主/店员不能在自己的店下单 */
function requireCustomer(shopId) {
  const token = wx.getStorageSync('token');
  if (!token) {
    wx.navigateTo({ url: '/pages/login/index' });
    return false;
  }
  const shops = wx.getStorageSync('shops') || [];
  const mine = shops.find((item) => String(item.shopId) === String(shopId));
  if (mine) {
    const who = mine.role === 'OWNER' ? '店主' : '店员';
    wx.showModal({
      title: '提示',
      content: who + '账号不能在自己的店铺下单，请换一个顾客账号。',
      showCancel: false,
    });
    return false;
  }
  return true;
}

function refreshCartBadge() {
  const token = wx.getStorageSync('token');
  if (!token) {
    wx.removeTabBarBadge({ index: 2, fail() {} });
    return;
  }
  get('/cart/count', null, { quiet: true })
    .then((count) => {
      const n = Number(count) || 0;
      if (n > 0) {
        wx.setTabBarBadge({ index: 2, text: n > 99 ? '99+' : String(n) });
      } else {
        wx.removeTabBarBadge({ index: 2, fail() {} });
      }
    })
    .catch(() => {});
}

function saveLogin(result) {
  wx.setStorageSync('token', result.token);
  wx.setStorageSync('user', result.user || null);
  wx.setStorageSync('shops', result.shops || []);
  refreshCartBadge();
}

module.exports = {
  ensureShop,
  requireCustomer,
  refreshCartBadge,
  saveLogin,
};
