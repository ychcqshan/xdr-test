# XDR Phase 2 详细设计文档 (As-built)

## 1. 终端 Agent (xdr-agent) 实现详情

### 1.1 指令执行监控 (Windows)
- **技术路线**：使用 `pywin32` 订阅 Security Event ID **4688**。
- **命令行提取**：
  - **实现逻辑**：启动监听线程，解析事件 XML 中的 `ProcessCommandLine` 数据节点。
  - **环境要求**：需在系统端开启“在进程创建事件中包含命令行”审计策略。

### 1.2 勒索软件检测 (Honeyfiles)
- **技术路线**：在 `%USERPROFILE%\Documents\` 下投放名为 `.sys_cache_info` 的隐藏诱饵文件。
- **触发机制**：通过 `watchdog` 库监听该文件的 `MODIFIED` 和 `DELETED` 事件。
- **告警响应**：一旦触发，Agent 立即生成 `CRITICAL` 级别的告警并同步上报。

### 1.3 流量采集与拓扑刻画
- **技术路线**：使用 **Scapy** 库开启网卡混杂模式 (`sniff(promisc=True)`)。
- **核心逻辑**：实时捕获并统计 Source/Destination IP 对及其端口信息，定期上报用于后端资产拓扑图谱的构建。

### 1.4 指令执行监控 (Linux)
- **实现方案**：驻留线程实时 Tail 读取 `/var/log/audit/audit.log`。
- **数据提取**：精准匹配 `type=EXECVE` 记录，拼接 `a0/a1/a2` 等原始执行参数。

## 2. 后端微服务架构与 API (As-built)

### 2.1 新增微服务清单
- **policy-service (Port: 8085)**: 负责安全策略的 CRUD 及生效策略的分层计算（Global -> Group -> Agent）。
- **upgrade-service (Port: 8086)**: 负责 Agent 升级包管理及任务实时追踪。
- **compliance-service (Port: 8087)**: 负责等保 2.0 等合规标准管理及扫描结果存储。

### 2.2 数据库兼容性与稳定性 (As-built)
- **保留字转义**：针对 MySQL 保留字 `alert` 和 `event`，在 Mapper SQL 及 `@TableName` 注解中统一使用反引号 (`` ` ``) 进行转义。
- **持久层重构**：`AlertService` 全面转向 `QueryWrapper`，解决 `LambdaQueryWrapper` 在处理 `BaseEntity` 继承字段时的反射兼容性问题。
- **构建标准化**：在父级 `pom.xml` 的 `pluginManagement` 中强制锁定 `spring-boot-maven-plugin` 版本及 `repackage` 目标，确保生成包含完整清单的独立可执行 JAR。

### 2.3 核心 API 对接 (via api-gateway)
- **资产详情**: `GET /api/v1/assets/{agentId}/details` (聚合进程、端口、软件、USB、登录日志)。
- **流量拓扑**: `GET /api/v1/assets/topology` (返回图谱节点与连接数据)。
- **策略管理**: 
    - `POST /api/v1/policies`: 保存/更新策略。
    - `GET /api/v1/policies/effective/{agentId}`: 获取单机最终生效策略。
- **升级任务**:
    - `GET /api/v1/upgrades/tasks/pending/{agentId}`: Agent 获取待处理任务。
    - `POST /api/v1/upgrades/tasks/status`: Agent 回传进度。

## 3. 基线与检测逻辑

### 3.1 漂移分析实装
- **存储结构**: `baseline` 表存储标准快照，`baseline_item` 存储明细。
- **对比引擎**: 后端 `BaselineService` 实现集合差集运算，识别 `MISSING` 和 `DEVIATED` 项。
- **视觉比对 (Visual Diff)**: 前端 `BaselineListView.vue` 实现实时状态与标准的左右对比。

### 3.2 勒索检测 (Honeyfiles) & 指令监控
- **诱饵文件**: 精准监听指定路径下的 `.sys_cache_info` 变动。
- **指令捕获**: 
    - **Windows**: 通过订阅 Event ID 4688 获取进程命令行。
    - **Linux**: Tail 解析 `/var/log/audit/audit.log` 中的 `EXECVE` 记录。

## 4. 前端 UI/UX 设计系统 (Linear Aesthetic)

### 4.1 核心视觉定位
- **风格参考**：深度对标 Linear.app，采用深色海军蓝 (#0a0a0b) 作为主背景。
- **布局方案**：**Bento Grid (便当盒布局)**。将各类安全指标、威胁趋势及资产状态封装为独立的卡片模块，并根据业务权重设置不同的 Grid Span。
- **组件规范**：
    - **玻璃拟态 (Glassmorphism)**：卡片使用 `backdrop-blur` 和 `border-slate-800` 实现半透明分层感。
    - **动效反馈**：图表加载采用平滑的过渡动画，关键指标（如高危告警）具备微妙的 CSS 阴影或光晕。
    - **导航优化**：侧边栏采用极简风格，集成 `CMD + K` 命令搜索快速入口。
