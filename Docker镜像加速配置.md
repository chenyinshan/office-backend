# Docker 镜像加速配置（方案一）

当 `docker pull` 或 `docker run` 拉取镜像时报错（如 `Get "https://registry-1.docker.io/v2/": EOF`），多为访问 Docker Hub 不稳定。可通过配置**镜像加速**解决。

---

## 步骤

### 1. 打开 Docker 引擎配置

- 打开 **Docker Desktop**
- 菜单栏点 **Settings**（或 **Preferences**）
- 左侧选 **Docker Engine**

### 2. 在 JSON 中加入 registry-mirrors

在编辑框里，把下面整段 **替换** 或 **合并** 进现有配置（若已有 `registry-mirrors`，只保留一个数组并合并镜像地址即可）：

```json
{
  "builder": {
    "gc": {
      "defaultKeepStorage": "20GB",
      "enabled": true
    }
  },
  "experimental": false,
  "registry-mirrors": [
    "https://docker.1ms.run",
    "https://docker.xuanyuan.me"
  ]
}
```

若你当前 JSON 里已有其他字段（如 `"features"`、`"log-level"` 等），**只加 `"registry-mirrors"` 这一段**即可，例如：

```json
{
  "registry-mirrors": [
    "https://docker.1ms.run",
    "https://docker.xuanyuan.me"
  ]
}
```

### 3. 应用并重启

- 点 **Apply & Restart**
- 等 Docker 重启完成后，再执行需要拉取镜像的命令，例如：

```bash
docker run -d -p 9000:9000 -p 9001:9001 --name minio minio/minio server /data --console-address ":9001"
```

---

## 其他可用镜像源（可选）

若上述镜像不稳定，可替换或追加为以下之一（任选其一或组合）：

| 镜像源     | 地址 |
|------------|------|
| 1ms.run    | `https://docker.1ms.run` |
|  Xuanyuan   | `https://docker.xuanyuan.me` |
| 阿里云     | 需登录 [容器镜像服务](https://cr.console.aliyun.com/cn-hangzhou/instances/mirrors) 获取个人加速地址 |
| 腾讯云     | `https://mirror.ccs.tencentyun.com` |
| 网易       | `https://hub-mirror.c.163.com` |

配置完成后，Docker 会优先从这些地址拉取镜像，从而避免直连 Docker Hub 超时或 EOF。
