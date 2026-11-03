const { refreshCartBadge } = require('./utils/session');

App({
  globalData: {
    shopId: '',
    shopName: '',
    shopReady: false,
  },
  onShow() {
    refreshCartBadge();
  },
});
