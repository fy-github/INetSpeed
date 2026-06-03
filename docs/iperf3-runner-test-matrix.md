# iperf3 Runner Test Matrix

> M0.5 必备规格附件
> 创建日期：2026-06-03

---

## 1. 测试目标

在正式数据层和完整 UI 之前，提前证明 iperf3 在 Android 真机上的：
- 可执行性（二进制能运行）
- 可取消性（进程能终止，不残留）
- 可解析性（输出能解析为结构化数据）

---

## 2. 测试环境要求

| 项目 | 要求 |
|------|------|
| 设备 | 至少 1 台 arm64-v8a Android 真机 |
| Android 版 | ≥ 10 (API 29) |
| ABI | arm64-v8a（主测），armeabi-v7a（可选） |
| iperf3 版本 | ≥ 3.14（来自 android-iperf3 仓库） |
| 服务端 | 公共 iperf3 服务端或自建服务端 |

### 公共 iperf3 服务端（测试用）

| 服务端 | 地址 | 端口 | 备注 |
|--------|------|------|------|
| iperf.he.net | iperf.he.net | 5201 | Hurricane Electric |
| bouygues.iperf.fr | bouygues.iperf.fr | 5201 | Bouygues Telecom |
| ping.online.net | ping.online.net | 5201 | Scaleway |

> 注意：公共服务器可能有并发限制或时段限制。建议同时准备自建服务端。

---

## 3. 测试矩阵

### 3.1 可执行性测试

| # | 测试项 | 命令 | 预期结果 | 通过条件 |
|---|--------|------|----------|----------|
| E1 | 版本查询 | `iperf3 --version` | 输出版本号 | 返回 exit code 0，stdout 包含 "iperf" |
| E2 | 帮助输出 | `iperf3 --help` | 输出参数帮助 | 返回 exit code 0 |
| E3 | TCP 正向测速 | `iperf3 -c <host> -t 3` | 输出 interval + 总结 | 有 interval 行和 SUM 行 |
| E4 | TCP 反向测速 | `iperf3 -c <host> -t 3 -R` | 输出反向结果 | 有 reverse 相关输出 |

### 3.2 协议与方向测试

| # | 测试项 | 命令 | 预期结果 | 通过条件 |
|---|--------|------|----------|----------|
| P1 | TCP 正向 | `iperf3 -c <host> -t 5 -i 1` | 每秒 interval 输出 | 5 个 interval 行 + SUM 行 |
| P2 | TCP 反向 | `iperf3 -c <host> -t 5 -i 1 -R` | 反向 interval 输出 | 5 个 interval 行 + SUM 行（reverse） |
| P3 | UDP 正向 | `iperf3 -c <host> -t 5 -i 1 --udp -b 10M` | UDP interval + jitter/loss | 有 jitter 和 lost/total 字段 |
| P4 | UDP 反向 | `iperf3 -c <host> -t 5 -i 1 --udp -b 10M -R` | UDP 反向结果 | 有 reverse + jitter/loss |
| P5 | IPv4 显式 | `iperf3 -c <host> -t 3 -4` | IPv4 连接 | 正常输出 |
| P6 | IPv6 | `iperf3 -c <host> -t 3 -6` | IPv6 连接或明确错误 | 成功或 errno 明确 |
| P7 | SCTP | `iperf3 -c <host> -t 3 --sctp` | SCTP 连接或不支持错误 | 成功或明确 "protocol not supported" |

### 3.3 参数测试

| # | 测试项 | 命令 | 预期结果 | 通过条件 |
|---|--------|------|----------|----------|
| A1 | 并发线程 | `iperf3 -c <host> -t 3 -P 4` | 4 线程输出 + 总计 | 有 [SUM] 行 |
| A2 | 自定义端口 | `iperf3 -c <host> -p 5202 -t 3` | 指定端口连接 | 连接成功或明确端口错误 |
| A3 | 窗口大小 | `iperf3 -c <host> -t 3 -w 256K` | 指定窗口大小 | 正常输出 |
| A4 | 缓冲区长度 | `iperf3 -c <host> -t 3 -l 64K` | 指定缓冲区 | 正常输出 |
| A5 | JSON 输出 | `iperf3 -c <host> -t 3 -J` | JSON 格式输出 | 可解析的 JSON |
| A6 | 错误端口 | `iperf3 -c <host> -p 99999 -t 3` | 端口越界错误 | exit code ≠ 0，stderr 有错误 |

### 3.4 取消与超时测试

| # | 测试项 | 操作 | 预期结果 | 通过条件 |
|---|--------|------|----------|----------|
| C1 | 运行中取消 | 启动 `iperf3 -c <host> -t 60`，3 秒后发送 SIGTERM | 进程终止 | process.exitValue() 在 5 秒内返回 |
| C2 | 超时处理 | 启动 `iperf3 -c <host> -t 60`，设置 10 秒超时 | 超时强杀 | 超时后进程被 kill |
| C3 | 无效主机 | `iperf3 -c 192.0.2.1 -t 3` | 连接超时 | 有明确错误输出，不挂起 |
| C4 | 重复取消 | 连续调用 cancel 两次 | 不崩溃 | 第二次 cancel 为 no-op |

### 3.5 输出解析测试

| # | 测试项 | 输入 | 预期解析结果 |
|---|--------|------|-------------|
| O1 | TCP interval 行 | `[  5] 0.0-1.0 sec  28.5 MBytes  239 Mbits/sec` | secondIndex=0, bitsPerSecond=239000000 |
| O2 | TCP SUM 行 | `[SUM] 0.0-10.0 sec  285 MBytes  239 Mbits/sec` | 最终 throughputMbps |
| O3 | UDP interval 行 | `[  5] 0.0-1.0 sec  1.25 MBytes  10.5 Mbits/sec  0.234 ms  0/1000 (0%)` | jitterMs=0.234, packetLossPercent=0 |
| O4 | JSON 输出 | `iperf3 -J` 完整 JSON | 解析为 TestMeasurement |
| O5 | 错误输出 | `iperf3 -c invalid -t 3` stderr | 解析为 IperfError |

---

## 4. SpeedInterval 解析规则

### TCP interval 格式

```
[ ID] Interval           Transfer     Bitrate
[  5] 0.00-1.00  sec  28.5 MBytes  239 Mbits/sec
```

正则：`\[\s*\d+\]\s+(\d+\.\d+)-(\d+\.\d+)\s+sec\s+([\d.]+)\s+(\w+)\s+([\d.]+)\s+(\w+/sec)`

### UDP interval 格式

```
[ ID] Interval           Transfer     Bitrate         Jitter    Lost/Total Datagrams
[  5] 0.00-1.00  sec  1.25 MBytes  10.5 Mbits/sec  0.234 ms  0/1000 (0%)
```

正则：`\[\s*\d+\]\s+(\d+\.\d+)-(\d+\.\d+)\s+sec\s+([\d.]+)\s+(\w+)\s+([\d.]+)\s+(\w+/sec)\s+([\d.]+)\s+ms\s+(\d+)/(\d+)\s+\(([\d.]+)%\)`

### JSON 输出结构

```json
{
  "intervals": [
    {
      "streams": [{ "bits_per_second": 239000000 }],
      "sum": { "seconds": 1.0, "bytes": 35651584, "bits_per_second": 239000000 }
    }
  ],
  "end": {
    "sum_sent": { "bytes": 285000000, "bits_per_second": 239000000 },
    "sum_received": { "bytes": 285000000, "bits_per_second": 239000000 }
  }
}
```

---

## 5. raw output 保存策略

| 项目 | 策略 |
|------|------|
| 存储位置 | 内部存储 `files/iperf3-outputs/` |
| 文件命名 | `{testId}_{timestamp}.txt` |
| 内容 | stdout + stderr 合并输出 |
| 大小限制 | 单文件 ≤ 1MB，超限截断旧内容 |
| 清理策略 | 删除测试记录时同步删除 raw output 文件 |
| 隐私 | 默认不上传，导出时按隐私规则脱敏 |

---

## 6. 验收证据模板

每次测试需记录：

```
## 测试记录

- 设备型号：
- Android 版本：
- ABI：
- iperf3 版本：
- 测试时间：
- 服务端：

### E1 版本查询
- 命令：
- stdout：
- exit code：

### P1 TCP 正向
- 命令：
- raw output（前 20 行）：
- 解析后的 SpeedInterval：
- 最终结果：

### C1 取消测试
- 命令：
- 启动时间：
- 取消时间：
- 进程退出时间：
- exit code：
- 残留进程检查：
```

---

## 7. 风险与降级

| 风险 | 触发条件 | 降级方案 |
|------|----------|----------|
| arm64 二进制无法执行 | SELinux/权限限制 | 尝试 chmod 755 + 放到 app_process 可访问路径 |
| 公共服务端不可用 | 网络限制/服务端下线 | 使用自建 iperf3 服务端 |
| SCTP 不支持 | 内核/二进制不支持 | 记录错误，UI 提示"当前协议不可用" |
| JSON 输出格式差异 | 版本差异 | 回退到文本 interval 解析 |
| IPv6 不可用 | 网络不支持 | 记录错误，UI 提示"IPv6 不可用" |
