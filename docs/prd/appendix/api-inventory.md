# API 完整清单

> 基础路径: `/api/v1` | 认证: Bearer Token | 超时: 10s

## 认证服务 (auth-service :8081)

| # | API | 方法 | 路径 | 参数 | 说明 |
|:---|:---|:---|:---|:---|:---|
| 1 | 登录 | POST | `/auth/login` | username, password | 返回 JWT Token |
| 2 | 登出 | POST | `/auth/logout` | — | 清除会话 |
| 3 | 刷新 Token | POST | `/auth/refresh` | refreshToken | 刷新 JWT |

## 资产服务 (asset-service :8082)

| # | API | 方法 | 路径 | 参数 | 说明 |
|:---|:---|:---|:---|:---|:---|
| 4 | 资产列表 | GET | `/assets` | page, size, keyword, osType, status, unitLevel1, responsiblePerson | 分页列表 |
| 5 | 资产详情 | GET | `/assets/:id` | — | 单条资产 |
| 6 | 资产采集详情 | GET | `/assets/:agentId/details` | startTime, endTime | 终端采集数据 |
| 7 | 网络拓扑 | GET | `/assets/topology` | — | 全局拓扑图 |
| 8 | 资产统计 | GET | `/assets/stats` | — | total, online |
| 9 | 历史时间线 | GET | `/assets/:agentId/timeline` | timestamp | 快照查询 |
| 10 | 入侵报告 | GET | `/assets/:agentId/intrusion-reports` | — | 入侵痕迹分析报告 |
| 11 | 心跳上报 | POST | `/heartbeat` | (Agent 调用) | Agent 免鉴权 |
| 12 | 原始数据同步 | POST | `/assets/internal/sync_raw` | (内部转发) | 内部 API |

## 基线服务 (baseline-service :8083)

| # | API | 方法 | 路径 | 参数 | 说明 |
|:---|:---|:---|:---|:---|:---|
| 13 | 基线列表 | GET | `/baselines` | page, size | 分页列表 |
| 14 | 发起学习 | POST | `/baselines/learning` | agentId, type, durationHours | 基线学习 |
| 15 | 审批基线 | POST | `/baselines/approve` | agentId, type | 人工确认 |
| 16 | 基线项详情 | GET | `/baselines/:id/items` | — | 基线明细 |

## 威胁服务 (threat-service :8084)

| # | API | 方法 | 路径 | 参数 | 说明 |
|:---|:---|:---|:---|:---|:---|
| 17 | 告警列表 | GET | `/alerts` | page, size | 分页查询 |
| 18 | 告警详情 | GET | `/alerts/:id` | — | 详细信息 |
| 19 | 更新状态 | PUT | `/alerts/:id/status` | statusData | 变更处置状态 |
| 20 | 响应处置 | POST | `/alerts/:id/respond` | operator | 标记已响应 |
| 21 | 告警统计 | GET | `/alerts/stats` | — | new, total |
| 22 | 事件上报 | POST | `/events` | (Agent 调用) | Agent 免鉴权 |

## 策略服务 (policy-service :8085)

| # | API | 方法 | 路径 | 参数 | 说明 |
|:---|:---|:---|:---|:---|:---|
| 23 | 策略列表 | GET | `/policies` | — | 所有策略 |
| 24 | 保存策略 | POST | `/policies` | policyData | CRUD |
| 25 | 生效策略 | GET | `/policies/effective/:agentId` | groupId? | 查询生效配置 |
| 26 | 远程指令 | POST | `/policies/commands` | commandData | 下发即时指令 |

## 升级服务 (upgrade-service :8086)

| # | API | 方法 | 路径 | 参数 | 说明 |
|:---|:---|:---|:---|:---|:---|
| 27 | 升级包列表 | GET | `/upgrades/packages` | — | 所有升级包 |
| 28 | 上传升级包 | POST | `/upgrades/packages` | packageData | 创建升级包 |
| 29 | 待处理任务 | GET | `/upgrades/tasks/pending/:agentId` | — | 终端拉取 |

## 合规服务 (compliance-service :8087)

| # | API | 方法 | 路径 | 参数 | 说明 |
|:---|:---|:---|:---|:---|:---|
| 30 | 合规标准 | GET | `/compliance/standards` | — | 所有标准 |
| 31 | 合规结果 | GET | `/compliance/results/:agentId` | — | 检查结果 |

---

**合计: 31 个 API 接口，跨 7 个微服务**

### Gateway 免鉴权白名单
- `/api/v1/auth/agent/register`
- `/api/v1/heartbeat`
- `/api/v1/events/**`
- `/api/v1/assets/**`
- `/api/v1/policies/**`
