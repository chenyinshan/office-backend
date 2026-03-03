## OA 本地运行与 Docker 部署一览

本文件说明两件事：

- **用 Docker 启动 Nacos / MySQL / Redis / MinIO 等中间件**
- **四个后端模块（gateway / portal / user / workflow）如何本地启动，如何打成镜像并用 Docker 运行**

> 约定：所有命令均在项目根目录 `office-backend` 下执行。

---

## 一、用 Docker 启动中间件（Nacos / MySQL / Redis / MinIO）

### 1. 启动（含 Nacos）

```bash
cd deploy
docker compose --profile nacos up -d
```

- 会启动：
  - `middleware-mysql`（MySQL 8.0，映射到本机 `3306`）
  - `middleware-redis`（Redis，映射到本机 `6379`）
  - `middleware-minio`（MinIO，映射 `9000/9001`）
  - `middleware-nacos`（Nacos 2.3，映射 `8848/9848/9849`）
- Nacos 配置：
  - `MODE=standalone`
  - `SPRING_DATASOURCE_PLATFORM=embedded`（**使用内置存储，不依赖外部 MySQL**）
  - 关闭鉴权：`NACOS_AUTH_ENABLE=false`

### 2. 状态检查

查看容器状态与端口：

```bash
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
```

预期能看到（示意）：

```text
middleware-mysql   Up ... (healthy)   0.0.0.0:3306->3306/tcp ...
middleware-redis   Up ...             0.0.0.0:6379->6379/tcp
middleware-minio   Up ...             0.0.0.0:9000-9001->9000-9001/tcp
middleware-nacos   Up ...             0.0.0.0:8848->8848/tcp, 0.0.0.0:9848-9849->9848-9849/tcp
```

简单连通性测试（Nacos 控制台）：

```bash
curl -I http://localhost:8848/nacos/
```

若返回 `HTTP/1.1 200` 即表示 8848 可用，浏览器访问 `http://localhost:8848/nacos` 即可。

### 3. 停止中间件

```bash
cd deploy
docker compose --profile nacos down
```

---

## 二、本地启动四个后端模块（不用 Docker 跑服务）

前提：已按「一、用 Docker 启动中间件」启动 MySQL / Redis / MinIO / Nacos。

### 1. 编译并安装依赖模块

```bash
mvn -pl oa-api,oa-common,oa-user-service,oa-workflow-service,oa-portal,oa-gateway -am install -DskipTests
```

说明：

- 只需执行一次（或有代码改动时重跑），会将各模块安装到本地 Maven 仓库。

### 2. 分别启动四个服务（每个服务一个终端）

**终端 1：用户服务 `oa-user-service`（端口 8082）**

```bash
mvn -pl oa-user-service spring-boot:run
```

**终端 2：审批流服务 `oa-workflow-service`（端口 8083）**

```bash
mvn -pl oa-workflow-service spring-boot:run
```

**终端 3：门户聚合层 `oa-portal`（端口 8081）**

待 8082、8083 启动完成后执行：

```bash
mvn -pl oa-portal spring-boot:run
```

**终端 4：网关 `oa-gateway`（端口 8080）**

待 8081 启动完成后执行：

```bash
mvn -pl oa-gateway spring-boot:run
```

启动成功后：

- 网关地址：`http://localhost:8080`
- Portal 内部通过 Rest / Dubbo 转发到 user / workflow

> 若启动过程中出现 Nacos 相关错误（如 `Failed to create nacos config service client`），优先检查：
> - Nacos 容器是否为 `Up` 状态；
> - `curl -I http://localhost:8848/nacos/` 是否能连通；

---

## 三、打包为 Docker 镜像并用 Docker 运行四个服务

本项目已经提供了 Dockerfile 与组合好的 `docker-compose.oa.yml`，用于打包并在 Docker 中运行四个后端服务。

### 1. 打包 Jar

先在项目根目录执行（会生成各模块的可执行 Jar）：

```bash
mvn package -DskipTests
```

### 2. 启动基础中间件（MySQL / Redis / MinIO / Nacos）

若尚未启动，使用第一节的命令：

```bash
cd deploy
docker compose --profile nacos up -d
```

确保 `middleware-mysql`、`middleware-redis`、`middleware-minio`、`middleware-nacos` 均为 `Up` 状态。

### 3. 构建四个 OA 服务的镜像

在 `deploy` 目录下执行：

```bash
cd deploy
docker compose -f docker-compose.yml -f docker-compose.oa.yml build
```

说明：

- 会根据以下 Dockerfile 构建镜像：
  - `deploy/docker/Dockerfile.user`         → `oa-user-service:local`
  - `deploy/docker/Dockerfile.workflow`     → `oa-workflow-service:local`
  - `deploy/docker/Dockerfile.portal`       → `oa-portal:local`
  - `deploy/docker/Dockerfile.gateway`      → `oa-gateway:local`

### 4. 启动 OA 四个服务容器

同样在 `deploy` 目录：

```bash
docker compose -f docker-compose.yml -f docker-compose.oa.yml up -d oa-user-service oa-workflow-service oa-portal oa-gateway
```

启动完成后：

- `oa-user-service` 映射：`localhost:8082`
- `oa-workflow-service` 映射：`localhost:8083`
- `oa-portal` 映射：`localhost:8081`
- `oa-gateway` 映射：`localhost:8080`

前端（如 `oa-web`）只需将 API 代理到 `http://localhost:8080` 即可。

### 5. 停止 OA 服务容器

```bash
cd deploy
docker compose -f docker-compose.yml -f docker-compose.oa.yml down
```

若只想停四个 OA 服务、保留中间件，可单独 stop 对应容器：

```bash
docker stop oa-gateway oa-portal oa-user-service oa-workflow-service
```

---

## 四、本地启动前端（Vite + Node）

前端代码在 `oa-web` 目录，默认使用 Vite 开发服务器，端口 `5173`。

### 1. 安装依赖

```bash
cd /Users/Ethan/Desktop/office-backend/oa-web
npm install
```

> 只需首次或依赖变更时执行一次。

### 2. 启动前端开发服务器

确保后端网关 `oa-gateway` 已在本地 `8080` 端口启动，然后执行：

```bash
cd /Users/Ethan/Desktop/office-backend/oa-web
npm run dev
```

启动成功后，浏览器访问：

- Vite 默认地址：`http://localhost:5173`
- Vite 会将 `/api`、`/ws` 请求代理到 `http://localhost:8080`（网关），无需额外配置。

如需修改端口或代理规则，可查看并调整 `oa-web/vite.config.*` 中的 devServer 配置。

---

## 五、小结

- **开发快速验证**：推荐用 Docker 启动中间件（MySQL / Redis / MinIO / Nacos），后端四服务用 `spring-boot:run` 本地跑，再在 `oa-web` 中跑 `npm run dev`，即可完成前后端联调。
- **一键演示 / 部署**：推荐用 `docker-compose.yml + docker-compose.oa.yml` 启动完整后端栈；前端只需对接 `http://localhost:8080`，或按需单独构建前端静态资源进行部署。

