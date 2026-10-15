# Owner IntelliPortal 项目长期约定

## 基本信息
- 工作区：`D:/code/onlyone`
- 多模块 Maven 工程，groupId `com.owner`，版本 `2026.03-SNAPSHOT`
- 模块：`owner-common`（通用）/ `owner-framework`（框架）/ `owner-system`（业务）/ `owner-server`（启动）
- 前端工程：`owner-web`（React 19 + Vite + TS + antd 6），dev 端口 5273，容器 8080
- 服务端口固定 **41452**，应用名 `owner-server`，Quartz 调度器实例名 `OwnerScheduler`
- JDK 25，编译 `release=25`，UTF-8；Maven 用工程自带 `./mvnw`（3.9.11，本机 Maven 3.6.3 过旧）

## 命名约定（2026-10-14 全量重命名）
- 历史上所有 `mt*` 前缀已统一改为 `owner`，**不要**再引入 `mtl` / `mtlntelliportal` / `MTL` 等旧前缀
- 模块 `owner-*`；包 `com.owner.*`；配置前缀 `owner.*`（websocket/druid/quartz/thread-pool/web）
- 常量前缀 `OWNER_`（如 `OWNER_WEBSOCKET_BROADCAST`）；线程名前缀 `owner-async-` / `owner-scheduling-`
- 容器/网络：`owner-mysql` / `owner-redis` / `owner-rocketmq-*` / `owner-net`；数据库 `owner`
- 口令统一 `owner@2026`（Docker 的 MySQL/Redis 与 Druid 监控台）
- 品牌名统一写作 **Owner IntelliPortal**（README 标题、前端 title/登录页/侧边栏/页脚）
- 启动 banner：`owner-server/src/main/resources/banner.txt`，大字用 figlet `standard` 字体写 **OWNER**，
  副标题行 `:: Owner IntelliPortal ::` + 右侧 `Spring Boot ${...}`
- **改名/替换后的残留复查必须全量扫描，不要用扩展名白名单**：
  ```bash
  grep -rniE "<旧词>" . --exclude-dir=node_modules --exclude-dir=target \
    --exclude-dir=dist --exclude-dir=.git --binary-files=without-match
  ```
  上次只扫了 `*.java/xml/yml/md/sql/ts/json/conf/sh`，漏掉 **`banner.txt`**（`.txt` 不在白名单），
  误报"零残留"，被用户在启动日志里一眼看出来。
  `scripts/rename-to-owner.py` 里保留旧词是正确的（它是迁移记录，不该改）

## 技术约定
- 统一响应：`com.owner.common.core.domain.R<T>`；分页 `PageResult` / `PageQuery`
- 业务异常统一抛 `ServiceException`，由 `GlobalExceptionHandler` 转 `R.fail`
- JSON 统一用 `JsonUtils`（Jackson 3 / `tools.jackson`），不要再引 Jackson 2 的 databind
- Redis 缓存用 `GenericJacksonJsonRedisSerializer` + `enableDefaultTyping`，缓存名 `sysUser`
- 新增/修改表结构时同步更新 `sql/init.sql`（含 QRTZ_* 表）
- 业务代码放 `owner-system`，`controller / service / mapper / domain(entity,dto,vo) / convert(MapStruct)`

## 前端约定（owner-web）
- 目录：`api`（request/portal/user/auth）、`hooks`、`layout`、`pages`、`router`、`store`、`types`、`utils`
- `src/api/request.ts` 的响应拦截器负责拆包 `R`，业务代码直接拿 `data`
- antd v6 用 `App.useApp()` 取 message，经 `src/utils/feedback.ts` 桥接给非组件模块
- 开发期由 Vite 代理 `/api`、`/actuator`、`/v3/api-docs`、`/doc.html`、`/ws` 到 41452
- 登录无真实认证接口，`src/api/auth.ts` 复用 `sys_user` 分页查询校验账号（密码不校验）
- `SysUserVO` 已补 `version` 字段，供前端「编辑」回传以走乐观锁
- **雪花 ID 一律用 `string`**：后端主键为 MyBatis-Plus `IdType.ASSIGN_ID`，19 位整数
  超出 JS `Number.MAX_SAFE_INTEGER`（16 位）。后端由 `owner-framework/config/JacksonConfig`
  注册 `Long -> String` 序列化器，前端 `SysUser.id` / `SysUserUpdatePayload.id`
  及 `getUserById/removeUser/pushWelcome` 签名全部为 `string`。
  区分：包装类型 `Long` 序列化为字符串，基本类型 `long`（`PageResult.total`、`R.timestamp`）仍是数字
- **测试脚本坑**：Git Bash 不认 `D:/...` 形式的 PATH 项，调 java 必须用绝对路径
  `/d/soft/.../bin/java.exe`（否则命中 Oracle javapath 的 Java 17，报 UnsupportedClassVersionError）

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

## 本地运行环境（本机）
- MySQL 8.0：`localhost:3306`，root / `9999`，库 `owner`（旧库 `mtlntelliportal` 已废弃）
- Redis：`localhost:6379`，**无密码**（配置中不要出现 `spring.data.redis.password`）
- 无 Docker、无 RocketMQ（本地 profile 通过不配置 `rocketmq.name-server` 关闭其自动配置）
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
