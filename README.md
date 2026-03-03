# 办公平台（Office Backend）

一个用于展示 Java 后端能力的 **平台**：包含登录鉴权、审批流、公告/通知、组织架构、报表统计，整套后端被设计成「通用后台骨架」，很容易替换领域模型后复用到电商运营、工单、教育管理等其他场景。

---

## 一、整体架构

- **前端单页应用**：`oa-web`（React + Vite + TypeScript + Ant Design）
- **API 网关**：`oa-gateway`（Spring Cloud Gateway，统一入口、鉴权、路由）
- **聚合层 BFF**：`oa-portal`（对接 user / workflow，多接口聚合为前端友好的 VO）
- **用户与组织服务**：`oa-user-service`（用户、部门、岗位、员工、公告、站内通知）
- **审批流服务**：`oa-workflow-service`（流程实例、任务、审批记录、附件）
- **公共模块**：`oa-common`（统一 Result、错误码、BizException、全局异常处理等）
- **中间件（Docker Compose 启动）**：MySQL、Redis、Nacos、MinIO、Prometheus/Grafana（自主选择）、XXL-JOB（自主选择）等

对外访问路径统一为 `http://localhost:8080/api/**`（REST）由网关转发到各服务。

---

## 二、模块职责 & 通信用法

- **oa-web**
  - 登录 / 退出：Token 存储在浏览器，Axios 拦截器自动挂 `Authorization: Bearer <token>`。
  - 业务页面：我的待办、我发起的、实例详情 + 审批记录、公告列表/详情、组织管理、报表统计。
  - 长连接演示：`ConnectionDemo` 页面展示 REST 短连接、SSE 长连接（`/api/sse/notify`）、WebSocket 长连接（`/ws/demo`）。

- **oa-gateway**
  - `AuthGlobalFilter`：统一入口鉴权。除 `POST /api/auth/login` 外，所有 `/api/**` 必须带 Bearer Token，否则直接 401。
  - 负责将 `/api/**` 转发到 `oa-portal`，前端只需要感知网关。

- **oa-portal**
  - 面向前端的聚合层（BFF）：对接 `oa-user-service`、`oa-workflow-service`，组合返回 VO。
  - `AuthFilter` 再次调用 `user-service /api/auth/me` 解析 Token，有效才放行。
  - 提供长连接接口：`GET /api/sse/notify`（SSE）、`WS /ws/demo`（WebSocket）。
  - 对下游服务的调用方式：统一使用 **HTTP REST + RestTemplate**。

- **oa-user-service**
  - 用户、部门、岗位、员工管理（组织模型）。
  - 公告（Notice）与站内通知（Notification），与前端消息红点、站内消息页打通。

- **oa-workflow-service**
  - 审批引擎：流程实例（WorkflowInstance）、任务（WorkflowTask）、审批记录（TaskLog）。
  - 目前内置请假单（LeaveApply）、报销单（ExpenseApply）两类业务，可替换为订单/工单/合同等。

> **服务间通信总结**：内部服务之间全部使用 **HTTP REST**，由 `oa-portal` 作为统一调用方；RocketMQ、Dubbo 等在技术栈中预留，但当前示例主要以 HTTP 为主，便于理解与调试。

---

## 三、环境准备

- **后端**
  - JDK 17+
  - Maven 3.8+
  - Docker / Docker Compose（推荐，用于一键拉起 MySQL 等中间件）
- **前端**
  - Node.js 18+（建议 LTS）
  - npm 或 pnpm（示例使用 npm）

---

## 四、后端编译与启动

在项目根目录 `office-backend` 执行：

```bash
# 1. 启动中间件（若本机已有同类服务可按需调整配置）
docker compose -f deploy/docker-compose.oa.yml up -d

# 2. 编译后端各模块（首次或代码有改动时）
mvn -pl oa-user-service,oa-workflow-service,oa-portal,oa-gateway -am package -DskipTests

# 3. 分别启动服务（示例：spring-boot:run）
mvn -f oa-user-service/pom.xml spring-boot:run
mvn -f oa-workflow-service/pom.xml spring-boot:run
mvn -f oa-portal/pom.xml spring-boot:run
mvn -f oa-gateway/pom.xml spring-boot:run
```

默认端口：

- `oa-gateway`：`8080`（统一入口）
- `oa-portal`：`8081`
- `oa-user-service`：`8082`
- `oa-workflow-service`：`8083`

也可以为各服务构建镜像，再由 `deploy/docker-compose.oa.yml` 一起拉起，以模拟多实例与扩容。

---

## 五、前端开发与构建

进入 `oa-web` 目录：

```bash
# 安装依赖（首次）
cd oa-web
npm install

# 启动开发服务器
npm run dev
```

开发模式访问：`http://localhost:5173`  
Vite 会将 `/api` 与 `/ws` 代理到 `http://localhost:8080`（网关）。

构建生产版本：

```bash
npm run build
```

构建产物在 `oa-web/dist/`，可由 Nginx 等静态服务器托管，并继续将 `/api/**` 与 `/ws/**` 代理到网关。

---

## 六、示例账号

- `admin`：系统管理员，拥有公告发布、组织管理等权限
- `leader`：部门负责人角色，用于演示多级审批场景
- 默认密码：`123456`

---

## 七、主要功能一览

- 登录 / 退出
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

## 八、如何改造成其他业务领域后台平台

这套 OA 更像是一套「带审批、组织、通知、实时推送的通用后台骨架」：

1. **保留骨架能力**
   - 认证与鉴权、API 网关、审批流引擎、组织模型、公告/通知中心、SSE/WebSocket 长连接。
2. **替换领域模型**
   - 将请假/报销等表结构替换为订单、工单、合同、采购单、变更单等领域单据。
3. **调整前端业务页面**
   - 重写列表 / 详情 / 审批表单等业务页面，继续共用 Layout、登录、通知、组织管理等公共部分。

通过以上步骤，可以快速从「OA 平台」演进为电商运营后台、工单系统或教育管理后台，同时最大化复用现有代码与架构。
