# XDR-Test 平台 — 产品需求文档 (PRD)

## 系统概述

XDR-Test 是一个面向企业级终端安全的扩展检测与响应 (Extended Detection and Response) 平台。系统采用云管端 (Cloud-Management-Endpoint) 架构，通过部署在终端的轻量级 Agent 采集安全数据，经微服务集群实时分析处理，在 Web 控制台提供统一的威胁态势感知、资产管理、基线检测、合规审查和远程管控能力。

**目标用户**: 企业安全运维团队 (SOC)、网络安全管理员、合规审计人员

**技术架构**: Vue 3 + Spring Boot 3 微服务 (8 个) + Python Agent + MySQL + Redis

## 模块概览

| 模块 | 页面 | 核心功能 |
|:---|:---|:---|
| 安全概览 | Dashboard | 全局安全态势可视化、关键指标统计、威胁趋势分析 |
| 资产管理 | 资产列表、资产详情 | 终端资产全生命周期管理、实时状态监控、历史快照时间机器 |
| 威胁告警 | 告警列表 | 安全事件检测、告警分级处置、响应追踪 |
| 基线管理 | 基线列表 | 安全基线学习、偏移检测、基线审批 |
| 合规管理 | 合规视图 | 等保合规标准管理、合规检查结果分析 |
| 策略管理 | 策略视图 | 安全策略配置下发、终端指令管控 |
| 远程升级 | 升级视图 | Agent OTA 远程升级、升级包管理 |
| 用户管理 | 用户视图 | 系统用户账户 CRUD、角色权限管理 |

## 页面清单

| # | 页面名称 | 路由 | 模块 | 文档链接 |
|:---|:---|:---|:---|:---|
| 1 | 登录 | `/login` | 认证 | [→](./pages/01-login.md) |
| 2 | 安全概览 | `/dashboard` | Dashboard | [→](./pages/02-dashboard.md) |
| 3 | 资产列表 | `/assets` | 资产管理 | [→](./pages/03-asset-list.md) |
| 4 | 资产详情 | `/assets/:id` | 资产管理 | [→](./pages/04-asset-detail.md) |
| 5 | 威胁告警 | `/alerts` | 威胁管理 | [→](./pages/05-alert-list.md) |
| 6 | 基线管理 | `/baselines` | 基线管理 | [→](./pages/06-baseline-list.md) |
| 7 | 合规管理 | `/compliance` | 合规管理 | [→](./pages/07-compliance.md) |
| 8 | 策略管理 | `/policy` | 策略管理 | [→](./pages/08-policy.md) |
| 9 | 远程升级 | `/upgrade` | 升级管理 | [→](./pages/09-upgrade.md) |
| 10 | 用户管理 | `/users` | 用户管理 | [→](./pages/10-users.md) |

## 全局约定

### 权限模型
- 基于 JWT Token 的 Bearer 认证
- 路由守卫：未登录自动跳转 `/login`
- Token 过期 (401) 自动清空 localStorage 并跳转登录页
- Gateway 白名单：Agent 注册、心跳、事件上报等接口免鉴权

### 通用交互模式
- **API 基础路径**: `/api/v1`，超时 10s
- **分页**: 所有列表采用 `page + size` 分页，默认 15 条/页
- **错误处理**: 统一 `code !== 200` 则 ElMessage.error
- **设计语言**: Fintech Elite（Liquid Glass / Bento Card 质感，深蓝/大白极简风格）
- **动画**: 入场交错动画 `entrance-stagger`，hover 微交互

### 枚举定义
详见 [枚举字典](./appendix/enum-dictionary.md)
