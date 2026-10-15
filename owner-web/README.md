# owner-web · Owner IntelliPortal 门户前端

React 19 + Vite 8 + TypeScript 7 + Ant Design 6 构建的企业级门户前端，与
`owner-server`（Spring Boot 4.1.0，端口 41452）配套。

## 技术栈

| 依赖 | 版本 | 用途 |
| --- | --- | --- |
| React / React DOM | 19.3.0 | UI 框架 |
| Vite | 8.3.4 | 构建 / 开发服务器 |
| TypeScript | 7.0.2 | 类型系统 |
| Ant Design | 6.6.6 | 组件库 |
| @ant-design/icons | 6.3.4 | 图标 |
| React Router | 7.18.4 | 路由 |
| Zustand | 5.0.15 | 状态管理（持久化登录态） |
| Axios | 1.20.0 | HTTP 客户端 |
| dayjs | 1.11.23 | 时间处理 |

## 快速开始

```bash
npm install
npm run dev        # http://localhost:5273
```

> 需要先启动后端：`java -jar ../owner-server/target/owner-server.jar --spring.profiles.active=local`

其他命令：

```bash
npm run typecheck  # 仅类型检查
npm run build      # 生产构建，产物在 dist/
npm run preview    # 预览生产构建（4173）
```

## 开发代理

`vite.config.ts` 已配置代理，浏览器侧与后端同源，无跨域问题：

| 前缀 | 目标 |
| --- | --- |
| `/api` | http://localhost:41452 |
| `/actuator` | http://localhost:41452 |
| `/v3/api-docs` | http://localhost:41452 |
| `/doc.html` | http://localhost:41452 |
| `/ws` | http://localhost:41452（`ws: true`） |

如需指向其他后端，修改 `.env.development` 中的 `VITE_BACKEND_ORIGIN`。

## 功能页面

| 路由 | 页面 | 说明 |
| --- | --- | --- |
| `/login` | 登录 | 账号校验（复用 `sys_user` 查询），示例账号 `admin` / `zhangsan` / `lisi` |
| `/dashboard` | 仪表盘 | 应用信息、WebSocket 在线数、Quartz 心跳执行次数、消息广播 |
| `/users` | 用户管理 | 分页查询、新增、编辑（乐观锁）、逻辑删除、推送欢迎消息 |
| `/websocket` | WebSocket 演示 | 连接管理、ping / online、HTTP 广播、定向推送、消息记录 |
| `/ai` | AI 对话 | 基于 Spring AI 的 `GET /api/portal/ai/chat` |

侧边栏「运维与文档」可直接跳转 Knife4j 接口文档、Druid 监控台与 Actuator 健康检查。

## 关键实现

**统一响应拆包** — `src/api/request.ts` 中的响应拦截器识别后端 `R{code,msg,data,timestamp}`：
`code === 200` 时直接返回 `data`，否则弹出错误提示并抛出 `BizError`。因此业务代码
`const page = await pageUsers(query)` 拿到的就是 `PageResult<SysUser>`。

**全局提示** — antd v6 推荐用 `App.useApp()` 获取 `message` 实例而非静态方法。
`src/utils/feedback.ts` 做了一个极轻的桥接 store，让 axios 拦截器这类非组件模块
也能复用同一套消息上下文。

**WebSocket** — `src/hooks/useWebSocket.ts` 封装了握手、`ping`/`online` 约定、
消息缓冲与指数退避自动重连（最多 5 次）。与后端
`PortalWebSocketHandler` 的协议一致：发送纯文本 `ping` 得到 `pong`，
发送 `online` 得到在线列表。

**登录态** — `src/store/auth.ts` 用 zustand `persist` 写入 localStorage；
`src/router/index.tsx` 的 `RequireAuth` 负责未登录跳转并记住来源路径。

**雪花 ID 一律用 `string` 承接（重要）** — 后端主键为 MyBatis-Plus 雪花 ID，
19 位整数，超出 JS `Number.MAX_SAFE_INTEGER`（16 位）。后端已通过
`JacksonConfig` 把包装类型 `Long` 序列化成 JSON 字符串，因此：

```ts
// src/types/api.ts
export interface SysUser {
  id: string;        // 不是 number！
  version?: number;  // 乐观锁版本号仍是数值
  // ...
}
export interface PageResult<T> {
  total: number;     // 基本类型 long，后端仍返回数字
  // ...
}
```

所有把 ID 透传给接口的函数签名（`getUserById(id: string)`、`removeUser(id: string)`、
`pushWelcome(id: string)`）也必须是 `string`。若写成 `number`，浏览器解析时会
把 `2110285652501422082` 截断成 `2110285652501422000`，随后的更新 / 删除
会因「用户不存在」而失败。新增含雪花主键的实体时请沿用同一约定。

## 容器化部署

```bash
# 在仓库根目录
docker compose up -d --build owner-web
```

镜像为 `node:22-alpine` 构建 + `nginx:1.27-alpine` 运行，Nginx 负责 SPA 回落、
静态资源缓存，并把 `/api`、`/ws`、`/doc.html`、`/druid/` 反代到 `owner-server:41452`。
访问 http://localhost:8080。

单独构建镜像：

```bash
docker build -t owner-web:2026.03.0 .
```

## 已知边界

- 后端尚未提供认证接口与密码字段，登录仅校验账号存在与启用状态，详见根目录 README 第 7.4 节。
- 列表接口返回的手机号已脱敏（`138****0001`），因此编辑表单中手机号默认为空，
  留空表示不修改（后端 MapStruct 使用 `NullValuePropertyMappingStrategy.IGNORE`）。
- AI 对话需要后端配置 `OPENAI_API_KEY` / `OPENAI_BASE_URL`，未配置时页面会给出提示。
- `src/main.tsx` 未启用 `StrictMode`：严格模式在开发环境会重复挂载组件，
  导致 WebSocket 演示页出现一次多余的重连。
