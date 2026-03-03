# OA 办公管理系统 — 模块与表设计

> 与 `deploy/mysql-init/02_oa.sql`、父 POM 模块列表一致。后续实现业务时按本文档扩展。

---

## 一、模块结构

```
office-platform (父工程)
├── service-demo          # 原有示例服务，保留
├── oa-common              # 公共：统一返回体、异常、常量、工具
├── oa-user-service        # 用户服务：部门、岗位、员工、账号、角色（端口 8082）
├── oa-workflow-service    # 审批流服务：流程定义/实例/待办、请假与报销单（端口 8083）
├── oa-gateway             # 网关：路由、鉴权、限流（端口 8080，依赖后续接入）
└── oa-portal              # 门户/聚合 API：对前端统一入口（端口 8081）
```

### 模块职责简述

| 模块 | 职责 | 主要表/能力 |
|------|------|-------------|
| **oa-common** | 被其他模块依赖，无独立进程 | 无表；提供 Result、BizException、错误码、DTO 基类等 |
| **oa-user-service** | 组织与人员、登录鉴权数据 | sys_dept, sys_post, sys_employee, sys_user_account, sys_role, sys_user_role |
| **oa-workflow-service** | 流程引擎、待办、业务表单 | wf_definition, wf_node, wf_instance, wf_task, wf_task_log, leave_apply, expense_apply |
| **oa-gateway** | 统一入口、路由转发、后续鉴权/限流 | 无表；配置路由到 user/workflow/portal |
| **oa-portal** | 聚合或转发：登录、待办数、我的申请等 | 无表；通过 Feign/Dubbo 调 user、workflow |

---

## 二、数据库设计（库 `oa`）

### 2.1 组织与人员（user-service）

| 表名 | 说明 |
|------|------|
| **sys_dept** | 部门树，parent_id 自关联，支持多级 |
| **sys_post** | 岗位字典，如：普通员工、部门主管、管理员 |
| **sys_employee** | 员工信息，关联 dept_id、post_id，工号唯一 |
| **sys_user_account** | 登录账号，与员工 1:1，username 唯一 |
| **sys_role** | 角色，含数据范围 data_scope（本人/本部门/全部） |
| **sys_user_role** | 用户-角色多对多 |

### 2.2 审批流（workflow-service）

| 表名 | 说明 |
|------|------|
| **wf_definition** | 流程定义，如 process_key=leave/expense，含版本 |
| **wf_node** | 节点定义：node_key、node_type(start/approval/end)、审批人类型、下一节点 |
| **wf_instance** | 流程实例：关联业务类型与 business_id、发起人、当前节点、状态 |
| **wf_task** | 待办任务：实例+节点+处理人+状态(pending/approved/rejected) |
| **wf_task_log** | 审批操作记录：谁在何时做了通过/驳回/转交 |

### 2.3 业务表单（与流程 1:1）

| 表名 | 说明 |
|------|------|
| **leave_apply** | 请假单：请假类型、起止时间、天数、原因、附件 id 列表 |
| **expense_apply** | 报销单：金额、类型、说明、附件 id 列表 |

### 2.4 公告与附件

| 表名 | 说明 |
|------|------|
| **oa_notice** | 公告：标题、正文、发布人、是否置顶、状态、发布时间 |
| **oa_notice_read** | 公告阅读记录：notice_id + user_id 唯一 |
| **oa_attachment** | 附件：业务类型、业务 id、文件名、存储路径(MinIO)、上传人 |

### 2.5 表关系小结

- **流程实例** `wf_instance.business_id` 指向 `leave_apply.id` 或 `expense_apply.id`，业务表通过 `instance_id` 反查流程。
- **待办** `wf_task.assignee_user_id`、**审批记录** `wf_task_log.operator_user_id`、**发起人** `wf_instance.applicant_user_id` 均指 `sys_user_account.id`。
- **附件** `oa_attachment.business_type` + `business_id` 可关联请假单、报销单或公告。

---

## 三、流程节点与初始数据

- **请假流程** `leave`：节点 start → dept_leader（部门主管审批）→ hr（人事审批）→ end。
- **报销流程** `expense`：同上，start → dept_leader → hr → end。
- 初始数据已写入：根部门、三个岗位、三个角色、两条流程定义及对应节点（见 `02_oa.sql` 末尾）。部门主管节点审批人 user_id=2（leader），人事节点审批人 user_id=1（admin），可在 `wf_node.approver_config` 中修改。
- **已部署库升级**：若此前已执行过 `02_oa.sql`，可执行 `deploy/mysql-init/03_oa_workflow_multilevel.sql` 增加人事节点，实现多级审批。

---

## 四、端口与本地运行

| 应用 | 端口 | 启动命令（在项目根目录） |
|------|------|---------------------------|
| oa-gateway | 8080 | `mvn -f oa-gateway/pom.xml spring-boot:run` |
| oa-user-service | 8082 | `mvn -f oa-user-service/pom.xml spring-boot:run` |
| oa-workflow-service | 8083 | `mvn -f oa-workflow-service/pom.xml spring-boot:run` |
| oa-portal | 8081 | `mvn -f oa-portal/pom.xml spring-boot:run` |

**网关**：前端请求统一走网关 8080，网关将 `/api/**` 转发到 portal:8081；鉴权、限流可后续在网关上扩展。  
**无网关时**：前端 vite 代理可改为 `target: http://localhost:8081`，直连 portal。

**workflow 接口约定**：当前无网关时，调用方需在请求头传 `X-User-Id`（`sys_user_account.id`），可选 `X-Employee-Id`（员工 id）。  
- 发起请假：`POST /api/workflow/leave/start`  
- 发起报销：`POST /api/workflow/expense/start`  
- 我的待办：`GET /api/workflow/tasks/pending`  
- 我发起的：`GET /api/workflow/instances/my`  
- 通过：`POST /api/workflow/tasks/{taskId}/approve`  
- 驳回：`POST /api/workflow/tasks/{taskId}/reject`  

**oa-portal（端口 8081）**：前端统一只连 **网关 8080**（或直连 8081），请求头带 `Authorization: Bearer <token>`（token 来自 `POST /api/auth/login`）。网关将 `/api` 转发到 portal；portal 将登录/登出/当前用户转发到 user-service，将审批流转发到 workflow-service，并先用 token 调 user-service `/api/auth/me` 得到 userId/employeeId 再带 `X-User-Id`、`X-Employee-Id` 调 workflow。配置：`app.user-service-url`、`app.workflow-service-url`（默认 8082、8083）。  

先启动 MySQL（`docker compose -f deploy/docker-compose.yml up -d mysql`），执行 `02_oa.sql` 初始化 `oa` 库后，再按需接入 MyBatis-Plus、Redis、Nacos、Gateway 等（参见 `docs/backend-stack-summary.md` 与 `docs/任职资格在项目中的体现方案.md`）。

---

## 五、API 接口说明（经 portal 统一入口）

前端统一请求 `http://portal:8081/api`，请求头带 `Authorization: Bearer <token>`（token 来自登录）。

### 5.1 认证（portal → user-service）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/login | 登录，body: `{ "username", "password" }`，返回 token、user |
| POST | /api/auth/logout | 登出 |
| GET | /api/auth/me | 当前用户信息（userId、username、employeeId、name、deptId 等） |

### 5.2 公告（portal → user-service，带 X-User-Id）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/notices | 已发布公告列表（置顶优先、发布时间倒序） |
| GET | /api/notices/my-drafts | 我的草稿列表（需 ADMIN 角色） |
| GET | /api/notices/{id} | 公告详情；已发布所有人可看，草稿仅作者可看；`?read=1` 同时标记已读 |
| POST | /api/notices | 发布公告，body: `{ "title", "content", "isTop"? }`（需 ADMIN） |
| POST | /api/notices/draft | 保存草稿，body: `{ "title", "content"?,"isTop"? }`（需 ADMIN） |
| PUT | /api/notices/{id} | 更新公告，body: `{ "title"?,"content"?,"isTop"? }`（草稿或已发布的作者可改） |
| POST | /api/notices/{id}/publish | 发布草稿（仅本人草稿） |
| POST | /api/notices/{id}/unpublish | 下架公告（已发布改为草稿，本人或 ADMIN） |
| DELETE | /api/notices/{id} | 删除：草稿仅作者可删，已发布仅 ADMIN 可删 |
| POST | /api/notices/{id}/read | 标记已读 |

### 5.3 审批流（portal → workflow-service，带 X-User-Id、X-Employee-Id）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/workflow/leave/start | 发起请假，body: `leaveType, startTime, endTime, days, reason?, attachmentIds?` |
| POST | /api/workflow/expense/start | 发起报销，body: `totalAmount, expenseType?, description?, attachmentIds?` |
| GET | /api/workflow/tasks/pending | 我的待办 |
| GET | /api/workflow/tasks/my | 我的任务（待办+已处理），?status=pending\|approved\|rejected |
| GET | /api/workflow/instances/my | 我发起的流程 |
| GET | /api/workflow/stats/leave | 请假统计，?startDate=&endDate= 格式 yyyy-MM-dd，仅已完成 |
| GET | /api/workflow/stats/expense | 报销统计，同上 |
| POST | /api/workflow/tasks/{taskId}/approve | 通过，body: `{ "comment"? }` |
| POST | /api/workflow/tasks/{taskId}/reject | 驳回，body: `{ "comment"? }` |

### 5.4 附件上传（portal 透传 multipart → workflow-service）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/workflow/attachments/upload | multipart/form-data：`file`（文件）、`businessType`（leave/expense），返回附件 id 等信息 |

- 前端需以 **FormData** 发送，且**不要**设置 `Content-Type`（由浏览器自动带 boundary）。  
- MinIO 桶名需满足 S3 规范（如 `oa-attachments`，至少 3 字符）。

### 5.5 统一返回与错误

- 成功：`{ "code": 0, "message": "成功", "data": ..., "success": true }`  
- 失败：`{ "code": 非0, "message": "错误说明", "data": null, "success": false }`  
- 常见 code：10002 未登录、10003 无权限、20003 用户名或密码错误、3xxxx 流程相关。

### 5.6 鉴权与权限

- **统一鉴权**：portal 对除 `POST /api/auth/login` 外的所有 `/api/**` 请求校验 `Authorization: Bearer <token>`；token 无效或未传时返回 401（code 10002）。
- **登录/me 返回**：`/api/auth/me` 及登录响应中的 `user` 含 `roles` 数组（角色编码，如 `["ADMIN","STAFF"]`），前端可根据 `roles` 控制按钮显隐。
- **发布公告**：仅拥有 **ADMIN** 角色的用户可调用 `POST /api/notices` 发布公告、`POST /api/notices/draft` 保存草稿、`GET /api/notices/my-drafts` 查看草稿箱，否则返回 403（code 10003）。草稿仅作者可查看/编辑/发布/删除；已发布公告作者或 ADMIN 可编辑/下架，仅 ADMIN 可删除。审批仍按流程节点配置的审批人执行（谁被指派谁审批）。

---

## 六、部署说明（简要）

- **中间件**：`cd deploy && docker compose -f docker-compose.yml up -d mysql minio`（MySQL 3306，MinIO 9000/9001）。  
- **后端**：在项目根目录 `mvn install -DskipTests` 后，依次启动 oa-user-service（8082）、oa-workflow-service（8083）、oa-portal（8081）、**oa-gateway（8080）**。  
- **前端**：`cd oa-web && npm run dev`，访问 http://localhost:5173，代理 `/api` 到 **gateway:8080**（或直连 portal:8081）。  
- 网关将 `/api/**` 转发到 portal:8081，后续可在网关上扩展鉴权、限流。  

详细步骤与常见问题见 **`docs/OA本地测试步骤.md`**。

---

## 七、后续实现顺序建议

1. **oa-common**：统一返回体 `Result<T>`、业务异常 `BizException`、错误码枚举。
2. **oa-user-service**：MyBatis-Plus 接入，部门/员工/账号/角色 CRUD，登录接口（JWT 可后续加）。
3. **oa-workflow-service**：流程引擎核心（根据 current_node 查下一节点、创建待办、审批后推进），请假/报销发起与审批接口。
4. **oa-portal**：登录、我的待办数、我的申请列表等聚合接口（通过 HTTP 或 Feign 调 user/workflow）。
5. **oa-gateway**：Spring Cloud Gateway + Nacos 路由到各服务，再接入鉴权与限流。
