#!/usr/bin/env node
/**
 * 手机端静态站点服务器（/m）。
 *
 * 用途：把 owner-web 的打包产物以「手机优先」的方式单独托管，供手机访问。
 * 与桌面版 dev server（5273）互不干扰，可以同时跑。
 *
 * 行为：
 *   - GET /            → 302 到 /m（手机打开根路径就是手机版）
 *   - GET /m/**        → SPA 回落到 index.html
 *   - /api /files /actuator /v3/api-docs /doc.html /druid → 反向代理到后端 41452
 *
 * 用法：
 *   node scripts/mobile-server.mjs [--port 5280] [--backend http://127.0.0.1:41452]
 */

import http from 'node:http';
import { createReadStream, existsSync, statSync } from 'node:fs';
import { pipeline } from 'node:stream';
import { createGzip } from 'node:zlib';
import { extname, join, normalize, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = fileURLToPath(new URL('.', import.meta.url));
const DIST = resolve(__dirname, '../owner-web/dist');

/** 需要转发给后端的路径前缀 */
const PROXY_PREFIXES = ['/api', '/files', '/actuator', '/v3/api-docs', '/doc.html', '/druid'];

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.webp': 'image/webp',
  '.gif': 'image/gif',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.txt': 'text/plain; charset=utf-8',
  '.map': 'application/json; charset=utf-8',
};

function arg(name, fallback) {
  const index = process.argv.indexOf(`--${name}`);
  return index > -1 && process.argv[index + 1] ? process.argv[index + 1] : fallback;
}

const PORT = Number(process.env.MOBILE_PORT || arg('port', 5280));
const BACKEND = process.env.MOBILE_BACKEND || arg('backend', 'http://127.0.0.1:41452');

if (!existsSync(join(DIST, 'index.html'))) {
  console.error(`[mobile] 找不到前端产物：${DIST}\n请先在 owner-web 下执行 npm run build`);
  process.exit(1);
}

/** 反向代理：把请求转发到后端，保留方法 / 头 / body */
function proxy(req, res) {
  const target = new URL(req.url, BACKEND);
  const headers = { ...req.headers, host: target.host };
  // 客户端断开时同步中断后端请求，避免堆积
  const upstream = http.request(
    {
      protocol: target.protocol,
      hostname: target.hostname,
      port: target.port || 80,
      path: target.pathname + target.search,
      method: req.method,
      headers,
    },
    (upstreamRes) => {
      res.writeHead(upstreamRes.statusCode || 502, upstreamRes.headers);
      upstreamRes.pipe(res);
    },
  );

  upstream.on('error', (error) => {
    if (!res.headersSent) res.writeHead(502, { 'Content-Type': 'application/json; charset=utf-8' });
    res.end(JSON.stringify({ code: 502, msg: `后端不可用：${error.message}`, success: false }));
  });

  req.pipe(upstream);
  req.on('aborted', () => upstream.destroy());
}

function sendFile(req, res, filePath, statusCode = 200) {
  const ext = extname(filePath).toLowerCase();
  const type = MIME[ext] || 'application/octet-stream';
  const stat = statSync(filePath);

  // 文本资源走 gzip：antd chunk 1.3MB → 约 420KB，公网隧道下差别很明显
  const compressible = /\.(html|js|mjs|css|json|svg|txt|map)$/.test(ext);
  const acceptEncoding = String(req.headers['accept-encoding'] || '');
  const useGzip = compressible && stat.size > 1024 && /\bgzip\b/.test(acceptEncoding);

  const headers = {
    'Content-Type': type,
    // 带 hash 的静态资源可以长缓存，index.html 不缓存，避免发版后拿到旧壳
    'Cache-Control': filePath.endsWith('index.html') ? 'no-cache' : 'public, max-age=604800',
    Vary: 'Accept-Encoding',
  };
  if (useGzip) headers['Content-Encoding'] = 'gzip';
  else headers['Content-Length'] = stat.size;

  res.writeHead(statusCode, headers);

  if (useGzip) {
    pipeline(createReadStream(filePath), createGzip(), res, () => undefined);
    return;
  }
  createReadStream(filePath).pipe(res);
}

const server = http.createServer((req, res) => {
  const pathname = decodeURIComponent((req.url || '/').split('?')[0]);

  if (PROXY_PREFIXES.some((prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`))) {
    proxy(req, res);
    return;
  }

  // 根路径直接进手机版
  if (pathname === '/' || pathname === '/index.html') {
    res.writeHead(302, { Location: '/m' });
    res.end();
    return;
  }

  // 静态文件：命中就用文件，否则 SPA 回落
  const candidate = join(DIST, normalize(pathname).replace(/^(\.\.[/\\])+/, ''));
  if (existsSync(candidate) && statSync(candidate).isFile()) {
    sendFile(req, res, candidate);
    return;
  }

  sendFile(req, res, join(DIST, 'index.html'));
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`[mobile] 手机端站点已启动：http://127.0.0.1:${PORT}/  →  跳转 /m`);
  console.log(`[mobile] 后端：${BACKEND}`);
  console.log(`[mobile] 局域网：http://<本机内网 IP>:${PORT}`);
});
