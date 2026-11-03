# Owner Mall（日用铺）长期约定

## 基本盘
- 工作区 `D:/code/onlyone`。Maven 多模块（`com.owner` / `2026.03-SNAPSHOT`）：
  `owner-common` / `owner-framework` / `owner-system`（业务）/ `owner-server`（启动）。
  前端 `owner-web`（React 19 + Vite + TS + antd 6，dev 5273），原生小程序 `owner-mp`（顾客端，appid `wx4588b8532c0b37bf`）。
- 端口 **41452**，应用名 `owner-server`，产 `owner-server.jar`（无版本号）；JDK 25（`release=25`），Maven 用 `./mvnw`。
- 品牌 Owner Mall /「日用铺电商系统」。旧前缀 `mt*` / `Owner IntelliPortal` 已废弃，别再引入；容器/库前缀 `owner`，口令 `owner@2026`。
- banner `owner-server/src/main/resources/banner.txt`：figlet `standard` 写 OWNER，**CJK 双宽**，改字后重算空格。
- 改名残留复查**必须全量扫**（曾漏 `banner.txt`）：
  `grep -rniE "<旧词>" . --exclude-dir={node_modules,target,dist,.git} --binary-files=without-match`

## 后端约定
- 统一响应 `R<T>` / `PageResult` / `PageQuery`；业务异常抛 `ServiceException` → `GlobalExceptionHandler`。
- JSON 用 `JsonUtils`（Jackson 3 / `tools.jackson`），**不要引 Jackson 2 databind**。
- Redis `GenericJacksonJsonRedisSerializer` + **Lettuce**（已删 redisson，它抢过 `RedisConnectionFactory`）。
- `Long` 包装类型序列化为字符串（`JacksonConfig`），`long` 基本类型仍是数字；金额一律**分**；前端 `Id = string`，运算前过 `num()`。
- 业务在 `com.owner.system.mall`（`controller/service/mapper/domain(entity,dto,vo,enums)/support/config/job`）；改表同步 `sql/init.sql`（含 QRTZ_*）+ `sql/mall.sql`。
- 防超卖用**带条件原子 UPDATE**，可售 = `stock - locked_stock`；库存链路：下单只加 `locked_stock` → 确认收款扣 `stock` 清锁定 → 退款回补（`scripts/smoke-stock-chain.py` 14 项）。
- 图片 `FileStorage` 抽象，默认 `LocalFileStorage` → `./uploads/...`，`/files/**` 公开；nginx 必须 `location ^~ /files/`，否则被静态后缀正则抢走 404。换 OSS 只换 Bean。
- **Spring Boot 4 坑**：无 `spring-boot-starter-aop`（用 `aspectjweaver`）；`spring.autoconfigure.exclude` 要排 Druid + Knife4j 三个；`io.swagger.core.v3:*` 锁 2.2.47；Quartz 不指定 `jobStore.class`。
- 依赖只留用到的（无 WebSocket / AI / MQ / Redisson），加依赖前先 grep。

## 业务规则
- PRD 唯一来源 `docs/电商系统规划.md`，改代码前先读。
- 角色：`sys_user.role` 只有 `MERCHANT`/`CUSTOMER`，真实身份看 `shop_member.role`（OWNER/STAFF）；前端 `isMerchant(user)` 判后台、`isShopOwner(shops, currentShopId)` 控按钮 disabled。平台超管是**死代码**（`/admin/shops`→`/admin/shop`、`/admin/users`→`/admin/customers`）。
- **无在线支付**：下单停 `PENDING_PAYMENT`（「待确认」）→ 店主「确认收款」`POST /api/shop/{shopId}/orders/{orderId}/confirm-payment` → `markPaidOffline`（`channel=OFFLINE` + 扣库存 + 核销券）。`order.timeout-job.enabled` 默认 **false**。
- 售后按**整单**，原状态记在 `order_log.from_status`，**别给 refund 表加字段**。口径用 `RefundStatus.openCodes()`，看板与列表共用。
- **下单三件套（2026-11-03）**：① 锁库存前 `build.lines()` 按 `skuId` 升序排（防交叉持锁死锁）；② `OrderSubmitDTO.requestId` 幂等 —— Redis `owner:mall:order:idem:{userId}:{requestId}`（30min setIfAbsent）+ `order_info.idempotent_key` / `uk_order_idem`，`submit()` 拆成「幂等壳 + doSubmit()」，**失败必须 releaseIdempotency** 否则重试被挡；前端 `utils/id.ts` 的 `newRequestId()` 三端各生成一次；③ 后台订单超 24h 待确认行 `.mall-row-overdue` 标黄 + 胶囊标签。
- **店主/店员不能在自己店下单**：`OrderService.submit` 查 `shop_member` 即拒（code 600），preview 只提示；开关 `owner.mall.order.allow-self-purchase=false`。
- 演示账号（README §4.7）：店主 `koko`/`koko@2026`、店员 `18203536390`/`sike@2026`、顾客 `13259841768`/`amara@2026`。填 username 或 mobile，昵称不行；连错 5 次锁 15 分钟（`owner:mall:login:fail:<account>`，删 key 解锁）。
- 冒烟 `scripts/smoke-mall.py`（61 项）/ `smoke-platform-users.py`（13 项），`MALL_BASE` 可覆盖（41452 常被 IDEA 占，用 41453）。**后端必须 `run_in_background=true` 起**。
- 演示商品 `sql/demo-products.sql` + `owner-web/public/demo/p1..p10.jpg`（另有 `owner-server/src/main/resources/static/demo/` 副本供小程序单域名用）；**跑完冒烟要重跑该 SQL**。

## 前端（owner-web）
- 三层配色：根 `App.tsx` 黑主色（登录页）→ 店面 `STORE_THEME` / 后台 `ADMIN_THEME`。**后台按钮圆角 8，店面 999 胶囊**。
- 店面 `styles/storefront.css` 全 `sf-` 前缀（暖橙 `#ff5a1f` / 墨黑 `#17130f` / 米白 `#f8f5f0`），商品卡用 `ProductTile.tsx`，优先复用 `.sf-*` 类。
- 手机 H5 `src/mobile/`：`m-` 前缀、**不用 antd**，`--m-*` 与 `--sf-*` 同色值须同步改；5280 由 `scripts/mobile-server.mjs` 托管 dist，**改完必须 `npm run build`**。
- **zustand selector 禁止返回新引用**（`s.x?.list ?? []` 会白屏 `Maximum update depth exceeded`），兜底用模块级常量。
- 状态标签统一 `StatusTag.tsx`，**不要 `<Tag color="blue">`** 冷色；antd v6：`Tag bordered={false}`→`variant="filled"`、`Alert message`→`title`、`Space direction`→`orientation`、`Drawer width`→`size`、`destroyOnClose`→`destroyOnHidden`、`Divider orientation` 只收 `horizontal/vertical`。
- **登录/注册后不要再调 `/auth/me`**（`LoginVO` 已带 user，`signIn()` 前 token 为 null 必 401）。无 `from` 时店主→`/admin`，顾客→`/`。
- `api/request.ts` 拆 `R`；`put(url, data, params)` 第三参走 query。
- 详情富文本用 `utils/format.ts` 的 `plainText()` 清洗，**不用 dangerouslySetInnerHTML**；缺图占位用 `PLACEHOLDER`。
- 省市区离线 `data/regions.json`（34/365/3082，动态 import），直辖市压平、台湾 district 为 NULL；后端 `AddressVO.join` 相邻同名只拼一次。
- 商品表单 1080 双栏抽屉（`.pf-*`），改字段同步 `Form.useWatch` 预览与上架检查。
- 后台移动端：`useIsMobile()`（767px）Sider↔Drawer；`<Table>` 必带 `scroll={{x:'max-content'}}`；表单宽度用 `flex: 1 1 160px`。
- `/welcome` 独立布局 + `RequireAuth`，`afterAuth(result, onboard=true)` 只在注册分支；清空头像传 `avatarUrl: v ?? ''`。

## 小程序（owner-mp）
- 顾客端原生小程序，页面 `pages/{home,list,product,cart,checkout,orders,order,coupons,address,address-edit,account,login}`。
- 接口地址在 `config.js` 的 `apiOrigin`（cloudflared 临时 https）；**隧道重启地址就变**，变了必须改 `config.js` 再预览。
- 雪花 ID 不进 `data-xxx`（会被转 Number 截尾），用 `data-sid="id-{{id}}"` + `datasetId()` 剥前缀。
- 手机预览：开发者工具「预览」扫码 → 小程序右上角 `···` → **开发调试 → 打开调试**（否则域名校验拦接口；电脑上的「不校验合法域名」只管模拟器）。小程序没有可分享链接，只能预览/体验版。

## 本机环境
- MySQL 8.0 `localhost:3306` root/`9999` 库 `owner`；Redis `6379` **无密码**；无 Docker。
- shell 有 `SERVER__PORT`（启动要显式 `--server.port=41452`）与透明代理 `http_proxy=127.0.0.1:64304`（curl 探本机加 `--noproxy '*'`），二者非持久。
- **终端 `java` 是 Java 17**：手动跑 jar 必须 `/d/soft/jdk25/jdk-25_windows-x64_bin/jdk-25.0.4/bin/java.exe`。
- 页面文件仅 ~1.1GB，打包/起 JVM 可能 `commit_memory failed (1455)`，释放内存即可。
- npm 走 `registry.npmmirror.com`；Git HTTPS remote，依赖 `.gitconfig` 代理 `127.0.0.1:33210`。
- 停后端：`Get-CimInstance Win32_Process -Filter "Name='java.exe'" | ? { $_.CommandLine -like '*owner-server.jar*' } | % { Stop-Process -Id $_.ProcessId -Force }`（打包报 `Unable to rename` 时先停它）。
- 视觉验证 `.workbuddy/tmp/qr/shot.mjs`：Chrome 须由 node fork；`SHOT_PATH=admin` 不带前导斜杠；表格行用 `tr.ant-table-row`。
