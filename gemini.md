# XDR-Test 平台 AI 上下文指南 (gemini.md)

本指南旨在帮助 AI Agent 快速了解 `xdr-test` 项目的架构、依赖及常用命令，避免每次开发前重复探测环境。你应当在启动新的任务前，以此文档作为项目基础事实。

---

## 🏗️ 1. 项目架构概览
本项目是一个标准的云管端架构 XDR (Extended Detection and Response) 平台。包含以下三大核心仓库/目录：

- **`xdr-frontend/` (前端控制台)**
  - **技术栈**: Vue 3 (Composition API) + Vite + Tailwind CSS + Element Plus + ECharts。
  - **设计语言**: Fintech Elite (Liquid Glass / Bento Card 质感，深蓝/大白为主，极简风格)。
  - **启动命令**: `npm run dev` (在 `xdr-frontend/` 目录下执行)。

- **`xdr-server/` (后端微服务集群)**
  - **技术栈**: Spring Boot 3 + Spring Cloud Gateway + MyBatis-Plus + Java 17。
  - **核心服务**：
    - `api-gateway` (端口 `8080`)：总网关。
    - `auth-service` (端口 `8081`)：JWT 认证与用户管理。
    - `asset-service` (端口 `8082`)：资产中心 + 资产生命周期管理（`host_asset_record` 表，Upsert/Inactive 流转，30天自动清理）。
    - `baseline-service` (端口 `8083`)：基线管理。
    - `threat-service` (端口 `8084`)：威胁与事件处理（资产同步已迁移至 asset-service，仅做 REST 转发）。
    - `policy-service` (端口 `8085`)：策略下发。
    - `upgrade-service` (端口 `8086`)：OTA 远程升级。
    - `compliance-service` (端口 `8087`)：合规审查。
  - **构建命令**: `mvn clean install -DskipTests` 或 `powershell -File e:\project\xdr-test\scripts\build-backend.ps1`
  - **启停脚本** (覆盖全部 8 个微服务):
    - 启动: `powershell -File e:\project\xdr-test\scripts\start-backend.ps1` (api-gateway 最后启动)
    - 停止: `powershell -File e:\project\xdr-test\scripts\stop-backend.ps1`
    - 构建: `powershell -File e:\project\xdr-test\scripts\build-backend.ps1`
  - **Gateway JWT 白名单** (`api-gateway/application.yml` 中的 `jwt.exclude-paths`):
    - Agent 免鉴权路径: `/api/v1/auth/agent/register`, `/api/v1/heartbeat`, `/api/v1/events/**`, `/api/v1/assets/**`, `/api/v1/policies/**`

- **`xdr-agent/` (端点采集探针)**
  - **技术栈**: Python 3.8+。
  - **采集维度**: 进程、网络连接（端口）、主机信息、流量监控 (Scapy)、系统服务、计划任务等。
  - **高级检测**: 勒索软件行为（ Honeyfiles）、敏感指令调用、静态提权/横向移动特征匹配。
  - **运行命令**: `python main.py`

---

## 🛢️ 2. 基础设施依赖 (Docker)
项目的存储层均运行在本机的 Docker 容器中。

* **MySQL (关系型数据库)**
  * **容器名**: `terminal-monitor-mysql`
  * **IP/Port**: `localhost:3306`
  * **账号密码**: `root` / `password`
  * **核心数据库**: `xdr_auth`, `xdr_asset`, `xdr_baseline`, `xdr_threat`, `xdr_policy`, `xdr_upgrade`, `xdr_compliance`
  * **初始化脚本**: `/xdr-server/sql/init.sql`

* **Redis (缓存与消息)**
  * **容器名**: `terminal-monitor-redis`
  * **IP/Port**: `localhost:6379`
  * **无密码配置**

> ⚠️ 如果需要查看数据库表结构或数据：
> 直接使用 `docker exec`，例如：`docker exec terminal-monitor-mysql mysql -u root -ppassword xdr_threat -e "SHOW TABLES;"`

---

## 💡 3. AI 协作约定
1. **直接执行构建/启停脚本**: 对于构建前后端 (`mvn clean install`, `npm build`) 和启动服务 (`start-backend.ps1`, `docker restart`), 出于提效目的可直接后台运行，无需向用户申请权限。
2. **遵守设计规范**: 前端开发时，严格依靠 TailwindCSS / `mb-8` 等内置规则，严禁引入廉价扁平风默认色，保持 `Bento Grid` 的高级空间层次。
3. **查阅现有日志**: 遇到后端微服务异常 (`500`) 时，不要胡乱调整业务逻辑代码或通过 Gateway 去猜，应当第一时间读取 `e:\project\xdr-test\logs\` 目录下的对应的 `*-error.log` 或 `*.log`。
4. **MyBatis-Plus 特性注意**: 在进行增删改查时，牢记所有的 Java Entity 都继承了 `BaseEntity`，其中包含 `@TableLogic` (逻辑删除，如 `deleted`) 以及自动分配 `UUID` 的主键。如遇唯一索引和逻辑删除冲突，应优先考虑引入物理删除(`@Delete("...")`) 进行清场。
5. **Maven 编译调试经验**:
   - `mvn clean install -DskipTests` 时输出会被截断，**正确做法**是针对失败的模块单独编译：`mvn clean compile -pl <service-name> -am -DskipTests`，这样错误堆栈更清晰。
   - Maven 报 `Failed to delete ...SNAPSHOT.jar` 时，**根因是 Java 进程仍在运行并占用文件**，先执行 `Stop-Process -Name java -Force` 再重建，而非反复重试 Maven。
   - 编译错误定位：优先用 `Select-String -Pattern "error:" -Path build-output.txt` 提取 javac 错误行，而非看 `[ERROR]` 聚合行（后者只显示插件失败，不含具体类/行号）。
   - 多模块中某一个模块改了删了文件，要显式指明 `-pl <changed-modules>` 而不是全量构建，速度更快、错误更集中。
6. **PowerShell 命令运行经验**:
   - `run_command` 工具在 Windows PowerShell 下，命令输出会被大量空行填充，实际内容被压缩。**推荐做法**：对长输出用 `Out-File` 重定向再 `Select-String` 过滤，例如：`mvn ... 2>&1 | Out-File build.log; Select-String "error:" build.log`。
   - 查看微服务运行状态：`netstat -ano | findstr ":808x"` 比 `Get-Process java` 更精确，能直接确认端口是否监听。
   - 后端服务启动后，直接调内部端口（如 `http://localhost:8082`）比走 Gateway (`8080`) 更容易排查是服务本身的问题还是网关问题。
   - Gateway 的 JWT 鉴权拦截会返回 401，与业务 500 需区分：401 先看 `api-gateway/application.yml` 的 `jwt.exclude-paths`；500 先看对应微服务的 `logs/<service>-error.log`。

---

## 🚀 4. 当前进度快照
* **Phase 1 [完成]**: MVP 构建（端 - 后端 - 前端打通）。
* **Phase 2 [完成]**: 高级检测与增强采集（进程、网络流量全覆盖，勒索与基准线检测机制落盘），前端 Fintech Elite 顶级视觉重构。
* **Phase 3 [完成]**: 资产生命周期管理基础建设。
  * ✅ 数据库模型升维 (`xdr_asset.host_asset_record` 模型设计完成)。
  * ✅ 跨服务一致性修复 (RestTemplate 注入与 Gateway 白名单放行)。
  * ✅ Agent 重注册与用户信息 GUI 采集逻辑。
* **Phase 4 [进行中]**: 资产管理服务重构与演进 (Time-Series Tracking)。
  * 🔲 数据库表物理创建 (`host_asset_record`)。
  * 🔲 解析逻辑从 `threat-service` 迁移至 `asset-service`。
  * 🔲 引入时序 Upsert 算法与 Inactive 状态自动流转。
  * 🔲 后端 `/timeline` 时序查询 API。
  * 🔲 前端历史快照时间机器控件。
* **Phase 5 [规划中]**: 基础设施升级 (Kafka + Elasticsearch 支撑等保合规)，全网态势可观测性。
