const { get, post, put } = require('../../utils/request');

Page({
  data: {
    id: '',
    receiver: '',
    mobile: '',
    region: [],
    regionText: '请选择',
    detail: '',
    isDefault: false,
  },

  onLoad(query) {
    const id = query.id || '';
    this.setData({ id });
    if (!id) return;
    get('/address').then((list) => {
      const found = (list || []).find((item) => String(item.id) === id);
      if (!found) return;
      const region = [found.province || '', found.city || '', found.district || ''].filter(Boolean);
      this.setData({
        receiver: found.receiver || '',
        mobile: found.mobile || '',
        region,
        regionText: region.join(' ') || '请选择',
        detail: found.detail || '',
        isDefault: found.isDefault === 1,
      });
    });
  },

  onReceiver(event) { this.setData({ receiver: event.detail.value }); },
  onMobile(event) { this.setData({ mobile: event.detail.value }); },
  onDetail(event) { this.setData({ detail: event.detail.value }); },
  onDefault(event) { this.setData({ isDefault: event.detail.value }); },

  onRegion(event) {
    const region = event.detail.value || [];
    this.setData({ region, regionText: region.join(' ') });
  },

  save() {
    const receiver = (this.data.receiver || '').trim();
    const mobile = (this.data.mobile || '').trim();
    const detail = (this.data.detail || '').trim();
    if (!receiver) {
      wx.showToast({ title: '请填写收件人', icon: 'none' });
      return;
    }
    if (!/^1[3-9]\d{9}$/.test(mobile)) {
      wx.showToast({ title: '手机号不正确', icon: 'none' });
      return;
    }
    if (!this.data.region.length) {
      wx.showToast({ title: '请选择省市区', icon: 'none' });
      return;
    }
    if (!detail) {
      wx.showToast({ title: '请填写详细地址', icon: 'none' });
      return;
    }
    const body = {
      receiver,
      mobile,
      province: this.data.region[0] || '',
      city: this.data.region[1] || '',
      district: this.data.region[2] || '',
      detail,
      isDefault: this.data.isDefault,
    };
    const task = this.data.id
      ? put('/address', Object.assign({ id: this.data.id }, body))
      : post('/address', body);
    task.then(() => {
      wx.showToast({ title: '已保存', icon: 'success' });
      setTimeout(() => wx.navigateBack(), 400);
    });
  },
});
