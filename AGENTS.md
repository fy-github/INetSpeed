# INetSpeed · 项目知识

## 项目概述

INetSpeed 是一款 Android 网络带宽测试工具，基于 iperf3 协议。

## 技术栈

- Kotlin + Jetpack Compose
- Hilt (依赖注入)
- Room (本地数据库)
- Navigation Compose
- ProcessBuilder (iperf3 执行)

## 模块结构

- `:app` — 应用入口、主题装配、导航宿主
- `:core:designsystem` — 颜色、字体、图标、仪表盘组件
- `:core:data` — Room、Repository、错误码、迁移
- `:core:iperf3` — iperf3 二进制管理、命令构建、执行、取消、解析
- `:core:network-discovery` — 内置服务器、Ping 推荐、mDNS 发现
- `:core:privacy` — 数据脱敏、导出授权、隐私配置
- `:core:sync` — 登录态、增量同步、冲突处理、重试队列
- `:core:ads` — AdManager、NoOp、广告容器状态
- `:feature:speedtest` — 首页、简单/专家模式、实时测速状态机
- `:feature:servers` — 服务器选择、编辑、收藏、推荐排序
- `:feature:tools` — Ping、Traceroute、网络信息
- `:feature:history` — 趋势图、筛选、详情
- `:feature:report` — 报告模板、截图、PDF/PNG/Excel/CSV 导出
- `:feature:settings` — 主题、同步、iperf3、关于、许可展示

## 设计规格

- 设计文档：`docs/superpowers/specs/2026-06-03-INetSpeed-design.md`
- 开发计划：`docs/superpowers/specs/2026-06-03-INetSpeed-plan.md`
- 设计原型：`.superpowers/brainstorm/457-1780467432/`

## 编码规范

- 语言：Kotlin
- UI：Jetpack Compose
- 架构：Clean Architecture (UI → Domain → Data)
- 依赖注入：Hilt
- 异步：Kotlin Coroutines + Flow
- 所有文本文件使用 UTF-8 编码

## 构建命令

```bash
# Debug 构建
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug

# 运行测试
./gradlew test
```
