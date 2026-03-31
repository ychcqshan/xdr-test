# XDR Project Roadmap

## Phase 1: MVP 核心构建 (Completed)
**目标**：端-云-管三端 MVP 闭环。

- **后端**: 5 微服务 (`api-gateway`, `auth`, `asset`, `baseline`, `threat`) + `common` 模块。
- **Agent**: Python 探针 (进程/网络/登录采集、心跳上报、断网缓存)。
- **前端**: Vue 3 + Vite + Element Plus (登录、仪表盘、资产列表、告警列表)。
- **基础设施**: Docker (MySQL + Redis)，7 个独立数据库。

---

## Phase 2: 高级检测与综合管理 (Completed)
**目标**：深化采集维度，引入勒索与指令检测，并实装核心管理矩阵。

- **终端 Agent**:
  - 指令执行监控 (Event 4688 / Auditd)。
  - 勒索软件诱饵文件防护。
  - 流量拓扑采集。
- **管理枢纽**:
  - **合规管理服务 (Compliance)**：内置等保 2.0 模板。
  - **远程升级 (Upgrade)**：支持 Agent 的版本追踪与任务监控。
  - **策略管理 (Policy)**：分层策略下发体系。
- **可视化与体验**:
  - **Linear.app 风格系统**: 引入深色调、1px 细边框及玻璃拟态 (Glassmorphism) 的 Bento Grid 仪表盘。
  - **资产聚合看板**: 自动聚合单机全维度详情，支持 ECharts 深度定制化图表渲染。
- **稳定性加固**:
  - **数据库兼容性**: 针对 `alert`/`event` 等 MySQL 保留字完成转义适配。
  - **构建标准化**: 强制锁定 Maven Repackage 流程，解决可执行 JAR 的启动清单问题。

---

## Phase 5: 终端入侵痕迹排查分析 (Completed)
**目标**：实现针对 Windows 终端的高级取证与痕迹分析闭环。

- **深度取证 (Forensic)**：
  - `ForensicCollector` 覆盖计划任务、注册表、WMI 及敏感目录。
  - 自动化反弹 Shell 检测逻辑。
  - PowerShell Base64 指令自动解码。
- **分析中心**:
  - `asset-service` 时序报告存储。
  - `threat-service` 异常项自动触发告警。
- **可视化**:
  - 资产详情页"入侵排查"标签页，采用 Bento Grid 语义化展示报告。

---

## Phase 4: 资产时序管理与服务重构 (Completed)
**目标**：构建资产生命周期时序追踪能力，实现微服务职责清晰化。

- **数据模型**:
  - `host_asset_record` 时序快照表设计与建表（指纹幂等 + ACTIVE/INACTIVE 状态）。
- **核心引擎**:
  - 基于指纹的 Upsert 算法 + Inactive 自动流转（30天清理）。
  - 数据解析逻辑从 `threat-service` 迁移至 `asset-service`（`/internal/sync_raw` 原生透传）。
- **时序查询**:
  - 后端 `/timeline` API（按 agentId + 时间戳查询历史快照切片）。
  - 前端"时光机"控件（时间拾取器 + 历史快照回溯展示）。
- **跨服务治理**:
  - RestTemplate 注入修复 + Gateway JWT 白名单扩展。
  - Agent 重注册与用户信息 GUI 采集逻辑。

---

## Phase 3: 基础设施升级与大规模适配 (Planned)
**目标**：提升海量数据处理能力，并完成国产化深度适配。

- **基础设施**:
  - **接入 Kafka**：引入消息队列缓冲高负载事件流。
  - **接入 Elasticsearch**：实现海量日志的秒级搜索与持久化存储。
- **国产化适配**:
  - 完成麒麟、统信在非 x86 架构上的驱动级加固。
- **自保护增强**:
  - 引入内核级防卸载与关键进程加固。

---

## Phase 6: 生态集成与深度溯源 (Planned)
**目标**：构建开放的安全生态，实现威胁的根因分析。

- **生态集成**:
  - **标准 API/SDK**：开放接口供 第三方 SIEM (如 Splunk) 或 SOAR 平台调用。
  - **联动响应**：支持与防火墙、EDR 等第三方设备联动阻断。
- **深度分析**:
  - **威胁溯源 (Root Cause)**：基于大数据的关联分析，自动定位初始攻击入口（如钓鱼邮件、漏洞利用）。
  - **UEBA**：引入用户实体行为分析，识别账号被盗用或内部威胁。
- **智能运维**:
  - **智能基线**：利用机器学习自动调整动态基线，减少误报。

