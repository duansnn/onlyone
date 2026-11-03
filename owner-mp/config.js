/**
 * 本地调试地址。手机要等正式上线、配好 https 域名后再改这里。
 */
module.exports = {
  apiOrigin: 'http://127.0.0.1:41452',
  // 留空：/demo/* 的回退到 apiOrigin。演示图已复制到后端 static/demo，
  // 同一个域名就能取到，不再依赖 owner-web 的 5273/4173。
  imageOrigin: '',
};
