# Owner Mall · 日用铺电商系统

一个单店（可扩展为多店）日用百货电商系统。后端为多模块 Maven 工程，前端为 React 工程。

需求以 [`docs/电商系统规划.md`](docs/电商系统规划.md) 为唯一来源，改代码前先读它。

> 命名约定：全工程统一 `owner` 前缀（模块 `owner-*`、包名 `com.owner.*`、
> 应用名 `owner-server`、配置前缀 `owner.*`、容器/网络 `owner-*`、数据库 `owner`）。
> 品牌展示名统一写作 **Owner Mall**（中文全称「日用铺电商系统」）。

| 项 | 配置 |
| --- | --- |
| JDK | 25（`java.version` / `.java-version` / 编译 `release` 均为 25） |
| Spring Boot | 4.1.0（Spring Framework 7） |
| 运行镜像 | `eclipse-temurin:25-jre`，堆 `-Xms512m -Xmx512m`，时区 `Asia/Shanghai` |
| 项目版本 | `2026.03-SNAPSHOT` |
| 启动模块 | `owner-server`（应用名 `owner-server`） |
| 服务端口 | **41452** |
| 数据库 | MySQL 8.x + Druid 1.2.28 + dynamic-datasource 4.5.0 |
| ORM | MyBatis 3.5.19 + MyBatis-Plus 3.5.16（分页 / 乐观锁 / 防全表更新 / 字段自动填充） |
| 缓存 | Redis + Spring Cache（`spring.cache.type=redis`，Spring Data Redis 4 的 Jackson 3 序列化） |
| 鉴权 | 自实现 HS256 JWT（JDK `Mac` + Base64，不引 jjwt）+ BCrypt 口令 |
| 接口文档 | Knife4j 4.5.0 + springdoc 3.0.3 |
| 定时任务 | Quartz + JDBC JobStore（`OwnerScheduler`） |
| 构建 | Maven 编译插件 3.14.0，UTF-8；Lombok 1.18.46 / MapStruct 1.6.3 |
| 前端 | React 19 + Vite + TypeScript + Ant Design 6（**尚未迁移到电商，见 §9**） |

---

## 1. 模块结构

```
onlyone
├── pom.xml                          # 根 POM：版本与插件统一管理
├── mvnw / mvnw.cmd / .mvn/          # Maven Wrapper（锁定 Maven 3.9.11）
├── Dockerfile                       # eclipse-temurin:25-jre 多阶段构建
├── docker-compose.yml               # MySQL + Redis + owner-server + owner-web
├── docs/电商系统规划.md              # PRD（唯一需求来源）
├── sql/init.sql                     # 建库 + sys_user + Quartz QRTZ_* 表
├── sql/mall.sql                     # 电商业务表（17 张）
├── scripts/smoke-mall.py            # 主流程端到端冒烟（61 项）
├── scripts/smoke-platform-users.py  # 平台用户管理冒烟（13 项）
│
├── owner-common           # 统一响应 R / 分页 / 异常 / 常量 / 工具
├── owner-framework        # 数据源、ORM、缓存、接口文档、定时任务、全局异常
├── owner-system           # 业务：全站账号 + com.owner.system.mall 电商
├── owner-server           # 启动模块：主类、配置、打包
└── owner-web              # 前端工程（仍是旧门户，待重做）
```

### framework 关键类

| 能力 | 类 |
| --- | --- |
| 数据源（动态 + Druid） | `DruidMonitorConfig` |
| ORM | `MybatisPlusConfig`（分页 / 乐观锁 / 防全表更新 / 字段自动填充） |
| 缓存 | `RedisConfig`（`RedisTemplate` + `CacheManager`） |
| 定时任务 | `QuartzConfig`（只做数据源绑定，业务任务由业务模块注册） |
| 接口文档 | `OpenApiConfig` |
| 全局异常 | `handler/GlobalExceptionHandler` |
| 跨域 / 链路追踪 | `WebMvcConfig`、`web/TraceIdInterceptor` |
| 线程池 | `ThreadPoolConfig`（`owner-async-` / `owner-scheduling-`） |
| JSON | `JacksonConfig`（包装类型 `Long` → 字符串，见 §5.2） |

---

## 2. 领域模型：只有两种长期实体

**用户** 和 **店铺**。商户不是第三套账号体系——一个人管哪家店由 `shop_member` 决定。

```
sys_user ──┬── shop_member ──┐            shop ──┬── category（两级）
           │  (OWNER/STAFF)  │                    ├── product ── product_image
           │                 └────────────────────┤              └── product_sku
           │                                      ├── coupon ── coupon_user
           ├── user_address                       ├── order_info ─┬── order_item
           ├── cart_item                          │               ├── order_log
           └── review                             │               └── payment
                                                  │       refund ─┘
                                                  └── stock_log
```

三种身份，一个登录入口，按身份路由：

| 身份 | 判定 | 能做什么 |
| --- | --- | --- |
| 顾客 | 任意注册用户 | 逛店、下单、售后、评价、领券 |
| 店主 / 店员 | `shop_member.role = OWNER / STAFF` | 管自己那家店的商品、库存、订单、售后、优惠券 |
| 平台超管 | `sys_user.platform_admin = 1`（koko） | 建店、管店铺状态、管全站用户 |

启动引导 `MallDataInitializer` 首次启动会自动补齐：

- `koko` 设为平台超管并写入 BCrypt 口令（默认 `koko@2026`）
- 店主账号 `shopadmin`（默认 `shop123`），并加入「日用铺」为 `OWNER`
- 店铺「日用铺」
- 5 组一级类目 + 每组 3 个二级类目（共 20 个）
- 店铺发货地址（售后退货默认用它，店主应在「店铺资料」里改成真实地址）

---

## 3. 业务规则（写代码前必须遵守）

1. **金额一律用「分」**，`BIGINT` 整数，**不出现浮点**。展示时才除以 100。
2. **雪花 ID 一律序列化成字符串** —— 见 §5.2。
3. **逻辑删除** 用 `del_flag`；**有订单引用的商品 / SKU 只停用，不物理删除**。
4. **列表查询必须带 `shop_id`**，不能跨店。
5. **防超卖** 用带条件的原子 `UPDATE`（`ProductSkuMapper`），不做「先查再改」：
   可售量 = `stock - locked_stock`。
6. **库存流水** 每次变动都记 `stock_log`（`INIT/MANUAL/LOCK/PAY/CANCEL/RETURN`）。
7. **支付必须经 `PaymentGateway`**：没有商户号时 `DisabledPaymentGateway` 返回不可支付，
   订单**停在待付款**，**不提供「我已付款」按钮**，也不会假装成功。
8. **退款同理**：通道未开通时退款发起失败并**回滚事务**，订单留在「退款中」，
   绝不出现「钱没退却显示已退款」。
9. **售后按整单**，不做部分退款。申请时订单原状态记在 `order_log`
   （`to_status=REFUNDING` 那条的 `from_status`），拒绝时反查恢复，
   **不要**给 `refund` 表加字段。
10. **一句话只对应一个订单状态**（`order_info.status`），不在多个字段里各存一份。
11. **评价一笔订单一条**，提交后不可改不可删；店铺后台只读，无删除入口。
12. **账号与口令**：口令只用 BCrypt；连续 5 次失败锁 15 分钟（Redis 计数）；
    接口不泄露账号是否存在，统一返回「账号或密码不正确」；手机号在列表里打星。

### 订单状态机

```
待付款 ──支付成功──▶ 待发货 ──发货──▶ 待收货 ──确认收货/超 7 天──▶ 已完成
   │                    │                  │                        │
   │超 30 分钟           │仅退款              │退货退款（7 天内）        │退货退款（7 天内）
   ▼                    ▼                  ▼                        ▼
 已取消                退款中 ──同意──▶ 已退款（库存退回）      退款中 ──同意──▶ 待退货
                                                                    ──顾客填运单──▶
                                                                    待收货(退货) ──店主确认──▶ 已退款
```

任一环节**拒绝售后**，订单回到申请前的状态。

---

## 4. 本地构建与运行

### 4.1 前置条件

- JDK 25（`JAVA_HOME` 指向 JDK 25）
- Maven 3.9+（本机若为旧版本，直接用仓库自带的 `mvnw`）
- MySQL 8.x（`localhost:3306`）、Redis（`localhost:6379`）

### 4.2 初始化数据库

```bash
mysql -uroot -p < sql/init.sql     # 建库 + sys_user + QRTZ_*
mysql -uroot -p owner < sql/mall.sql   # 电商业务表
```

### 4.3 数据库口令

`application-local.yml` 默认 `MYSQL_PASSWORD=9999`，可用环境变量覆盖：

```bash
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=你的密码
```

### 4.4 构建

```bash
./mvnw clean package          # 含单元测试
./mvnw clean package -DskipTests
```

产物：**`owner-server/target/owner-server.jar`**（无版本号后缀）

### 4.5 运行

```bash
java -jar owner-server/target/owner-server.jar --spring.profiles.active=local
```

> **本机注意**：环境变量 `SERVER__PORT` / `SERVER__HOST` 会被 Spring Boot 宽松绑定
> 映射到 `server.port` / `server.address`，覆盖配置文件里的 41452。
> 启动时显式加 `--server.port=41452` 最稳妥。
>
> 终端里直接 `java` 可能命中 PATH 中 Oracle 的 javapath（Java 17），报
> `UnsupportedClassVersionError: class file version 69.0`。用绝对路径调用 JDK 25 的
> `java.exe`，或修正 PATH。

### 4.6 容器运行

```bash
docker compose up -d --build
```

会拉起 `owner-mysql`、`owner-redis`、`owner-server`（41452）与 `owner-web`（8080）。
MySQL 首次启动自动执行 `sql/init.sql` 与 `sql/mall.sql`。

---

## 5. 访问入口与示例请求

| 入口 | 地址 |
| --- | --- |
| Knife4j 接口文档 | http://localhost:41452/doc.html |
| Swagger UI | http://localhost:41452/swagger-ui.html |
| OpenAPI JSON | http://localhost:41452/v3/api-docs |
| Druid 监控 | http://localhost:41452/druid/login.html （账号密码见 `.env`） |
| 健康检查 | http://localhost:41452/actuator/health |

### 5.1 示例请求

```bash
BASE=http://localhost:41452

# 注册（自助注册即顾客）
curl -X POST $BASE/api/auth/register -H 'Content-Type: application/json' \
  -d '{"mobile":"13900000001","password":"a123456","nickname":"小林"}'

# 登录（account 可填 username 或 mobile），拿 token
TOKEN=$(curl -s -X POST $BASE/api/auth/login -H 'Content-Type: application/json' \
  -d '{"account":"shopadmin","password":"shop123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# 店面首页（公开，无需登录）；shopId 见启动日志 [MallBootstrap] 一行，
#   或平台超管调 GET /api/platform/shops
curl "$BASE/api/storefront/shops/$SHOP_ID/home"

# 我的订单
curl "$BASE/api/order/mine" -H "Authorization: Bearer $TOKEN"
```

### 5.2 雪花 ID 为什么是字符串

主键用 MyBatis-Plus `IdType.ASSIGN_ID`（雪花算法），19 位整数，例如 `2110285652501422082`。
而 JavaScript 的 `Number.MAX_SAFE_INTEGER` 只有 `9007199254740991`（16 位），
浏览器 `JSON.parse` 会静默丢精度：

```
后端返回   2110285652501422082
浏览器解析 2110285652501422000   ← 尾数被截断
PUT 回传   2110285652501422000
后端响应   600 用户不存在：2110285652501422000
```

`JacksonConfig` 因此注册了 `Long → String` 序列化器：

| 类型 | 序列化结果 | 示例 |
| --- | --- | --- |
| 包装类型 `Long` | 字符串 | `"id": "2110..."` |
| 基本类型 `long` | 数字 | `"total": 4`、`"timestamp": 1750000000000` |

> 只注册 `Long.class`（装箱），`long.class`（原始类型）不受影响，
> 因此 `PageResult.total`、`R.timestamp` 仍是数字。
> **前端所有含雪花主键的字段一律用 `string` 承接。**

---

## 6. 接口清单

`/api/storefront/**` 与 `/api/auth/register`、`/api/auth/login`、`/api/pay/notify/**` 公开，
其余需 `Authorization: Bearer <token>`。

### 顾客侧

| 模块 | 接口 |
| --- | --- |
| 认证 | `POST /api/auth/register`、`POST /api/auth/login`、`POST /api/auth/logout`、`GET /api/auth/me`、`PUT /api/auth/profile`、`PUT /api/auth/password`、`POST /api/auth/mobile/code`、`PUT /api/auth/mobile` |
| 店面 | `GET /api/storefront/shops/{shopId}/home`、`/products`、`/search`、`/products/{productId}`、`/products/{productId}/reviews` |
| 地址 | `GET/POST /api/address`、`PUT /api/address/{addressId}/default`、`DELETE /api/address/{addressId}` |
| 购物车 | `GET /api/cart`、`GET /api/cart/count`、`POST /api/cart`、`POST /api/cart/merge`、`PUT /api/cart/quantity`、`DELETE /api/cart/{skuId}`、`DELETE /api/cart` |
| 订单 | `POST /api/order/preview`、`POST /api/order/submit`、`GET /api/order/mine`、`GET /api/order/{orderId}`、`POST /api/order/{orderId}/cancel`、`POST /api/order/{orderId}/confirm`、`GET /api/order/options` |
| 支付 | `POST /api/pay/{orderId}`、`POST /api/pay/notify/{channel}`、`POST /api/pay/mock/callback` |
| 售后 | `POST /api/refund/orders/{orderId}`、`GET /api/refund/mine`、`GET /api/refund/{refundId}`、`POST /api/refund/{refundId}/return-ship`、`GET /api/refund/options` |
| 评价 | `POST /api/review`、`GET /api/review/orders/{orderId}` |
| 优惠券 | `GET /api/coupon/center`、`POST /api/coupon/{couponId}/receive`、`GET /api/coupon/mine`、`GET /api/coupon/usable` |

### 店铺侧（需 `shop_member`；管理操作需 `OWNER`）

| 模块 | 接口 |
| --- | --- |
| 店铺 | `GET /api/shop/{shopId}/dashboard`、`GET/PUT /api/shop/{shopId}/profile`、`GET/POST /api/shop/{shopId}/members`、`PUT /api/shop/{shopId}/members/{memberId}` |
| 类目 | `GET/POST /api/shop/{shopId}/categories`、`PUT /api/shop/{shopId}/categories/{categoryId}/status` |
| 商品 | `GET/POST /api/shop/{shopId}/products`、`GET/PUT /api/shop/{shopId}/products/{productId}`、`PUT .../{productId}/status` |
| 库存 | `GET /api/shop/{shopId}/stock/skus`、`POST /api/shop/{shopId}/stock/adjust`、`GET /api/shop/{shopId}/stock/logs` |
| 订单 | `GET /api/shop/{shopId}/orders`、`GET .../ship-companies`、`GET .../{orderId}`、`POST .../{orderId}/ship`、`PUT .../{orderId}/ship` |
| 售后 | `GET /api/shop/{shopId}/refunds`、`GET .../{refundId}`、`POST .../{refundId}/agree`、`/refuse`、`/received` |
| 评价 | `GET /api/shop/{shopId}/reviews`（只读，无删除） |
| 优惠券 | `GET/POST /api/shop/{shopId}/coupons`、`PUT .../{couponId}/status`、`POST .../{couponId}/grant` |

### 平台侧（需 `platform_admin = 1`）

| 模块 | 接口 |
| --- | --- |
| 店铺 | `GET /api/platform/shops/overview`、`GET/POST /api/platform/shops`、`PUT /api/platform/shops/{shopId}/status` |
| 用户 | `GET /api/platform/users`（手机号打星）、`PUT /api/platform/users/{userId}/status`、`PUT /api/platform/users/{userId}/password` |

---

## 7. 配置项

`application.yml` 中的 `owner.*`：

| 键 | 默认 | 说明 |
| --- | --- | --- |
| `owner.auth.jwt.secret` | 内置开发密钥 | 生产**必须**用 `OWNER_JWT_SECRET` 覆盖 |
| `owner.auth.jwt.expire-seconds` | `604800` | 令牌有效期 7 天；退出登录会把 jti 拉黑 |
| `owner.mall.bootstrap.enabled` | `true` | 首次启动补齐超管 / 店主 / 店铺 / 类目 |
| `owner.mall.bootstrap.shop-name` | `日用铺` | |
| `owner.mall.bootstrap.ship-address` | 示例地址 | 售后退货默认用它，**店主应改成真实地址** |
| `owner.mall.bootstrap.owner-*` | `shopadmin / shop123 / 13800000001` | 店主账号 |
| `owner.mall.sms.expose-code` | `false` | 未接短信通道；`true` 时验证码随响应返回，**仅本地联调** |
| `owner.mall.pay.mock-enabled` | `false` | 没有商户号时必须为 `false` |
| `owner.mall.order.timeout-job.enabled` | `true` | 每分钟取消超 30 分钟的待付款订单并释放库存 |
| `owner.mall.order.auto-finish-job.enabled` | `true` | 每天 02:10 把发货满 7 天的待收货订单改为已完成 |
| `owner.thread-pool.*` | `8 / 32 / 1000 / 60` | 业务线程池 |
| `owner.druid.monitor.*` | `admin / change-me` | Druid 监控台账号 |

**Profile**：

| Profile | 说明 |
| --- | --- |
| `local` | 默认。MySQL / Redis 走 `localhost`；**模拟支付与验证码回显打开**，便于本地跑通全流程 |
| `docker` | 容器环境。服务名寻址；模拟支付与验证码回显**关闭**，与生产一致 |

> 敏感值推荐写在根目录 `.env`（已被 `.gitignore` 排除，由
> `spring.config.import: optional:file:./.env[.properties]` 加载）。

---

## 8. Spring Boot 4 适配要点

迁移到 Spring Boot 4 后有几处与 Boot 3 不同，工程内已逐一处理：

1. **Jackson 3**
   Boot 4 默认 JSON 实现是 Jackson 3（`tools.jackson`，注解仍在 `com.fasterxml.jackson.annotation`）。
   `JsonUtils` 基于 `JsonMapper`，Redis 缓存用 `GenericJacksonJsonRedisSerializer`。
   旧写法 `spring.jackson.serialization.write-dates-as-timestamps` 在 Jackson 3 已不存在，
   会导致属性绑定失败，配置中已移除。
   **不要再引 Jackson 2 的 `databind`。**

2. **`spring-boot-starter-aop` 已移除**
   AOP 能力改为直接引入 `org.aspectj:aspectjweaver`。

3. **Druid 与动态数据源共存**
   必须排除 Druid 自带的 `DruidDataSourceAutoConfigure`（否则与 dynamic-datasource 争抢
   `DataSource`），并排除 Knife4j 的两个自动配置类：

   ```yaml
   spring.autoconfigure.exclude:
     - com.alibaba.druid.spring.boot4.autoconfigure.DruidDataSourceAutoConfigure
     - com.github.xiaoymin.knife4j.spring.configuration.Knife4jAutoConfiguration
     - com.github.xiaoymin.knife4j.spring.configuration.insight.Knife4jInsightAutoConfiguration
   ```

4. **Quartz JDBC JobStore**
   数据源由 dynamic-datasource 提供，Boot 自带装配条件不生效，因此由 `QuartzConfig` 通过
   `SchedulerFactoryBeanCustomizer` 显式绑定；
   **不要**在 `spring.quartz.properties` 里指定 `org.quartz.jobStore.class`，
   留空时 Spring 用 `LocalDataSourceJobStore` 才能复用 Spring 管理的 `DataSource`
   （改成 `JobStoreTX` 会抛 `DataSource name not set`）。

5. **Knife4j 4.5.0 与 springdoc 3.0.3**
   Knife4j 4.5.0 的自动配置针对 springdoc 2.x 编写，直接启用会抛
   `NoSuchMethodError: SpringDocConfigProperties.getGroupConfigs()`。
   排除其自动配置后仅保留增强 UI（`/doc.html` 来自静态 webjar，不依赖自动配置）。
   同时把 `io.swagger.core.v3:*` 统一锁定到 `2.2.47`。

6. **MyBatis-Plus 3.5.9+**
   分页等 SQL 解析能力已拆分为独立构件，需显式引入 `mybatis-plus-jsqlparser`。

7. **Redis 无密码场景**
   不要保留 `spring.data.redis.password` 配置项：空字符串会让客户端仍发送 `AUTH`
   导致连接失败。需要密码时设 `SPRING_DATA_REDIS_PASSWORD`。

8. **端口覆盖**
   Spring Boot 宽松绑定会把环境变量 `SERVER__PORT` 映射到 `server.port`，
   可用 `--server.port=41452` 显式指定。

---

## 9. 前端工程 owner-web（尚未迁移）

**当前仍是旧门户的登录页、仪表盘与用户管理页，与电商系统不匹配，需要整体重做。**

要做的部分：

| 部分 | 页面 |
| --- | --- |
| 店面前台 | 首页 / 类目 / 搜索 / 商品详情 / 购物车 / 结算 / 我的订单 / 售后 / 评价 / 领券中心 |
| 店铺后台 | 仪表盘 / 商品 / 规格与库存 / 订单发货 / 售后处理 / 评价 / 优惠券 / 员工 / 店铺资料 |
| 平台后台 | 店铺管理 / 全站用户管理 |

技术栈与既有约定：

| 依赖 | 版本 |
| --- | --- |
| React / React DOM | 19.3.0 |
| Vite | 8.3.4 |
| TypeScript | 7.0.2 |
| Ant Design | 6.6.6 |
| React Router | 7.18.4 |
| Zustand | 5.0.15 |
| Axios | 1.20.0 |

- dev 端口 `5273`，容器 `8080`；Vite 把 `/api`、`/actuator`、`/v3/api-docs`、`/doc.html`
  代理到 `41452`，浏览器侧与后端同源。
- `src/api/request.ts` 的响应拦截器负责拆包 `R`，业务代码直接拿 `data`。
- antd v6 用 `App.useApp()` 取 message，经 `src/utils/feedback.ts` 桥接给非组件模块。
- 含雪花主键的字段一律声明为 `string`。

```bash
cd owner-web
npm install
npm run dev          # http://localhost:5273
npm run build        # 输出 dist/
npm run typecheck    # 仅类型检查
```

---

## 10. 验证

后端启动后（**必须用能常驻的方式启动**，shell 一返回进程就会被回收）执行：

```bash
python scripts/smoke-mall.py            # 61 项：全流程 + 权限
python scripts/smoke-platform-users.py  # 13 项：平台用户管理
```

`smoke-mall.py` 覆盖：建商品上架（无图 / 无可售 SKU 自动降级草稿）、下单价格不一致被拦、
库存锁定、模拟支付入账、仅退款全流程、退货退款全流程、拒绝后订单回退、库存流水记 `RETURN`、
评价三种拒绝理由、优惠券门槛 / 限领 / `LOCKED→USED` / 取消 `RETURNED`，
以及顾客 / 店员 / 平台超管的越权拦截。

单元测试（不依赖 MySQL / Redis）：

```bash
./mvnw test
```
