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
| 前端 | React 19 + Vite + TypeScript + Ant Design 6（店面 + 店铺后台 + 平台后台，见 §9） |

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
└── owner-web              # 前端工程（店面 + 店铺后台 + 平台后台）
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

**用户** 和 **店铺**。单商家模式下只有一个店铺（日用铺）、一个店主（koko），
顾客可以有很多，全部走同一个登录入口，按 `role` 分流。多店铺/店员能力保留在底层，界面已收起。

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

| 角色 | 判定 | 能做什么 |
| --- | --- | --- |
| 店主（`MERCHANT`） | `sys_user.platform_admin = 1`（koko），或 `shop_member` 里有成员记录 | 进店铺后台：商品、库存、订单、售后、优惠券、顾客管理、店铺资料 |
| 顾客（`CUSTOMER`） | 其余全部注册用户 | 逛店面、下单、看自己的订单、售后、评价、领券；<b>看不到也调不到任何经营接口</b> |

> **2026-10-20 起是单商家模式**：本站只有 koko 一个店主，顾客可以有很多。
> 登录接口返回的 `user.role` 是唯一身份判断依据（`MERCHANT` / `CUSTOMER`），
> 前端三层守卫（`RequireAuth` / `RequireShop` / `RequireMAuth`）都按它判断。
> `shop_member`、多店铺、店员这些能力**底层保留**（数据库表与接口都在），
> 但界面上的多店铺切换、店员管理、平台店铺管理入口已收起；
> `/api/platform/**` 里的经营接口现在只校验「是不是店主」。


启动引导 `MallDataInitializer` 首次启动会自动补齐：

- `koko` 设为平台超管并写入 BCrypt 口令（默认 `koko@2026`）
- `koko` 同时作为「日用铺」的 `OWNER`（不再另建 `shopadmin`，店主与平台超管是同一个账号）
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
11. **评价一笔订单一条**，顾客提交后不可改；店主可整条删除（处理广告 / 辱骂 / 泄露隐私），店员没有删除权限。
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
  -d '{"account":"koko","password":"koko@2026"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

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

### 5.3 图片存储：默认本地磁盘，不依赖对象存储

商品图、店铺横幅都走同一个接口 `POST /api/upload/image`，文件落在
`owner.mall.upload.dir`（默认 `./uploads/<业务目录>/<年>/<月>/<日>/<随机名>.<ext>`），
通过 `/files/**` 直接访问——`img` 标签不会带 `Authorization`，所以这个前缀是公开的。

```
上传：POST /api/upload/image  (multipart, file=@p1.jpg, folder=product)
返回：{"code":200,"data":"/files/product/2026/10/19/3f2a....jpg"}
静态映射：/files/**  →  file:<upload.dir>/
```

**换 OSS 时不需要改前端**：`FileStorage` 是接口，新增一个 `OssFileStorage` 实现
（上传到 OSS 后返回完整 `https://cdn...` 地址），替换掉 `LocalFileStorage` 这个 Bean 即可，
数据库里的 `url` 字段本来就是字符串，两种地址都能存。

| 方案 | 成本 | 适用 |
| --- | --- | --- |
| 本地磁盘（当前） | 0 | 单机部署、内网、开发演示 |
| 对象存储（OSS / COS / 七牛 / R2） | 存储约 0.12 元/GB/月 + 流量 | 正式上线；国内需已备案域名 |
| 免费图床 | 0 | 只适合临时演示，随时可能失效 |

> 演示商品图片放在前端 `owner-web/public/demo/p1..p10.jpg`，地址写成 `/demo/p1.jpg`，
> 不依赖任何外网。演示数据由 `sql/demo-products.sql` 灌入（可重复执行）。

---

## 6. 接口清单

`/api/storefront/**` 与 `/api/auth/register`、`/api/auth/login` 公开，
其余需 `Authorization: Bearer <token>`。

### 顾客侧

| 模块 | 接口 |
| --- | --- |
| 认证 | `POST /api/auth/register`、`POST /api/auth/login`、`POST /api/auth/logout`、`GET /api/auth/me`、`PUT /api/auth/profile`、`PUT /api/auth/password`、`POST /api/auth/mobile/code`、`PUT /api/auth/mobile` |
| 店面 | `GET /api/storefront/shops`（营业中的店铺）、`/api/storefront/shops/{shopId}/home`、`/products`、`/search`、`/products/{productId}`、`/products/{productId}/reviews` |
| 地址 | `GET/POST /api/address`、`PUT /api/address/{addressId}/default`、`DELETE /api/address/{addressId}` |
| 购物车 | `GET /api/cart`、`GET /api/cart/count`、`POST /api/cart`、`POST /api/cart/merge`、`PUT /api/cart/quantity`、`DELETE /api/cart/{skuId}`、`DELETE /api/cart` |
| 订单 | `POST /api/order/preview`、`POST /api/order/submit`、`GET /api/order/mine`、`GET /api/order/{orderId}`、`POST /api/order/{orderId}/cancel`、`POST /api/order/{orderId}/confirm`、`GET /api/order/options` |
| 售后 | `POST /api/refund/orders/{orderId}`、`GET /api/refund/mine`、`GET /api/refund/{refundId}`、`POST /api/refund/{refundId}/return-ship`、`GET /api/refund/options` |
| 评价 | `POST /api/review`、`GET /api/review/orders/{orderId}` |
| 优惠券 | `GET /api/coupon/center`、`POST /api/coupon/{couponId}/receive`、`GET /api/coupon/mine`、`DELETE /api/coupon/mine/{couponUserId}`、`GET /api/coupon/usable` |

### 通用

| 模块 | 接口 |
| --- | --- |
| 上传 | `POST /api/upload/image`（multipart，字段 `file`，可选 `folder=product/shop/review/avatar`；返回 `/files/...` 相对地址） |

### 店铺侧（需 `shop_member`；管理操作需 `OWNER`）

| 模块 | 接口 |
| --- | --- |
| 店铺 | `GET /api/shop/{shopId}/dashboard`、`GET/PUT /api/shop/{shopId}/profile`、`GET/POST /api/shop/{shopId}/members`、`PUT /api/shop/{shopId}/members/{memberId}` |
| 类目 | `GET/POST /api/shop/{shopId}/categories`、`PUT /api/shop/{shopId}/categories/{categoryId}/status` |
| 商品 | `GET/POST /api/shop/{shopId}/products`、`GET/PUT/DELETE /api/shop/{shopId}/products/{productId}`、`PUT .../{productId}/status`；删除是逻辑删除，有未完结订单时拒绝 |
| 库存 | `GET /api/shop/{shopId}/stock/skus`、`POST /api/shop/{shopId}/stock/adjust`、`GET /api/shop/{shopId}/stock/logs` |
| 订单 | `GET /api/shop/{shopId}/orders`、`GET .../ship-companies`、`GET .../{orderId}`、`POST .../{orderId}/ship`、`PUT .../{orderId}/ship` |
| 售后 | `GET /api/shop/{shopId}/refunds`、`GET .../{refundId}`、`POST .../{refundId}/agree`、`/refuse`、`/received` |
| 评价 | `GET /api/shop/{shopId}/reviews`、`DELETE /api/shop/{shopId}/reviews/{reviewId}`（仅店主） |
| 优惠券 | `GET/POST /api/shop/{shopId}/coupons`、`PUT .../{couponId}/status`、`POST .../{couponId}/grant` |

### 经营接口（需店主身份，顾客一律 403）

| 模块 | 接口 |
| --- | --- |
| 顾客管理 | `GET /api/platform/users`（带订单数/累计消费/地址数）、`GET /api/platform/users/{userId}`（档案+地址+最近订单）、`PUT .../{userId}/status`、`PUT .../{userId}/password` |
| 店铺 | `GET /api/platform/shops/overview`、`GET/POST /api/platform/shops`、`PUT /api/platform/shops/{shopId}/status`（单商家下界面已收起，接口保留） |

> 路径仍在 `/api/platform/users` 下是为了不改既有契约，语义上就是「我的顾客」。
> 列表里给完整手机号（要发货、要联系），并排除店主与店铺成员——
> 店铺成员不是顾客，不该出现在这张表里。

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
| `owner.mall.bootstrap.owner-*` | `koko / koko@2026 / 17535306390` | 店主账号，默认与平台超管同一个 |
| `owner.mall.sms.expose-code` | `false` | 未接短信通道；`true` 时验证码随响应返回，**仅本地联调** |
| `owner.mall.order.timeout-job.enabled` | `false` | 已关闭：线下收款没有「付款倒计时」，订单不会被脚本自动取消；需要时可打开（每分钟取消超 30 分钟的待确认订单） |
| `owner.mall.order.auto-finish-job.enabled` | `true` | 每天 02:10 把发货满 7 天的待收货订单改为已完成 |
| `owner.mall.upload.dir` | `./uploads` | 图片落盘目录（相对启动目录或绝对路径），生产建议挂独立数据盘 |
| `owner.mall.upload.public-prefix` | `/files` | 上传文件的访问前缀，与前端代理一致 |
| `owner.mall.upload.max-image-size` | `5242880` | 单张图片上限 5 MB |
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

## 9. 前端工程 owner-web

已整体迁移到电商系统，一套工程分三种界面：

| 区域 | 路由前缀 | 页面 |
| --- | --- | --- |
| 店面（顾客，公开可逛） | `/` | 首页、全部商品（类目筛选 / 排序 / 分页）、搜索、商品详情（含评价）、购物车、确认订单、我的订单（支付 / 取消 / 收货 / 申请售后 / 评价）、我的账号（头像 / 资料 / 地址 / 优惠券 / 退款售后）；注册后先落到 `/welcome` 补头像 |
| 店铺后台（店主 / 店员） | `/admin` | 店铺看板、订单与发货、退款售后、商品管理、库存管理、优惠券、顾客评价、店铺与分类（资料 / 类目 / 店员） |
| 平台后台（平台管理员） | `/admin/shops`、`/admin/users` | 店铺管理（总览 + 开通店铺 + 停用）、用户管理（查询 / 停用 / 重置密码） |

登录页 `/login` 带「登录 / 注册」两个页签。一个账号同时是顾客和店主：登录返回的
`shops` 非空时，顶部用户菜单才出现「进入店铺后台」。

### 目录约定

```
owner-web/src
├── api/          request.ts（拆包 R）/ mall.ts（顾客侧）/ admin.ts（店铺 + 平台）
├── components/   SafeImage（图片兜底）/ ImageUploader（多图上传，首图为主图）
├── data/         regions.ts + regions.json（省市区离线数据，见下）
├── layout/       StoreLayout.tsx（店面）/ AdminLayout.tsx（后台）
├── pages/        Login / store/* / admin/* / platform/*
├── router/       index.tsx（RequireAuth / RequireShop / RequirePlatform 三层守卫）
├── store/        auth.ts（令牌 + 身份 + 当前店铺）/ shop.ts（当前浏览的店铺）
├── types/        mall.ts（与后端 VO/DTO 一一对应）
└── utils/        format.ts（分↔元、时间、状态配色）/ feedback.ts（message 桥接）
```

### 收货地址的省市区选择

地址表单用 `Cascader` 三级联动，数据来自 **`src/data/regions.json`（86 KB，gzip 后约 19 KB）**，
**不走任何第三方接口**：34 个省级（含港澳台）、365 个市级、3082 个区县，只在打开地址弹窗时
动态 import（Vite 自动拆成独立 chunk），首屏不受影响。

- 直辖市已压平成「北京市 / 北京市 / 朝阳区」，不会出现「市辖区」这种中间层
- 台湾省只有县市两级，此时 `district` 留空（后端 `AddressSaveDTO` 不强制该字段）
- 编辑历史地址时按名称反查路径，兼容「杭州」↔「杭州市」这类缺后缀的旧数据
- 数据需要更新（行政区划调整）时重跑 `python scripts/gen-regions.py`，前端代码不用改

### 与后端对接的几个关键点

- **金额**：后端一律**分**。展示用 `money()` / `yuan()`，表单里让用户填**元**，提交前用
  `yuanToFen()` 转回去（商品价格、优惠券门槛与面额都是这样）。
- **雪花 ID**：一律 `string`，类型里定义为 `Id = string`。
- **Long 包装类型序列化成字符串**：促销价、订单明细单价等字段在 JSON 里是 `"990"` 而不是
  `990`，所以类型声明为 `Money = string | number`，参与运算前统一过 `num()`。统计类字段
  （`long`）本来就是数字。
- **下单必须回传单价**：后端会比对 `price` 与会话里的价格，不一致直接拒绝（防篡改）。所以
  购物车和「立即购买」把 `price` 一起塞进 `sessionStorage['checkout:payload']`。
- **不做在线支付**：项目**没有接入微信支付、也没有接入支付宝**（无 SDK、无商户号、无证书），
  顾客端没有任何支付入口，也没有「我已付款」按钮。下单后订单停在 `PENDING_PAYMENT`（待确认），
  商家线下收到钱（现金 / 转账 / 到付）后在店铺后台点「确认收款」
  （`POST /api/shop/{shopId}/orders/{orderId}/confirm-payment`），订单才转待发货并扣减锁定库存。
  退款同理：商家线下退回后点「同意退款」，系统只做记账与库存回滚。
  将来要接微信/支付宝，在 `PaymentService` 里加通道实现即可，业务侧状态流转不用改。
- **店面默认进哪家店**：新增公开接口 `GET /api/storefront/shops` 返回营业中的店铺，
  未登录也能看到，前端据此确定浏览的店铺（多店时按返回顺序取第一家）。

### 手机端（`/m`）

手机端不是一个新工程，而是同一份 `owner-web` 里的独立界面层：**独立样式、独立布局、独立入口**，
桌面版改动不会溢出到手机端。

| 项 | 说明 |
| --- | --- |
| 路由 | `/m`（首页）、`/m/list`（搜索 / 类目 / 排序）、`/m/product/:id`、`/m/cart`、`/m/checkout`、`/m/orders`、`/m/account`、`/m/address`、`/m/login` |
| 目录 | `src/mobile/`：`MobileLayout.tsx` + `pages/*` + `components/MProductCard.tsx` + `mobile.css` |
| 样式 | 全部类名以 `m-` 开头，**不使用 antd 组件**，避免桌面版组件在窄屏上的尺寸与间距问题 |
| 布局 | 底部四个 Tab（首页 / 商品 / 购物车 / 我的）；详情、结算等二级页隐藏 Tab，改为底部操作条 |
| 适配 | `viewport` + 卡片式两列网格 + 44px 触控高度 + `env(safe-area-inset-bottom)` 刘海安全区 |
| 地址 | 手机端用**原生三级 `select`**（拉起系统选择器），复用同一份 `regions.json` |

登录鉴权走独立的 `RequireMAuth` 守卫（未登录跳 `/m/login`，登录后回原页），
令牌与桌面版共用同一份 `owner-mall-auth` 持久化。

单独托管（手机访问的独立链接）：

```bash
cd owner-web
npm run build                       # 先出 dist
npm run mobile                      # 等价于 node ../scripts/mobile-server.mjs --port 5280
# 或：node scripts/mobile-server.mjs --port 5280 --backend http://127.0.0.1:41452
```

`scripts/mobile-server.mjs` 是一个零依赖的静态服务器：根路径跳 `/m`、其余路径 SPA 回落
到 `index.html`、文本资源 gzip、并把 `/api`、`/files`、`/actuator`、`/v3/api-docs`、`/doc.html`
反代到后端。手机与电脑不在同一网段、或路由器开了客户端隔离时，用
`cloudflared tunnel --url http://127.0.0.1:5280` 拿到一个临时公网 https 地址即可。

技术栈：

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
- antd v6 注意：`Divider` 的 `orientation` 现在只接受 `horizontal / vertical`（旧的
  `left/center/right` 已移除）。

```bash
cd owner-web
npm install
npm run dev          # http://localhost:5273
npm run build        # tsc --noEmit && vite build，输出 dist/
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
