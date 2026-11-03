const config = require('../config');

/** 最近一次请求失败的原始错误，登录页会显示出来，方便定位是域名校验还是网络不通 */
let lastError = '';

function showError(message) {
  const text = message || '请求失败';
  wx.showModal({
    title: '提示',
    content: text,
    showCancel: false,
  });
}

function networkMessage(err) {
  const detail = err && err.errMsg ? String(err.errMsg) : '';
  if (/domain list|合法域名/.test(detail)) {
    return '微信拦住了接口。开发者工具里的勾选只管电脑。请点手机右上角「···」，打开「开发调试」，退出小程序后再进入。';
  }
  return '连不上服务器。请用开发者工具重新编译并预览，手机扫新的预览码。电脑需要开着。';
}

/** 把原始错误附在弹窗末尾，真机上只看文案分不清是域名校验还是断网 */
function withDetail(message, err) {
  const detail = err && err.errMsg ? String(err.errMsg) : '';
  return detail ? message + '\n\n原始错误：' + detail : message;
}

/**
 * 调用 owner-server。成功时拆掉统一响应，只返回 data。
 * options.quiet 为 true 时不弹错误，也不因 401 跳登录。
 */
function request(method, url, data, options) {
  const opts = options || {};
  const token = wx.getStorageSync('token');
  return new Promise((resolve, reject) => {
    wx.request({
      url: config.apiOrigin + '/api' + url,
      method,
      data: data == null ? {} : data,
      header: {
        'Content-Type': 'application/json',
        Authorization: token ? 'Bearer ' + token : '',
      },
      success(res) {
        const body = res.data || {};
        if (typeof body.code !== 'number') {
          const message = '服务返回异常（HTTP ' + res.statusCode + '）';
          if (!opts.quiet) showError(message);
          reject(new Error(message));
          return;
        }
        if (body.code === 200) {
          resolve(body.data);
          return;
        }
        const message = body.msg || '请求失败（' + body.code + '）';
        if (body.code === 401) {
          wx.removeStorageSync('token');
          wx.removeStorageSync('user');
          wx.removeStorageSync('shops');
          if (!opts.quiet) {
            const pages = getCurrentPages();
            const current = pages.length ? pages[pages.length - 1].route : '';
            if (current !== 'pages/login/index') {
              wx.navigateTo({ url: '/pages/login/index' });
            }
          }
        } else if (!opts.quiet) {
          showError(message);
        }
        reject(new Error(message));
      },
      fail(err) {
        const detail = err && err.errMsg ? String(err.errMsg) : '';
        lastError = detail || '未知错误';
        const message = withDetail(networkMessage(err), err);
        const error = new Error(message);
        error.errMsg = err && err.errMsg;
        if (!opts.quiet) showError(message);
        reject(error);
      },
    });
  });
}

module.exports = {
  get: (url, data, options) => request('GET', url, data, options),
  post: (url, data, options) => request('POST', url, data, options),
  put: (url, data, options) => request('PUT', url, data, options),
  del: (url, data, options) => request('DELETE', url, data, options),
  networkMessage,
  getLastError: () => lastError,
  apiOrigin: config.apiOrigin,
};
