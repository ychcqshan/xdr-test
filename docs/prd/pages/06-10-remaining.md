# 基线管理

> **路由:** `/baselines`
> **模块:** 基线管理
> **组件:** `BaselineListView.vue`

## 概述
基线管理用于建立终端安全行为基准，通过机器学习算法学习正常行为模式，并检测偏离基线的异常行为。

## 核心功能
- 基线列表展示（分页查询）
- 发起基线学习（指定 agentId、类型、学习时长）
- 基线审批（人工确认学习结果）
- 查看基线详细项

## API 依赖

| API | 方法 | 路径 | 参数 |
|:---|:---|:---|:---|
| 基线列表 | GET | `/api/v1/baselines` | page, size 分页 |
| 发起学习 | POST | `/api/v1/baselines/learning` | agentId, type, durationHours |
| 审批基线 | POST | `/api/v1/baselines/approve` | agentId, type |
| 基线项详情 | GET | `/api/v1/baselines/:id/items` | — |

---

# 合规管理

> **路由:** `/compliance`
> **模块:** 合规管理
> **组件:** `ComplianceView.vue`

## 概述
合规管理模块对照等保/行业合规标准，对终端进行合规性检查，展示检查结果和合规健康度。

## API 依赖

| API | 方法 | 路径 | 说明 |
|:---|:---|:---|:---|
| 合规标准列表 | GET | `/api/v1/compliance/standards` | 所有合规标准 |
| 合规检查结果 | GET | `/api/v1/compliance/results/:agentId` | 单终端合规检查结果 |

---

# 策略管理

> **路由:** `/policy`
> **模块:** 策略管理
> **组件:** `PolicyView.vue`

## 概述
策略管理用于配置和下发安全策略到终端 Agent。支持策略 CRUD、按组/终端生效查询、远程指令下发。

## API 依赖

| API | 方法 | 路径 | 说明 |
|:---|:---|:---|:---|
| 策略列表 | GET | `/api/v1/policies` | 所有安全策略 |
| 保存策略 | POST | `/api/v1/policies` | 创建/更新策略 |
| 生效策略 | GET | `/api/v1/policies/effective/:agentId` | 查询终端生效策略，可选 groupId |
| 远程指令 | POST | `/api/v1/policies/commands` | 向终端下发即时指令 |

---

# 远程升级

> **路由:** `/upgrade`
> **模块:** 升级管理
> **组件:** `UpgradeView.vue`

## 概述
OTA 远程升级模块管理 Agent 升级包的发布和分发。支持升级包上传、任务创建和终端拉取。

## API 依赖

| API | 方法 | 路径 | 说明 |
|:---|:---|:---|:---|
| 升级包列表 | GET | `/api/v1/upgrades/packages` | 所有升级包 |
| 上传升级包 | POST | `/api/v1/upgrades/packages` | 创建新升级包 |
| 待处理任务 | GET | `/api/v1/upgrades/tasks/pending/:agentId` | 终端拉取待升级任务 |

---

# 用户管理

> **路由:** `/users`
> **模块:** 用户管理
> **组件:** `UserView.vue`

## 概述
系统用户管理页面，管理员在此进行用户账户的 CRUD 操作和角色权限分配。

## 说明
用户管理后端由 `auth-service` (端口 8081) 提供。[TBC: 前端页面 API 调用尚未完整对接，需查看 UserView.vue 确认]
