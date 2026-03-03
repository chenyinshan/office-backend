# Redis 使用与验证

oa-user-service 使用 Redis 存储**登录 Token** 和**用户信息 L2 缓存**。以下方法可确认 Redis 已接入且生效。

---

## 一、启动前：确认 Redis 已运行

```bash
redis-cli ping
```

应返回 **PONG**。若报 `Could not connect`，说明 Redis 未启动，需先 `brew services start redis` 或启动本机 Redis 服务。

---

## 二、登录后：在 Redis 里看到 OA 的 key

1. 启动 **Redis**、**MySQL**、**oa-user-service**（以及 portal、gateway、前端）。
2. 在浏览器用 **admin / 123456** 登录一次。
3. 新开终端，执行：

```bash
redis-cli KEYS "oa:*"
```

应看到类似：

- `oa:auth:token:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`（登录 Token，value 为 userId）
- `oa:user:info:1`（用户信息缓存，value 为 JSON）

再查 Token 对应的 value（把下面命令里的 key 换成你看到的 token key）：

```bash
redis-cli GET "oa:auth:token:你的token"
```

应返回 `1`（admin 的 userId）。

查用户信息缓存（admin 的 userId=1）：

```bash
redis-cli GET "oa:user:info:1"
```

应返回一长串 JSON（包含 username、name、roles 等）。

---

## 三、验证 Token 持久在 Redis（重启服务仍有效）

1. 登录后，在 Redis 中确认存在 `oa:auth:token:xxx`（见上一节）。
2. **停掉 oa-user-service**（Ctrl+C 或 kill 进程）。
3. **再次启动 oa-user-service**，**不要再次登录**。
4. 在浏览器里**刷新页面或点击需要登录的菜单**。

若仍能正常访问（不要求重新登录），说明 Token 是从 Redis 读的，**已生效**。  
若未接入 Redis，Token 只存在内存里，重启服务后 token 会丢失，需要重新登录。

---

## 四、验证二级缓存（L2 命中）

1. 登录后，在 Redis 中执行：

```bash
redis-cli GET "oa:user:info:1"
```

记下返回的 JSON（或直接再执行一次 GET，确认存在）。

2. 在**不重启** oa-user-service 的前提下，多次访问「当前用户」接口（如前端个人中心或调用 `GET /api/auth/me`）。  
   - 第一次可能查 DB 并写入 L1/L2；  
   - 之后请求会命中 **L1（Caffeine）** 或 **L2（Redis）**，不会每次打 DB。  
3. 再在 Redis 里执行：

```bash
redis-cli TTL "oa:user:info:1"
```

应返回剩余秒数（例如几百），说明 key 带 TTL，是我们在 L2 里缓存的用户信息。

---

## 五、不启 Redis 时行为对比

若**不启动 Redis** 就启动 oa-user-service，应用会报连接 Redis 失败（如 `Connection refused` 或 Lettuce 报错），**服务起不来**。  
这说明 oa-user-service 已**强依赖** Redis，接入是生效的。

---

## 六、小结

| 验证项           | 做法                         | 预期结果                         |
|------------------|------------------------------|----------------------------------|
| Redis 已运行     | `redis-cli ping`             | 返回 PONG                        |
| Token 存 Redis  | 登录后 `redis-cli KEYS "oa:*"` | 能看到 `oa:auth:token:xxx`       |
| 用户信息 L2 缓存 | 登录后 `redis-cli GET "oa:user:info:1"` | 返回 JSON；`TTL` 有剩余秒数 |
| Token 跨重启有效 | 登录 → 重启 user-service → 再访问 | 仍登录状态，不需重新登录       |
| 未启 Redis       | 不启 Redis 只启 user-service | 启动报错，连接被拒绝             |
