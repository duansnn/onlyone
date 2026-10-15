# Owner IntelliPortal

企业级智能门户（后端多模块 Maven 工程 + 前端 React 工程）。

> 命名约定：全工程统一使用 `owner` 前缀（模块 `owner-*`、包名 `com.owner.*`、
> 应用名 `owner-server`、配置前缀 `owner.*`、容器/网络 `owner-*`、数据库 `owner`）。

| 项 | 配置 |
| --- | --- |
| JDK | 25（`java.version` / `.java-version` / 编译 `release` 均为 25） |
| Spring Boot | 4.1.0（Spring Framework 7.0.8） |
| Spring AI | 2.0.0 |
| 运行镜像 | `eclipse-temurin:25-jre`，堆 `-Xms512m -Xmx512m`，时区 `Asia/Shanghai` |
| 项目版本 | `2026.03-SNAPSHOT` |
| 启动模块 | `owner-server`（应用名 `owner-server`） |
| 服务端口 | **41452** |
| 数据库 | MySQL + Druid 1.2.28 + dynamic-datasource 4.5.0 |
| ORM | MyBatis 3.5.19 + MyBatis-Plus 3.5.16 |
| 缓存 | Redis（Redisson 4.6.1），Spring Cache 类型为 `REDIS` |
| 消息 | RocketMQ Spring 2.3.6；WebSocket 发送方式默认 `local`，可切 `redis` / `rocketmq` / `kafka` / `rabbitmq` |
| 接口文档 | Knife4j 4.5.0 + springdoc 3.0.3 |
| 定时任务 | Quartz，Job 存入 JDBC（`job-store-type=jdbc`） |
| 构建 | Maven 编译插件 3.14.0，编码 UTF-8；Lombok 1.18.46 / MapStruct 1.6.3 |
| 前端 | React 19 + Vite 8 + TypeScript 7 + Ant Design 6 + React Router 7 + Zustand 5 + Axios |

---

## 1. 模块结构

```
onlyone
├── pom.xml                          # 根 POM：版本与插件统一管理
├── mvnw / mvnw.cmd / .mvn/          # Maven Wrapper（锁定 Maven 3.9.11）
├── .java-version                    # 25
├── Dockerfile                       # eclipse-temurin:25-jre 多阶段构建
├── docker-compose.yml               # MySQL + Redis + RocketMQ + owner-server + owner-web
├── docker/rocketmq/broker.conf
├── scripts/ws-smoke-test.mjs        # WebSocket 端到端冒烟脚本
├── sql/init.sql                     # 业务表 + Quartz JDBC JobStore 表
│
├── owner-common           # 统一响应 R / 分页 / 异常 / 常量 / 工具
├── owner-framework        # 数据源、ORM、缓存、消息、WebSocket、文档、定时任务、AI
├── owner-system           # 业务示例：门户用户 CRUD + 运行状态接口
├── owner-server           # 启动模块：主类、配置、打包
└── owner-web              # 前端工程：登录、仪表盘、用户管理、WebSocket、AI 对话
```

### framework 关键类

| 能力 | 类 |
| --- | --- |
| 数据源（动态 + Druid） | `MybatisPlusConfig`、`DruidMonitorConfig` |
| ORM | `MybatisPlusConfig`（分页 / 乐观锁 / 防全表更新 / 字段自动填充） |
| 缓存 | `RedisConfig`（RedisTemplate + `RedisCacheManager`，`spring.cache.type=REDIS`） |
| 定时任务 | `QuartzConfig`、`PortalHeartbeatJob` |
| 接口文档 | `OpenApiConfig` |
| WebSocket | `WebSocketConfig`、`PortalWebSocketHandler`、`WebSocketBroadcastService` |
| WebSocket 发送策略 | `websocket/sender/*`，由 `WebSocketSenderConfiguration` 按配置装配 |
| RocketMQ | `mq/RocketMqHelper`、`websocket/sender/RocketMqWebSocketConsumer` |
| AI | `ai/AiChatService` |
| 全局异常 | `handler/GlobalExceptionHandler` |

---

## 2. 本地构建与运行

### 2.1 前置条件

- JDK 25（`JAVA_HOME` 指向 JDK 25）
- Maven 3.9+（本机若为旧版本，直接用仓库自带的 `mvnw`）
- MySQL 8.x（`localhost:3306`）、Redis（`localhost:6379`）

### 2.2 初始化数据库

```bash
mysql -uroot -p < sql/init.sql
```

脚本创建 `owner` 库、`sys_user` 表（含 4 条示例数据）以及全部 `QRTZ_*` 表。

### 2.3 数据库口令

`application-local.yml` 的默认口令为 `9999`，也可用环境变量覆盖：

```bash
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=你的密码
```

### 2.4 构建

```bash
./mvnw clean package          # 含单元测试
./mvnw clean package -DskipTests
```

产物：`owner-server/target/owner-server.jar`

### 2.5 运行

```bash
java -jar owner-server/target/owner-server.jar --spring.profiles.active=local

# 或
./mvnw -pl owner-server spring-boot:run -Dspring-boot.run.profiles=local
```

### 2.6 容器运行

```bash
docker compose up -d --build
```

会依次拉起 `owner-mysql`、`owner-redis`、`owner-rocketmq-namesrv`、`owner-rocketmq-broker`、
`owner-server`（41452）与 `owner-web`（8080）。

---

## 3. 访问入口

| 入口 | 地址 |
| --- | --- |
| Knife4j 接口文档 | http://localhost:41452/doc.html |
| Swagger UI | http://localhost:41452/swagger-ui.html |
| OpenAPI JSON | http://localhost:41452/v3/api-docs |
| Druid 监控 | http://localhost:41452/druid/login.html （账号密码见本地 `.env`） |
| 健康检查 | http://localhost:41452/actuator/health |
| 运行状态 | http://localhost:41452/api/portal/info |
| WebSocket | `ws://localhost:41452/ws/portal?clientId=demo` |
| 门户前端（dev） | http://localhost:5273 |
| 门户前端（容器） | http://localhost:8080 |

### 示例请求

```bash
curl http://localhost:41452/api/portal/info
curl "http://localhost:41452/api/portal/users?pageNum=1&pageSize=10"
curl http://localhost:41452/api/portal/users/1001          # 结果写入 Redis 缓存

curl -X POST http://localhost:41452/api/portal/users \
  -H 'Content-Type: application/json' \
  -d '{"username":"zhaoliu","nickname":"赵六","mobile":"13800000006","email":"zhaoliu@owner.com","deptName":"平台研发中心"}'

curl -X POST "http://localhost:41452/api/portal/websocket/broadcast?content=hello"
curl "http://localhost:41452/api/portal/ai/chat?prompt=你好"   # 需配置 OPENAI_API_KEY
```

### WebSocket 冒烟测试

```bash
node scripts/ws-smoke-test.mjs
```

脚本会连接 WebSocket、验证 ping/pong、在线列表，并通过 HTTP 触发一次广播验证推送链路。

---

## 4. WebSocket 发送方式

由 `owner.websocket.send-mode` 控制，默认 `local`：

| 取值 | 说明 | 依赖 |
| --- | --- | --- |
| `local` | 进程内直发，单实例 | 无（默认） |
| `redis` | Redisson `RTopic` 发布订阅 | Redisson 4.6.1 |
| `rocketmq` | RocketMQ 广播消费（BROADCASTING） | `rocketmq.name-server` |
| `kafka` | Kafka 广播（每实例独立 groupId） | `spring.kafka.bootstrap-servers` |
| `rabbitmq` | RabbitMQ Fanout Exchange | `spring.rabbitmq.*` |

无论哪种方式，最终都由 `WebSocketLocalDeliverer` 写入本机连接；
业务侧只调用 `WebSocketBroadcastService#broadcast / sendToClient`。

---

## 5. 环境 Profile

| Profile | 说明 |
| --- | --- |
| `local` | 默认。MySQL / Redis 走 `localhost`，RocketMQ 关闭，WebSocket `local` |
| `docker` | 容器环境。服务名寻址，WebSocket 切换为 `rocketmq` |

---

## 6. Spring Boot 4 适配要点（重要）

迁移到 Spring Boot 4 后有几处与 Boot 3 不同的地方，工程内已逐一处理：

1. **Jackson 3**
   Boot 4 默认 JSON 实现是 Jackson 3（`tools.jackson`，注解仍在 `com.fasterxml.jackson.annotation`）。
   工程内 JSON 工具与 Redis 序列化统一使用 Jackson 3：
   `JsonUtils` 基于 `JsonMapper`，缓存使用 `GenericJacksonJsonRedisSerializer`。
   旧写法 `spring.jackson.serialization.write-dates-as-timestamps` 在 Jackson 3 已不存在，
   会导致启动时属性绑定失败，配置中已移除。

2. **`spring-boot-starter-aop` 已移除**
   AOP 能力改为直接引入 `org.aspectj:aspectjweaver`。

3. **Druid 与动态数据源共存**
   排除 Druid 自带的 `DruidDataSourceAutoConfigure`（否则会与 dynamic-datasource 争抢
   `DataSource`），由 dynamic-datasource 接管；Druid 监控页由 `DruidMonitorConfig` 手动注册。

4. **Quartz JDBC JobStore**
   数据源由 dynamic-datasource 提供，Boot 自带的装配条件不生效，因此通过
   `SchedulerFactoryBeanCustomizer` 显式绑定数据源；
   同时**不要**在 `spring.quartz.properties` 中指定 `org.quartz.jobStore.class`，
   留空时 Spring 使用 `LocalDataSourceJobStore` 才能复用 Spring 管理的 `DataSource`
   （改成 `JobStoreTX` 会抛 `DataSource name not set`）。

5. **Knife4j 4.5.0 与 springdoc 3.0.3**
   Knife4j 4.5.0 的自动配置针对 springdoc 2.x 编写，直接启用会抛
   `NoSuchMethodError: SpringDocConfigProperties.getGroupConfigs()`。
   工程中排除了 Knife4j 的两个自动配置类，仅保留其增强 UI
   （`/doc.html` 来自静态 webjar，不依赖自动配置），接口元数据仍由 springdoc 3.0.3 生成。
   同时将 `io.swagger.core.v3:*` 统一锁定为 `2.2.47`，避免被 Knife4j 传入的旧版注解覆盖。

6. **MyBatis-Plus 3.5.9+**
   分页等 SQL 解析能力已拆分为独立构件，需显式引入 `mybatis-plus-jsqlparser`。

7. **Redis 无密码场景**
   不要保留 `spring.data.redis.password` 配置项：空字符串会让 Redisson 仍发送 `AUTH`
   导致连接失败。需要密码时设置环境变量 `SPRING_DATA_REDIS_PASSWORD`。

8. **端口覆盖**
   Spring Boot 的宽松绑定会把环境变量 `SERVER__PORT` 映射到 `server.port`。
   若所在 shell 中存在该变量（部分 IDE / CLI 会注入），会覆盖配置文件里的 41452，
   可用 `--server.port=41452` 显式指定。

---

## 7. 前端工程 owner-web

### 7.1 技术栈

| 依赖 | 版本 |
| --- | --- |
| React / React DOM | 19.3.0 |
| Vite | 8.3.4 |
| TypeScript | 7.0.2 |
| Ant Design | 6.6.6 |
| @ant-design/icons | 6.3.4 |
| React Router | 7.18.4 |
| Zustand | 5.0.15 |
| Axios | 1.20.0 |

### 7.2 目录结构

```
owner-web
├── index.html
├── vite.config.ts              # 别名 @、dev 代理、分包
├── tsconfig.json
├── Dockerfile                  # node:22 构建 → nginx:1.27 运行
├── nginx.conf                  # SPA 回落 + /api、/ws 反代到 owner-server:41452
├── .env.development            # 代理目标 http://localhost:41452
├── .env.production
└── src
    ├── main.tsx / App.tsx      # ConfigProvider(zh_CN) + antd App + Router
    ├── api
    │   ├── request.ts          # axios 实例：统一拆包 R、traceId、错误提示
    │   ├── portal.ts           # /api/portal/info、websocket、ai
    │   ├── user.ts             # /api/portal/users CRUD
    │   └── auth.ts             # 登录（复用 sys_user 查询）
    ├── hooks/useWebSocket.ts   # 握手、心跳、自动重连（指数退避）
    ├── layout/MainLayout.tsx   # 侧边栏 + 顶栏 + 内容区
    ├── pages
    │   ├── Login               # 登录页
    │   ├── Dashboard           # 运行状态 / 在线数 / 心跳次数 / 广播
    │   ├── UserManage          # 分页查询、新增、编辑、逻辑删除、推送欢迎
    │   ├── WebSocketDemo       # 连接管理、ping/online、广播、定向推送
    │   ├── AiChat              # Spring AI 对话
    │   └── NotFound
    ├── router/index.tsx        # 路由 + 登录态守卫
    ├── store/auth.ts           # zustand + persist（localStorage）
    └── types/api.ts            # 与后端 R / PageResult / SysUserVO 对齐
```

### 7.3 启动

```bash
cd owner-web
npm install
npm run dev          # http://localhost:5273
```

开发服务器会把 `/api`、`/actuator`、`/v3/api-docs`、`/doc.html`、`/ws` 代理到
`http://localhost:41452`，因此浏览器侧与后端同源，无需处理跨域。

```bash
npm run build        # 输出 dist/
npm run preview      # 预览生产构建
npm run typecheck    # 仅类型检查
```

### 7.4 登录说明

后端当前**未提供独立认证接口**（无 `/api/auth/login`，`sys_user` 也无密码字段）。
前端登录的 `src/api/auth.ts` 复用「分页查询门户用户」完成校验：按 `username`
精确匹配 `sys_user`，校验账号存在且未被停用（`status === 0`），密码暂不校验。

可用的示例账号：`admin`、`zhangsan`、`lisi`（密码任意）。

接入真实认证时只需替换 `src/api/auth.ts` 中的 `login()`，其余页面无需改动。

### 7.5 与后端的接口对应

| 页面 | 前端调用 | 后端接口 |
| --- | --- | --- |
| 登录 | `login()` | `GET /api/portal/users?username=...` |
| 仪表盘 | `fetchPortalInfo()` | `GET /api/portal/info` |
| 仪表盘广播 | `broadcastMessage()` | `POST /api/portal/websocket/broadcast` |
| 用户管理 | `pageUsers / createUser / updateUser / removeUser` | `GET/POST/PUT/DELETE /api/portal/users` |
| 用户管理推送 | `pushWelcome()` | `POST /api/portal/users/{id}/welcome` |
| WebSocket | `useWebSocket` | `ws://host:41452/ws/portal?clientId=xxx` |
| AI 对话 | `aiChat()` | `GET /api/portal/ai/chat?prompt=...` |

### 7.6 为前端补充的两处后端改动

**（1）`SysUserVO` 新增 `version` 字段**

`SysUserUpdateDTO` 依赖 `@Version` 乐观锁，但原 VO 未透出版本号，前端无法回传。
补充后「编辑」操作能真正走乐观锁校验
（并发修改时后端返回 `600 数据已被他人修改，请刷新后重试`）。

**（2）`JacksonConfig`：包装类型 `Long` 统一序列化为 JSON 字符串**

主键使用 MyBatis-Plus `IdType.ASSIGN_ID`（雪花算法），生成的 ID 为 19 位整数，
例如 `2110285652501422082`。而 JavaScript 的 `Number.MAX_SAFE_INTEGER` 只有
`9007199254740991`（16 位），浏览器在执行 `JSON.parse` 时会静默丢失精度：

```
后端返回   2110285652501422082
浏览器解析 2110285652501422000   ← 尾数被截断
PUT 回传   2110285652501422000
后端响应   600 用户不存在：2110285652501422000
```

因此 `owner-framework` 中新增了 `JacksonConfig`，注册 `Long -> String` 序列化器：

```java
@Bean
public JsonMapperBuilderCustomizer ownerLongAsStringCustomizer() {
    return builder -> builder.addModule(
            new SimpleModule("ownerLongAsStringModule")
                    .addSerializer(Long.class, ToStringSerializer.instance));
}
```

约定如下：

| 类型 | 序列化结果 | 示例 | 说明 |
| --- | --- | --- | --- |
| 包装类型 `Long` | 字符串 | `"id": "2110..."` | 雪花 ID 精度保护 |
| 基本类型 `long` | 数字 | `"total": 4`、`"timestamp": 1750000000000` | 分页总数、时间戳等小数值不受影响 |

> 注意区分 `Long.class`（装箱）与 `long.class`（原始类型）：只注册前者，
> 后者不受影响，从而避免 `PageResult.total`、`R.timestamp` 变成字符串。
>
> 前端 `src/types/api.ts` 中 `SysUser.id`、`SysUserUpdatePayload.id` 等字段
> 相应声明为 `string`，`src/api/user.ts` 的 `getUserById/removeUser/pushWelcome`
> 签名也接收 `string`。**新增其他含雪花主键的 VO 时，前端同样要用 `string` 承接。**

