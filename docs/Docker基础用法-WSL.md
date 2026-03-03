# Docker 基础用法（WSL）

> 面向「不会 Docker、用 WSL」的入门教程，只讲把中间件跑起来所需的最少命令和概念。学完后能执行 `docker-compose up -d`、看懂并改端口/密码即可。

---

## 一、在 WSL 里装 Docker

### 1.1 方式 A：WSL2 内直接装（推荐）

在 WSL 终端里执行（以 Ubuntu 为例）：

```bash
# 更新并装依赖
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg

# 添加 Docker 官方 GPG 并加源
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 安装 Docker Engine + Compose 插件
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

装好后：

```bash
# 把当前用户加入 docker 组，避免每次 sudo
sudo usermod -aG docker $USER
# 然后退出终端再重新打开，或执行：newgrp docker

# 验证
docker --version
docker compose version
```

若提示 `Cannot connect to the Docker daemon`，先启动服务：

```bash
sudo service docker start
```

---

### 1.2 方式 B：用 Windows 上的 Docker Desktop（WSL 集成）

1. 在 Windows 安装 [Docker Desktop](https://docs.docker.com/desktop/install/windows-install/)。
2. 安装时勾选 **Use WSL 2 based engine**，并在 **Settings → Resources → WSL Integration** 里启用你的 WSL 发行版。
3. 在 WSL 终端里即可直接用 `docker`、`docker compose`（无需在 WSL 里再装 Docker Engine）。

---

## 二、三个核心概念（够用即可）

| 概念 | 一句话 |
|------|--------|
| **镜像（Image）** | 只读模板，例如 `mysql:8.0`、`redis:7`，相当于“安装包”。 |
| **容器（Container）** | 镜像跑起来后的实例，相当于“正在运行的程序”；可以起多个容器来自同一个镜像。 |
| **docker-compose** | 用一份 YAML 描述“多个容器 + 端口 + 数据卷 + 依赖”，一条命令一起起、一起停。 |

我们起中间件时：**不用手写 `docker run` 一长串**，只维护一个 `docker-compose.yml`，用 `docker compose up -d` 即可。

---

## 三、必会命令（6 个）

在**包含 docker-compose.yml 的目录**下执行。

| 命令 | 作用 |
|------|------|
| `docker compose up -d` | 按 compose 文件**后台启动**所有服务（-d 即 detach）。 |
| `docker compose down` | **停止并删除**本次 compose 起的容器（数据卷可选保留）。 |
| `docker compose ps` | 查看当前 compose 里各容器的**状态**（Up/Exit）。 |
| `docker compose logs -f [服务名]` | 看某个服务的**日志**（-f 持续刷，Ctrl+C 退出）。 |
| `docker compose restart [服务名]` | **重启**某一个服务。 |
| `docker compose pull` | 拉取 compose 里用到的**最新镜像**（可选，一般 up 时会自动拉）。 |

---

## 四、compose 里常改的两样

打开 `docker-compose.yml`，会看到类似：

```yaml
services:
  mysql:
    image: mysql:8.0
    ports:
      - "3306:3306"        # 左边是宿主机端口，右边是容器内端口
    environment:
      MYSQL_ROOT_PASSWORD: your_password   # 改这里
```

- **改端口**：把 `"3306:3306"` 左边改成你本机未占用的端口，例如 `"3307:3306"`。
- **改密码**：改 `MYSQL_ROOT_PASSWORD`（或对应服务的 `xxx_PASSWORD`）即可。

改完保存，执行：

```bash
docker compose down
docker compose up -d
```

---

## 五、常用调试命令（可选）

| 命令 | 作用 |
|------|------|
| `docker ps` | 列出当前**所有运行中的容器**。 |
| `docker ps -a` | 列出所有容器（含已停止的）。 |
| `docker logs <容器名或ID>` | 看某个容器的日志（和 `docker compose logs 服务名` 类似）。 |
| `docker exec -it <容器名或ID> bash` | 进入容器内部敲命令（排查时用）；退出用 `exit`。 |
| `docker compose down -v` | 停止并删除容器，**并删除 compose 里声明的卷**（慎用，会清数据）。 |

---

## 六、和你项目的关系

- 本仓库在 **deploy** 目录提供 **docker-compose.yml**，把 Nacos、MySQL、Redis、RocketMQ、ES、Zipkin、Prometheus、Grafana、XXL-JOB、Sentinel、MinIO 等写在一起。
- 在项目根目录执行：
  - `docker compose -f deploy/docker-compose.yml up -d` → 中间件全部起来；
  - `docker compose -f deploy/docker-compose.yml ps` → 看是否都是 Up；
  - 本机应用连 `localhost:对应端口` 即可。
- 详细端口与账号见本文**第八节**。不需要会写 Dockerfile、不需要懂网络细节，会**安装 + 上面 6 个命令 + 改端口/密码**就够用。

---

## 七、自测：跑一个 Redis

在 WSL 里：

```bash
docker run -d --name my-redis -p 6379:6379 redis:7
```

然后：

```bash
docker ps          # 能看到 my-redis 在跑
docker stop my-redis
docker rm my-redis
```

能跑通、能停、能删，说明 Docker 可用。接下来就可以用 `docker compose` 一把起整套中间件了。

---

## 八、本项目中间件一键启动

本仓库在 **deploy** 目录下提供了一键启动所有中间件的 `docker-compose.yml`，与 `docs/backend-stack-summary.md` 第十节对应。

### 8.1 启动与停止

在**项目根目录**执行（推荐）：

```bash
docker compose -f deploy/docker-compose.yml up -d
```

或先进入 deploy 再执行（此时可省略 `-f`）：

```bash
cd deploy
docker compose up -d
```

查看状态：

```bash
docker compose -f deploy/docker-compose.yml ps
```

**说明**：RocketMQ Broker 已放入 profile `rocketmq`，默认 `up -d` **不会**启动它（避免当前 5.x 镜像在 Docker 下反复重启）。Nacos 已放入 profile `nacos`，默认也不会启动（当前镜像存在 db 配置问题，后续修好再启用）。需要起 Broker 时执行：`docker compose -f deploy/docker-compose.yml --profile rocketmq up -d`；需要起 Nacos 时：`docker compose -f deploy/docker-compose.yml --profile nacos up -d`。

停止并删除容器（数据卷保留）：

```bash
docker compose -f deploy/docker-compose.yml down
```

### 8.2 端口与默认账号一览

| 中间件 | 端口 | 默认账号/说明 |
|--------|------|----------------|
| MySQL | 3306 | root / root123（可在 deploy/.env 或环境变量中改 `MYSQL_ROOT_PASSWORD`） |
| Redis | 6379 | 无密码 |
| Nacos | 8848（控制台） | 默认 nacos/nacos；鉴权 Token 见 deploy/.env.example |
| RocketMQ NameServer | 9876 | — |
| RocketMQ Broker | 10909, 10911, 10912 | — |
| Elasticsearch | 9200（HTTP） | 当前配置未开启安全，无账号 |
| Zipkin | 9411 | — |
| Prometheus | 9090 | — |
| Grafana | 3000 | admin / admin（可改 `GRAFANA_ADMIN_*`） |
| XXL-JOB | 8081（映射容器 8080） | admin / 123456 |
| Sentinel Dashboard | 8858 | sentinel / sentinel |
| MinIO API / 控制台 | 9000 / 9001 | minioadmin / minioadmin（可改 `MINIO_ROOT_*`） |

本机应用连接时使用 **localhost:上表端口**；若应用也跑在 Docker 同一 compose 网络中，则用**服务名**作主机（如 `mysql`、`redis`、`nacos`）。

### 8.3 自定义密码与配置

复制 `deploy/.env.example` 为 `deploy/.env`，按需修改后再执行：

```bash
docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d
```

MySQL 首次启动时会执行 `deploy/mysql-init/01_xxl_job.sql` 初始化 XXL-JOB 库；若需重新初始化，需先删卷：`docker compose -f deploy/docker-compose.yml down -v`，再 `up -d`（会清空所有中间件数据，慎用）。

---

*有问题把报错贴出来，按报错逐条排查即可。*
