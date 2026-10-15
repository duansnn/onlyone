```markdown
# PRD 产品需求文档
项目名称：AI轻量化任务看板
> 前后端分离全栈作品集项目
技术栈：
前端：Vue3 + Element Plus + ECharts
后端：Node.js(Express/NestJS) / SpringBoot
数据库：MySQL
AI服务：通义千问 / DeepSeek API
部署：前端Vercel，后端阿里云轻量服务器

## 1. 用户角色
- 普通登录用户：创建项目、管理任务、使用AI能力、查看统计报表、导入导出任务数据
> MVP不开发管理员、成员权限模块，减少开发工作量。

## 2. 功能需求清单
> 🟢 MVP必做（优先开发）
> 🟡 V1.1 AI模块（第二阶段开发）
> ⚪ 可选迭代功能（时间充裕再开发，MVP不做）

### 模块1：账号模块 🟢
1. 用户注册：填写用户名、密码；密码bcrypt加密存入数据库
2. 用户登录：JWT鉴权，保持登录状态
3. 退出登录：销毁前端token，跳转登录页
> 不做短信验证码、密码找回功能，降低开发量

### 模块2：项目管理 🟢
项目是任务父级，一个用户可以创建多个独立项目（作品集开发、求职准备、房屋装修等）
项目字段：
- id：项目主键ID
- user_id：所属用户ID
- name：项目名称
- description：项目描述
- create_time：创建时间
- update_time：更新时间

功能：
1. 新建项目
2. 编辑项目名称、描述
3. 删除项目：二次确认弹窗，删除项目会级联删除项目下全部任务
4. 项目列表，支持点击切换不同项目

### 模块3：任务看板核心 🟢
看板分为3列：待办(todo) / 进行中(doing) / 已完成(done)
任务卡片字段：
- id：任务主键ID
- project_id：所属项目ID
- title：任务标题
- description：任务详情
- due_date：截止日期
- priority：优先级 high/medium/low（高/中/低）
- status：任务状态 todo/doing/done
- tags：标签，多个标签逗号分隔
- create_time：创建时间
- update_time：更新时间

功能：
1. 新增任务：弹窗表单填写任务全部字段
2. 编辑任务：修改任务任意字段
3. 删除任务：弹窗确认删除
4. 拖拽卡片：拖动任务卡片，自动更新任务状态并保存后端
5. 任务筛选：按优先级、截止日期、标签筛选
6. 任务搜索：关键词搜索任务标题、描述

### 模块4：数据统计看板 🟢
独立统计页面，ECharts可视化图表
1. 任务状态饼图：待办、进行中、已完成任务数量占比
2. 逾期任务统计：截止日期早于当前日期，标记逾期任务
3. 趋势折线图：一段时间内新增任务、完成任务数量趋势

### 模块5：导入导出 🟢
1. 导出：当前项目所有任务导出JSON文件备份
2. 导入：上传JSON文件，批量导入任务至当前项目

### 模块6：AI能力模块 🟡
> AI全部为手动点击触发，不会自动调用大模型接口
1. AI任务拆解
用户输入目标描述，AI返回多条子任务信息；一键批量创建任务卡片放入【待办】列。
> 面试核心演示功能
2. AI生成项目周报
选择时间范围，读取该时间段项目内全部任务，AI自动生成项目进度周报文本，支持复制。
3. AI任务描述润色
选中单个任务，一键调用AI优化任务描述文案。

### 模块7：可选迭代功能 ⚪
1. 任务评论
2. 站内逾期消息提醒
3. 多人协作、成员权限管理

## 3. 页面清单
1. 登录/注册页面
2. 首页：项目列表页面
3. 看板主页面（核心拖拽页面）
4. 任务统计图表页面
5. AI助手弹窗
6. 导入导出弹窗

## 4. 非功能需求
1. 性能：常规接口响应 <500ms；AI接口5~15s响应，加载动画提示用户等待
2. 浏览器兼容：Chrome、Edge主流浏览器
3. 安全要求
    - 用户密码bcrypt加密存储
    - JWT鉴权，所有后端接口做登录拦截
    - 前端输入内容做XSS过滤
4. 用户提示：操作成功/失败全局弹窗；删除等高风险操作二次确认

## 5. 数据库表设计 SQL
​```sql
-- 用户表 user
CREATE TABLE `user` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT '用户id',
  `username` varchar(50) NOT NULL UNIQUE COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '加密后的密码',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 项目表 project
CREATE TABLE `project` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `name` varchar(100) NOT NULL COMMENT '项目名称',
  `description` text COMMENT '项目描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 任务表 task
CREATE TABLE `task` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `project_id` bigint NOT NULL COMMENT '所属项目ID',
  `title` varchar(200) NOT NULL COMMENT '任务标题',
  `description` text COMMENT '任务详情',
  `due_date` date COMMENT '截止日期',
  `priority` varchar(20) COMMENT 'high/medium/low',
  `status` varchar(20) NOT NULL COMMENT 'todo/doing/done',
  `tags` varchar(200) COMMENT '标签，逗号分隔',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 6. 后端REST接口清单
### 用户接口
- POST /api/user/register 用户注册
- POST /api/user/login 用户登录

### 项目接口
- GET /api/project/list 获取当前用户全部项目
- POST /api/project 创建新项目
- PUT /api/project/:id 修改项目信息
- DELETE /api/project/:id 删除项目

### 任务接口
- GET /api/task/list?projectId=xxx 获取项目下全部任务
- POST /api/task 新增任务
- PUT /api/task/:id 更新任务（拖拽更新状态也调用此接口）
- DELETE /api/task/:id 删除任务

### AI接口
- POST /api/ai/split-task AI拆解目标生成任务列表
- POST /api/ai/generate-report AI生成项目周报
- POST /api/ai/optimize-task AI润色任务描述

### 导入导出接口
- GET /api/task/export?projectId=xxx 导出任务JSON
- POST /api/task/import?projectId=xxx 上传JSON批量导入任务

## 7. 原型交互简述
1. 登录成功进入项目列表页，卡片展示各个项目（作品集网站开发、求职准备、房屋装修）
2. 点击项目卡片进入看板页面，三列看板展示任务卡片，卡片展示标题、优先级、截止日期
3. 拖拽卡片实时调用后端接口保存任务状态
4. 页面右上角AI助手按钮，弹出输入框，输入目标描述，AI批量生成任务卡片到待办栏
5. 支持打开统计页面查看图表，导入导出JSON备份数据

## 8. 上线与面试演示方案
1. 后端部署阿里云轻量服务器，MySQL数据库
2. 前端打包部署Vercel，公网可访问
3. 编写种子数据脚本：一键预置演示项目+任务，面试演示一键加载测试数据

## 9. 开发排期
1. 第1周：搭建前后端框架、数据库，完成用户、项目、任务基础CRUD接口、基础页面
2. 第2周：看板拖拽、搜索筛选、统计图表、JSON导入导出，完成MVP可演示版本
3. 第3周：开发AI接口，对接大模型API，调试AI生成任务功能，完善交互细节
4. 第4周：UI优化、bug修复、项目部署、编写GitHub README文档