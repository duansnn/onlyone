const config = require('../config');

function num(value) {
  if (value === null || value === undefined || value === '') return 0;
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

function money(fen) {
  return '¥' + (num(fen) / 100).toFixed(2);
}

function dt(value) {
  if (!value) return '';
  return String(value).replace('T', ' ').slice(0, 16);
}

/** 商品详情里的简单 HTML 收成纯文本 */
function plainText(value) {
  if (!value) return '';
  return String(value)
    .replace(/<\s*(?:br|\/p|\/div|\/li|\/h[1-6])\s*\/?>/gi, '\n')
    .replace(/<[^>]*>/g, '')
    .replace(/&nbsp;/gi, ' ')
    .replace(/&amp;/gi, '&')
    .replace(/&lt;/gi, '<')
    .replace(/&gt;/gi, '>')
    .replace(/\n{3,}/g, '\n\n')
    .trim();
}

/**
 * 相对地址补成小程序可请求的绝对地址。
 * /files 在后端，/demo 在网站静态目录。
 */
function imageUrl(path) {
  if (!path) return '';
  const text = String(path);
  if (/^https?:\/\//.test(text)) return text;
  const normalized = text.charAt(0) === '/' ? text : '/' + text;
  const origin = normalized.indexOf('/demo/') === 0 && config.imageOrigin
    ? config.imageOrigin
    : config.apiOrigin;
  return origin + normalized;
}

/**
 * 雪花 ID 不能放进 data-xxx：微信会把纯数字收成 Number，尾数被截掉。
 * 模板里写成 data-sid="id-{{item.id}}"，这里再剥掉前缀。
 */
function datasetId(dataset, key) {
  const raw = dataset ? dataset[key] : '';
  const text = raw === undefined || raw === null ? '' : String(raw);
  return text.indexOf('id-') === 0 ? text.slice(3) : text;
}

function stars(score) {
  const n = Math.max(0, Math.min(5, num(score)));
  return '★★★★★'.slice(0, n);
}

module.exports = {
  num,
  money,
  dt,
  plainText,
  imageUrl,
  datasetId,
  stars,
};
