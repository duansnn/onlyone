const { post } = require('../../utils/request');
const { imageUrl } = require('../../utils/format');
const { refreshCartBadge } = require('../../utils/session');

function needLogin() {
  if (wx.getStorageSync('token')) return false;
  wx.navigateTo({ url: '/pages/login/index' });
  return true;
}

Page({
  data: {
    user: null,
    avatar: '',
    initial: '',
    roleLabel: '',
  },

  onShow() {
    const user = wx.getStorageSync('user') || null;
    const shops = wx.getStorageSync('shops') || [];
    const role = shops.length ? (shops[0].roleLabel || '') : '';
    const name = user ? (user.nickname || user.username || '') : '';
    this.setData({
      user,
      avatar: user && user.avatarUrl ? imageUrl(user.avatarUrl) : '',
      initial: name ? name.slice(0, 1) : '',
      roleLabel: role,
    });
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/index' });
  },

  goOrders() {
    if (needLogin()) return;
    wx.navigateTo({ url: '/pages/orders/index' });
  },

  goAddress() {
    if (needLogin()) return;
    wx.navigateTo({ url: '/pages/address/index' });
  },

  goCoupons() {
    if (needLogin()) return;
    wx.navigateTo({ url: '/pages/coupons/index' });
  },

  logout() {
    post('/auth/logout', null, { quiet: true })
      .catch(() => {})
      .finally(() => {
        wx.removeStorageSync('token');
        wx.removeStorageSync('user');
        wx.removeStorageSync('shops');
        refreshCartBadge();
        this.setData({ user: null, roleLabel: '', avatar: '', initial: '' });
        wx.showToast({ title: '已退出', icon: 'success' });
      });
  },
});
