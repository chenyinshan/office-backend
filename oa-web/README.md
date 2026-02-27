# OA 办公平台（Office Backend / OA-Web）

一个用于展示后端能力的 **轻量级 OA 平台**：包含登录鉴权、审批流、公告/通知、组织架构、报表统计，以及 **短连接 + SSE + WebSocket** 的长短连接演示。前端基于 React / Ant Design，对接后端多服务集群，可很容易改造成其他业务领域的后台平台（电商运营、工单、教育管理等）。

---

## 一、整体架构简述

- **前端单页应用**：`oa-web`（React + Vite + TS + Ant Design）
- **API 网关**：`oa-gateway`（Spring Cloud Gateway，统一入口、鉴权、路由）
- **聚合层 BFF**：`oa-portal`（聚合 user / workflow，多接口组合返回给前端）
- **用户与组织服务**：`oa-user-service`（用户、部门、岗位、员工、公告、站内通知）
- **审批流服务**：`oa-workflow-service`（流程实例、任务、审批记录、附件）  
- **中间件（通过 Docker Compose 启动）**：MySQL、Redis、Nacos、RocketMQ、Sentinel、Prometheus/Grafana、XXL-JOB、MinIO 等

前端所有 API 默认走 `/api/**` 到网关 8080，由网关转发到 `oa-portal`，再由 portal 转发或聚合 user/workflow 的接口。

---

## 二、各模块作用（可复用为其他领域后台骨架）

- **oa-web**
  - 登录 / 退出，Token 本地存储，Axios 拦截器自动挂 `Authorization`。
  - 菜单与页面：我的待办、我发起的、实例详情 + 审批记录、公告列表/详情、组织管理、报表统计、长/短连接演示页。
  - 长连接演示：`ConnectionDemo` 页面展示 REST 短连接、SSE 长连接、WebSocket 长连接 + 服务端推送。

- **oa-gateway**
  - `AuthGlobalFilter`：统一入口鉴权。除 `POST /api/auth/login` 外，所有 `/api/**` 必须带 Bearer Token，否则直接 401。
  - 统一对外暴露端口（默认 8080），前端只需要认识这一层。

- **oa-portal**
  - 面向前端的 **BFF 聚合层**：对接 user / workflow，多接口聚合为前端友好的 VO。
  - `AuthFilter` 再次校验 Token（调用 user-service `/api/auth/me`）。
  - 提供 `/api/sse/notify`（SSE）和 `/ws/demo`（WebSocket）长连接示例。

- **oa-user-service**
  - 用户、部门、岗位、员工基础数据。
  - 公告发布/下架（Notice）+ 站内通知（Notification），结合前端消息红点和站内消息页。

- **oa-workflow-service**
  - 审批流引擎：流程实例（WorkflowInstance）、任务（WorkflowTask）、审批记录（TaskLog）。
  - 支持请假单（LeaveApply）、报销单（ExpenseApply）两类业务示例，可很容易替换为订单/工单/合同等其他单据。

这套架构可以看成「通用后台骨架」：只要把请假/报销等领域模型替换掉，就能迁移到电商运营后台、工单平台、教育管理后台等不同业务场景。

---

## 三、环境准备

- **后端**：
  - JDK 17+
  - Maven 3.8+
  - Docker / Docker Compose（推荐，用于一键拉起 MySQL 等中间件）
- **前端**：
  - Node.js 18+（建议 LTS）
  - npm 或 pnpm（本仓库示例使用 npm）

---

## 四、后端编译 & 启动

在项目根目录（`office-backend`）执行：

```bash
# 1. 启动中间件（可选，若本机已有同类服务可自行配置）
docker compose -f deploy/docker-compose.oa.yml up -d

# 2. 编译后端各模块（首次或有代码改动时）
mvn -pl oa-user-service,oa-workflow-service,oa-portal,oa-gateway -am package -DskipTests

# 3. 分别启动服务（示例：使用 spring-boot:run）
mvn -f oa-user-service/pom.xml spring-boot:run
mvn -f oa-workflow-service/pom.xml spring-boot:run
mvn -f oa-portal/pom.xml spring-boot:run
mvn -f oa-gateway/pom.xml spring-boot:run
```

默认端口：

- `oa-gateway`：8080（前端 `/api/**` 与 `/ws/**` 的统一入口）
- `oa-portal`：8081（网关转发到此处）
- `oa-user-service`：8082
- `oa-workflow-service`：8083

> 也可以通过 Dockerfile 为各服务构建镜像，再由 `docker-compose.oa.yml` 一起拉起，方便演示和水平扩展。

---

## 五、前端开发 & 构建

在 `oa-web` 目录下：

```bash
# 1. 安装依赖（首次）
npm install

# 2. 启动开发服务器
npm run dev
```

开发模式下访问：`http://localhost:5173`，Vite 通过 `vite.config.ts` 将 `/api` 和 `/ws` 代理到 `http://localhost:8080`。

构建生产版本：

```bash
npm run build
```

构建产物在 `dist/` 目录，可由 Nginx 等静态服务器托管，API 请求继续代理到网关 8080。

---

## 六、主要功能一览（前端）

- 登录 / 退出（示例账号：
  - `admin`：系统管理员，拥有公告发布、组织管理等权限
  - `leader`：部门负责人角色，用于演示多级审批场景
  - 默认密码：`123456`）
- 审批流：
  - 我的待办：查看当前用户待审批任务，支持通过/驳回
  - 我发起的：查看自己发起的流程实例列表
  - 实例详情：查看业务字段 + 审批记录
- 业务单据：发起请假、发起报销，并上传附件
- 公告与通知：公告列表/详情、站内消息列表 + 红点提醒
- 组织管理：部门树、岗位、员工管理
- 报表统计：请假/报销统计展示与导出
- 长/短连接演示：REST vs SSE vs WebSocket 的交互效果对比页（`ConnectionDemo`）

---

## 七、如何改造成其他业务领域后台平台

保留现有骨架能力：认证与鉴权、API 网关、审批流引擎、组织模型、公告/通知中心、SSE/WebSocket 长连接。
将请假/报销等表结构替换为目标领域的业务单据（如订单、工单、合同、采购单等）。
根据新领域调整前端业务页面（列表、详情、审批表单），共用 Layout、登录、通知、组织管理等通用部分。

通过这种方式，可以快速从「OA 平台」演进为电商运营后台、工单系统或教育管理后台，同时最大化复用现有代码与架构。
