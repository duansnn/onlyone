# Owner Mall（日用铺电商系统）项目长期约定

## 基本信息
- 工作区：`D:/code/onlyone`
- 多模块 Maven 工程，groupId `com.owner`，版本 `2026.03-SNAPSHOT`
- 模块：`owner-common`（通用）/ `owner-framework`（框架）/ `owner-system`（业务）/ `owner-server`（启动）
- 前端工程：`owner-web`（React 19 + Vite + TS + antd 6），dev 端口 5273，容器 8080
- 服务端口固定 **41452**，应用名 `owner-server`，Quartz 调度器实例名 `OwnerScheduler`
- JDK 25，编译 `release=25`，UTF-8；Maven 用工程自带 `./mvnw`（3.9.11，本机 Maven 3.6.3 过旧）

## 命名约定（2026-10-14 全量重命名）
- 历史上所有 `mt*` 前缀已统一改为 `owner`，**不要**再引入 `mtl` / `mtlntelliportal` / `MTL` 等旧前缀
- 模块 `owner-*`；包 `com.owner.*`；配置前缀 `owner.*`（auth/mall/druid/thread-pool/web/app）
- 常量前缀 `OWNER_`；线程名前缀 `owner-async-` / `owner-scheduling-`
- 容器/网络：`owner-mysql` / `owner-redis` / `owner-server` / `owner-web` / `owner-net`；数据库 `owner`
- 口令统一 `owner@2026`（Docker 的 MySQL/Redis 与 Druid 监控台）
- 品牌名统一写作 **Owner Mall**，中文全称「日用铺电商系统」。
  2026-10-16 起 **`Owner IntelliPortal` 已废弃**，除 `scripts/rename-to-owner.py`
  （迁移记录）与尚未重做的 `owner-web/` 外不应再出现
- 启动 banner：`owner-server/src/main/resources/banner.txt`，大字用 figlet `standard` 字体写 **OWNER**，
  副标题 `:: Owner Mall · 日用铺 ::` + 右侧 `Spring Boot ${...}`。
  **注意 CJK 是双宽**，改写副标题后要按显示列数重算中间空格，否则右侧不对齐
- **改名/替换后的残留复查必须全量扫描，不要用扩展名白名单**：
  ```bash
  grep -rniE "<旧词>" . --exclude-dir=node_modules --exclude-dir=target \
    --exclude-dir=dist --exclude-dir=.git --binary-files=without-match
  ```
  上次只扫了 `*.java/xml/yml/md/sql/ts/json/conf/sh`，漏掉 **`banner.txt`**（`.txt` 不在白名单），
  误报"零残留"，被用户在启动日志里一眼看出来

## 技术约定
- 统一响应：`com.owner.common.core.domain.R<T>`；分页 `PageResult` / `PageQuery`
- 业务异常统一抛 `ServiceException`，由 `GlobalExceptionHandler` 转 `R.fail`
- JSON 统一用 `JsonUtils`（Jackson 3 / `tools.jackson`），不要再引 Jackson 2 的 databind
- Redis 缓存用 `GenericJacksonJsonRedisSerializer`；**Redis 客户端是 Lettuce**
  （2026-10-16 已删掉 `redisson-spring-boot-starter`，它此前悄悄接管了 `RedisConnectionFactory`）
- 新增/修改表结构时同步更新 `sql/init.sql`（含 QRTZ_* 表）与 `sql/mall.sql`
- 业务代码放 `owner-system`：框架级公用能力在 `com.owner.system.{domain.entity,mapper}`，
  电商业务全部在 `com.owner.system.mall` 下按 `controller / service / mapper / domain(entity,dto,vo,enums) / support / config / job` 分层
- **依赖只留真正用到的**：项目已无 WebSocket / AI / RocketMQ / Kafka / RabbitMQ / Redisson。
  加依赖前先 grep 确认有引用，别把上一版能力再带回来

## 前端约定（owner-web）
- **2026-10-16 已整体迁移到电商**，一套工程三种界面：
  - 店面（顾客，`/` 下，**公开可逛**）：Home / ProductList / ProductDetail / Cart / Checkout / Orders / Account
  - 店铺后台（`/admin`）：Dashboard / Orders / Refunds / Products / Stock / Coupons / Reviews / Shop
  - 平台后台（`/admin/shops`、`/admin/users`，仅 `platformAdmin`）
  - 登录页 `/login` 带「登录 / 注册」页签
- 目录：`api/{request,mall,admin}.ts`、`layout/{StoreLayout,AdminLayout}`、`pages/{Login,store,admin,platform}`、
  `router`（`RequireAuth` / `RequireShop` / `RequirePlatform` 三层守卫）、`store/{auth,shop}`、
  `types/mall.ts`、`utils/{format,feedback}.ts`
- `src/api/request.ts` 的响应拦截器负责拆包 `R`，业务代码直接拿 `data`；
  **`put(url, data, params)` 支持第三个参数**（状态切换类接口是 `@RequestParam`，要走 query）
- antd v6 用 `App.useApp()` 取 message，经 `src/utils/feedback.ts` 桥接给非组件模块
- 开发期由 Vite 代理 `/api`、`/actuator`、`/v3/api-docs`、`/doc.html` 到 41452
  （后端地址可用 `VITE_BACKEND_ORIGIN` 覆盖，端口可用 `VITE_DEV_PORT` 覆盖）
- **雪花 ID 一律用 `string`**（`types/mall.ts` 的 `Id = string`）：后端主键为 MyBatis-Plus
  `IdType.ASSIGN_ID`，19 位整数超出 JS `Number.MAX_SAFE_INTEGER`（16 位）。
  后端由 `owner-framework/config/JacksonConfig` 注册 `Long -> String` 序列化器。
  区分：**包装类型 `Long` 序列化为字符串**（所以价格字段是 `"990"`），
  **基本类型 `long` 仍是数字**（`PageResult.total`、`R.timestamp`、`minPrice`）。
  前端把 Long 字段声明为 `Money = string | number`，参与运算前一律过 `utils/format.ts` 的 `num()`
- **金额**：后端一律分；展示用 `money()`/`yuan()`，表单让用户填元，提交前 `yuanToFen()`
- **下单必须回传单价** `price`（后端比对，防篡改）——购物车/立即购买把
  `{shopId, items:[{skuId,quantity,price}]}` 塞进 `sessionStorage['checkout:payload']`
- **店面默认店铺**：公开接口 `GET /api/storefront/shops` 返回营业中的店铺（前端未登录也能确定逛哪家店）
- **支付不假装成功**：`payable=false` 时只提示未开通；开发环境才有「模拟支付成功」按钮，
  调 `/api/pay/mock/callback`，走与真实通道相同的校验与幂等逻辑
- antd v6 坑：`Divider` 的 `orientation` 现在只接受 `horizontal/vertical`（旧的 `left/center/right` 已移除）
- **测试脚本坑**：Git Bash 不认 `D:/...` 形式的 PATH 项，调 java 必须用绝对路径
  `/d/soft/.../bin/java.exe`（否则命中 Oracle javapath 的 Java 17，报 UnsupportedClassVersionError）
- 冒烟脚本支持 `MALL_BASE` 环境变量覆盖后端地址（默认 `http://127.0.0.1:41452`），
  本机 41452 常被 IDEA 调试实例占着，验证时用 `MALL_BASE=http://127.0.0.1:41453`

## Spring Boot 4 必须注意
- 不要用 `spring-boot-starter-aop`（已移除），AOP 用 `aspectjweaver`
- YAML 里不要写 Jackson 2 时代的 `spring.jackson.serialization.write-dates-as-timestamps`
- `spring.autoconfigure.exclude` 必须包含：
  - `com.alibaba.druid.spring.boot4.autoconfigure.DruidDataSourceAutoConfigure`
  - `com.github.xiaoymin.knife4j.spring.configuration.Knife4jAutoConfiguration`
  - `com.github.xiaoymin.knife4j.spring.configuration.insight.Knife4jInsightAutoConfiguration`
- `io.swagger.core.v3:*` 锁定 2.2.47（与 springdoc 3.0.3 配套）
- Quartz 不要指定 `org.quartz.jobStore.class`，用 Spring 默认 `LocalDataSourceJobStore`
- Lombok / MapStruct 在根 POM 的 `<dependencies>` 中声明，子模块不再单独声明

## 日用铺电商系统（com.owner.system.mall）
- **项目已整体切换为电商**：PRD = `docs/电商系统规划.md`，是唯一的需求来源，改代码前先读它
- 只有两种长期实体：**用户**和**店铺**。商户不是第三套账号，管哪家店由 `shop_member` 决定
- 金额一律 **分（Long 整数）**；雪花 ID 一律序列化成**字符串**；逻辑删除 `del_flag`
- 店铺数据一律带 `shop_id`；每个店铺接口先取登录态再查 `shop_member`（`ShopGuard`）
- 防超卖用**带条件的原子 UPDATE**（`ProductSkuMapper`），可售 = `stock - locked_stock`
- **支付与退款都必须经 `PaymentGateway`**，通道未开通时返回 false 并回滚，
  绝不假装成功（没有「我已付款」按钮，也不会「钱没退却显示已退款」）
- 售后按**整单**；申请时的订单原状态记在 `order_log`（`to_status=REFUNDING` 那条的
  `from_status`），拒绝时反查恢复，**不要**给 refund 表加字段
- 启动引导 `MallDataInitializer` 自动建 koko / shopadmin / 「日用铺」/ 5 组两级类目（共 20 个）
- 启动冒烟脚本在 **`scripts/`**（不在 .workbuddy/tmp）：`smoke-mall.py`（61 项）、
  `smoke-platform-users.py`（13 项），可重复跑。
  **后端必须用 Bash 的 `run_in_background=true` 起**，否则 shell 返回即被回收
- 打包产物是 `owner-server.jar`（无版本号后缀）
- 前端已完整对接（2026-10-16）；README §9 与 §6 接口清单是前端改动时的第一手参考

## 本地运行环境（本机）
- MySQL 8.0：`localhost:3306`，root / `9999`，库 `owner`（旧库 `mtlntelliportal` 已废弃）
- Redis：`localhost:6379`，**无密码**（配置中不要出现 `spring.data.redis.password`）
- 无 Docker；项目已无 RocketMQ / Kafka / RabbitMQ / WebSocket / AI 依赖（2026-10-16 清理）
- shell 中存在环境变量 `SERVER__PORT`，会覆盖 `server.port`，启动时需显式 `--server.port=41452`
  （或 `unset SERVER__PORT`；另有 `SERVER__HOST`，会映射到 `server.address`）
- shell 中预设了透明代理 `http_proxy/https_proxy=http://127.0.0.1:64304`，会让 curl 访问
  localhost 返回 **502（upstream connect failed）** 而不是连接错误；探测本机服务时需
  `unset http_proxy https_proxy` 或 `curl --noproxy '*'`
- npm 官方源在本机极慢（~16 KB/s），`owner-web/.npmrc` 已指向 `registry.npmmirror.com`
- **本机 PATH 里的 `%JAVA_ HOME%\bin` 写错了一个空格**（Machine 级），展开后是无效路径，
  因此 JAVA_HOME 的 bin 不在 PATH 上；`C:\Program Files\Common Files\Oracle\Java\javapath`
  排在前面，导致命令行 `java` 实际是 **Java 17**。
  影响：`mvnw` 和 IDEA 都读 `JAVA_HOME`（= `D:\soft\jdk25\jdk-25_windows-x64_bin\jdk-25.0.4`，正确，注意是下划线），
  所以构建没问题；但**终端里直接 `java -jar` 会跑 Java 17，报 `UnsupportedClassVersionError: class file version 69.0`**。
  解决：改用绝对路径 `D:/soft/jdk25/jdk-25_windows-x64_bin/jdk-25.0.4/bin/java.exe`，或修正 PATH 那个条目
- 注意 `SERVER__PORT` / `http_proxy` 等只存在于 WorkBuddy 的 shell 里，
  **不是** User/Machine 级持久环境变量，自己在 IDEA / 终端里跑不会受影响
- **本机页面文件过小**：物理 32.7 GB，但 `C:\pagefile.sys` 只有约 1.1 GB，导致系统提交内存
  容易耗尽，Maven 打包 / 启动 JVM 会报
  `os::commit_memory failed; error='页面文件太小' (DOS error/errno=1455)` 或
  `Native memory allocation (mmap) failed ... G1 virtual space`。
  常驻大户：idea64 ~3.5 GB、Memory Compression ~2.5 GB、WorkBuddy ~1.1 GB、飞书 ~1.8 GB、Edge ~0.8 GB。
  **遇到时先释放内存或调大页面文件，这不是项目代码问题**
- 停止后端：`Get-CimInstance Win32_Process -Filter "Name='java.exe'" | ? { $_.CommandLine -like '*owner-server.jar*' } | % { Stop-Process -Id $_.ProcessId -Force }`
