const { post, getLastError, apiOrigin } = require('../../utils/request');
const { saveLogin } = require('../../utils/session');

Page({
  data: {
    mode: 'login',
    account: '',
    mobile: '',
    password: '',
    nickname: '',
    busy: false,
    apiOrigin,
    lastError: '',
  },

  onShow() {
    this.setData({ apiOrigin, lastError: getLastError() });
  },

  switchMode(event) {
    this.setData({ mode: event.currentTarget.dataset.mode, password: '' });
  },

  onAccount(event) { this.setData({ account: event.detail.value }); },
  onPassword(event) { this.setData({ password: event.detail.value }); },
  onMobile(event) { this.setData({ mobile: event.detail.value }); },
  onNickname(event) { this.setData({ nickname: event.detail.value }); },

  submit() {
    if (this.data.busy) return;
    const password = this.data.password || '';
    if (password.length < 6) {
      wx.showToast({ title: '密码至少 6 位', icon: 'none' });
      return;
    }
    let task;
    if (this.data.mode === 'login') {
      const account = (this.data.account || '').trim();
      if (!account) {
        wx.showToast({ title: '请输入账号', icon: 'none' });
        return;
      }
      task = post('/auth/login', { account, password });
    } else {
      const mobile = (this.data.mobile || '').trim();
      if (!/^1[3-9]\d{9}$/.test(mobile)) {
        wx.showToast({ title: '手机号不正确', icon: 'none' });
        return;
      }
      task = post('/auth/register', {
        mobile,
        password,
        nickname: (this.data.nickname || '').trim(),
      });
    }
    this.setData({ busy: true });
    task
      .then((result) => {
        saveLogin(result);
        wx.showToast({ title: '登录成功', icon: 'success' });
        setTimeout(() => {
          const pages = getCurrentPages();
          if (pages.length > 1) wx.navigateBack();
          else wx.switchTab({ url: '/pages/home/index' });
        }, 400);
      })
      .catch(() => {
        this.setData({ lastError: getLastError() });
      })
      .finally(() => this.setData({ busy: false }));
  },
});
