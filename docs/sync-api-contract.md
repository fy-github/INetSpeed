# Sync API Contract

> M8 必备规格附件
> 创建日期：2026-06-03

---

## 1. 认证

| 项目 | 说明 |
|------|------|
| 方式 | Bearer Token (JWT) |
| 获取 | POST /auth/login |
| 刷新 | POST /auth/refresh |
| 存储 | EncryptedSharedPreferences |

### POST /auth/login

```
Request:
{
    "email": "user@example.com",
    "password": "***"
}

Response:
{
    "token": "jwt_token",
    "refreshToken": "refresh_token",
    "expiresIn": 3600
}
```

---

## 2. 同步接口

### POST /sync/push

上传本地变更数据。

```
Request:
{
    "lastSyncAt": 1717000000000,
    "measurements": [
        {
            "localId": 1,
            "timestamp": 1717000000000,
            "serverName": "香港节点",
            "protocol": "tcp",
            "throughputMbps": 246.5,
            ...
        }
    ],
    "servers": [
        {
            "localId": 10,
            "name": "自定义服务器",
            "address": "192.168.1.100",
            "port": 5201,
            "isFavorite": true
        }
    ],
    "deletedIds": {
        "measurements": [5, 8],
        "servers": [12]
    }
}

Response:
{
    "syncedAt": 1717000060000,
    "conflicts": [
        {
            "type": "measurement",
            "localId": 1,
            "serverVersion": {...},
            "resolution": "server_wins"
        }
    ],
    "errors": []
}
```

### POST /sync/pull

拉取服务端数据。

```
Request:
{
    "since": 1717000000000,
    "types": ["measurements", "servers"]
}

Response:
{
    "syncedAt": 1717000060000,
    "measurements": [...],
    "servers": [...],
    "deletedIds": {
        "measurements": [3],
        "servers": []
    }
}
```

### POST /sync/report

手动上传报告文件。

```
Request (multipart):
- reportId: long
- file: binary

Response:
{
    "fileUrl": "https://.../report.pdf"
}
```

---

## 3. 删除墓碑

服务端维护已删除记录的墓碑表，防止已删除数据在同步时回填。

| 字段 | 说明 |
|------|------|
| entityId | 被删除的记录 ID |
| entityType | measurement / server |
| deletedAt | 删除时间戳 |

墓碑保留策略：30 天后自动清理。

---

## 4. 冲突策略

| 场景 | 策略 |
|------|------|
| 同一条记录本地和服务端都修改 | 服务端优先（last-write-wins） |
| 本地删除 vs 服务端修改 | 本地删除优先 |
| 本地新增 vs 服务端已存在 | 保留服务端版本 |

---

## 5. 同步流程

```
用户开启同步 → 登录 → 首次全量拉取 → 增量同步
                                         ↑
                                    每次 App 启动
                                    手动触发
                                    数据变更后延迟 5 秒
```

---

## 6. 错误处理

| 错误码 | 说明 | 处理 |
|--------|------|------|
| 401 | Token 过期 | 刷新 Token 后重试 |
| 409 | 冲突 | 按冲突策略处理 |
| 429 | 频率限制 | 指数退避重试 |
| 500 | 服务端错误 | 加入重试队列 |
| 网络错误 | 断网 | 恢复后自动重试 |
