# INetSpeed

基于 iperf3 协议的 Android 网络带宽测试工具，提供专业级网络性能测试能力。

## 功能特性

### 测速核心
- **TCP/UDP 双协议** — 支持吞吐量、时延、抖动、丢包率测试
- **双向测试** — 正向（上传）/ 反向（下载）切换
- **IPv4/IPv6** — 双栈支持
- **实时遥测** — 测速过程中实时展示带宽曲线、进度、指标
- **CLI 会话模式** — 专家模式下可直接输入 iperf3 命令

### 服务器管理
- 内置公共 iperf3 服务器列表
- 自定义服务器添加、编辑、收藏
- Ping / TCP Ping 可达性预检
- mDNS 局域网服务器自动发现

### 网络工具
- **Ping** — ICMP 连通性测试
- **Traceroute** — 路由追踪
- **网络信息** — 当前网络状态概览

### 历史与报告
- 测速历史记录与趋势图
- 多格式导出：PDF / PNG / Excel / CSV
- 数据筛选（时间、服务器、协议）

### 其他
- 亮色 / 暗色主题切换
- 数据同步（登录态 + 增量同步）
- 隐私数据脱敏
- 广告模块（可配置 NoOp）

## 技术栈

| 层级 | 技术 |
|------|------|
| UI | Jetpack Compose + Material3 |
| 架构 | Clean Architecture (UI → Domain → Data) |
| 状态管理 | StateFlow + Sealed Class |
| 依赖注入 | Hilt |
| 本地存储 | Room |
| 异步 | Kotlin Coroutines + Flow |
| 测速引擎 | iperf3 (ProcessBuilder) |
| 最低版本 | Android 10 (API 29) |

## 模块结构

```
INetSpeed/
├── app/                        # 应用入口、导航宿主
├── core/
│   ├── designsystem/           # 颜色、字体、组件库
│   ├── data/                   # Room、Repository、错误码
│   ├── iperf3/                 # 二进制管理、命令构建、解析
│   ├── network-discovery/      # Ping、Traceroute、mDNS
│   ├── privacy/                # 数据脱敏
│   ├── sync/                   # 登录态、增量同步
│   ├── ads/                    # 广告管理
│   └── service/                # 前台服务
└── feature/
    ├── speedtest/              # 首页测速（简单/专家模式）
    ├── servers/                # 服务器选择与管理
    ├── tools/                  # Ping、Traceroute、网络信息
    ├── history/                # 历史记录与趋势图
    ├── report/                 # 报告生成与导出
    └── settings/               # 主题、同步、关于
```

## 构建与运行

### 环境要求
- Android Studio Ladybug 或更高版本
- JDK 17
- Android SDK 35

### 命令

```bash
# Debug 构建
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug

# 运行单元测试
./gradlew test
```

## 截速界面

| 简单模式 | 专家模式 |
|---------|---------|
| 一键测速，实时带宽曲线 | 自定义参数 / CLI 命令会话 |
| TCP/UDP 协议切换 | 并发数、时长、窗口大小等高级配置 |

## License

MIT
