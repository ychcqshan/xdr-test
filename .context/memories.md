# 🧠 Project Memories & Lessons Learned

本文档记录了项目开发过程中沉淀的经验、教训及避坑指南。AI Agent 在开始任务前应检索此文档。

---

## 🛠️ 后端开发 (Spring Boot / Java)

### 1. 编译与运行环境
- **Lombok 与 JDK 版本**: 项目目前使用 Java 17/21 环境。注意高版本 JDK 可能与旧版本 Lombok 不兼容，若出现编译错误，优先检查 Lombok 本身及相关插件版本。
- **文件占用问题**: Maven 构建报错 `Failed to delete ...SNAPSHOT.jar` 时，通常是因为后台 Java 进程未关闭。
  - **建议**: 先执行 `Stop-Process -Name java -Force` 再重试。

### 2. 序列化与反序列化 (Jackson)
- **DTO vs Entity**: 严禁直接将 MyBatis-Plus 的 `Entity` 直接作为 Controller 的 `@RequestBody` 参数。
  - **原因**: Entity 包含大量框架注解（如 `@TableLogic`, `@TableId`）和默认值，容易导致 Jackson 序列化失败或逻辑错误。
  - **对策**: 务必使用专门的 `DTO` 或 `Request` 类进行数据承载。

### 3. MyBatis-Plus 与 逻辑删除
- **唯一索引冲突**: 当表中使用 `@TableLogic` (deleted 字段) 时，由于逻辑删除后旧数据仍存在，会导致新的插入在唯一索引上冲突。
  - **对策**: 如果业务场景要求“物理上的重名不可行”，应考虑使用物理删除 (`@Delete`) 或将 `deleted` 字段加入唯一索引组合键。

---

## 🔐 安全与网关 (Gateway / Auth)

### 1. JWT 鉴权与白名单
- **Agent 通讯**: `xdr-agent` 注册与心跳不带 JWT。
  - **对策**: 任何涉及 Agent 调用的新路径，必须同步更新 `api-gateway/application.yml` 中的 `jwt.exclude-paths` 白名单。

### 2. 跨服务调用
- **RestTemplate**: 系统内跨服务调用通常不带 JWT 头部。若被调用服务非白名单，需手动在 Ribbon/OpenFeign/RestTemplate 中透传或配置免鉴权。

---

## 🐚 脚本与命令行 (Windows PowerShell)

### 1. 准确性高于一切
- **服务状态**: `Get-Process java` 并不代表服务已就绪。
  - **对策**: 使用 `netstat -ano | findstr ":808x"` 确认端口监听才是硬指标。
- **日志截断**: `run_command` 输出长文本会被截断。
  - **对策**: 将输出重定向到文件 (`Out-File`) 后再使用 `Select-String` 进行关键词搜索。
- **环境维护**: 项目运行久了会产生大量 `.log` 和临时 `.txt`。
  - **对策**: 定期运行 `powershell -File scripts/clean-logs.ps1` 保持工作区整洁。

---

## 🎨 前端开发 (Vue 3 / Vite)

### 1. 视觉层次 (Bento Grid)
- **设计风格**: 严格遵循 Fintech Elite 风格。
  - **对策**: 禁止使用 Element Plus 的默认原生圆角和硬边框。优先使用 Tailwind 的 `rounded-2xl`, `shadow-xl`, `backdrop-blur` 和渐变背景。

### 2. 数据源切换时必须重置 UI 状态
- **场景**: 时光机切换、筛选条件变化、Tab 切换等导致数据源变更。
  - **对策**: 必须同时重置所有关联的分页变量、排序状态、选中项。仅重置部分变量会导致空白页或数据错位。
  - 🔗 来源: `error_journal.md [2026-03-29] 前端时光机切换未重置分页状态`

### 3. Element Plus 组件的显式导入
- **场景**: 使用 `ElMessage`, `ElMessageBox`, `ElNotification` 等命令式组件。
  - **对策**: 必须显式 `import { ElMessage } from 'element-plus'`，不依赖自动导入（unplugin-auto-import 对命令式 API 不生效）。

---

## 🔄 微服务数据流 (跨服务通信)

### 1. 外部数据提取后的 null 保护
- **场景**: 从 Agent 上报数据中按 key 提取 List 时，可能因结构不匹配返回 null。
  - **对策**: 任何 `extractXxx()` 返回值必须做 `null/empty` 前置校验，**尤其当下游有"全量清理"或"覆盖式写入"语义时**（如 FULL reportType 会将缺失项标记为 INACTIVE）。
  - 🔗 来源: `error_journal.md [2026-03-29] processRawEventData 未对 null items 做保护`

### 2. 微服务职责边界
- **原则**: 数据归谁管，解析就由谁做。
  - **反模式**: `threat-service` 负责解析资产数据结构再转发给 `asset-service`（越俎代庖）。
  - **正确做法**: `threat-service` 原生透传，`asset-service` 内部自行 `extractItemsFromEventData`。
  - 🔗 来源: Phase 4 资产时序管理重构

### 3. Spring Boot 3 中 RestTemplate 需手动注册
- **场景**: 跨服务 REST 调用时 `restTemplate` 为 null。
  - **对策**: Spring Boot 3 不再自动创建 `RestTemplate` Bean，每个需要的模块必须在 `@Configuration` 中显式 `@Bean` 声明。

---

## 📐 架构与编码规范

### 1. MySQL 保留字转义
- **场景**: 表名或列名使用了 SQL 保留字（如 `alert`, `event`, `order`）。
  - **对策**: 在 MyBatis-Plus `@TableName` 中用反引号包裹，或在建表时就避免使用保留字命名。

### 2. Maven 多模块编译调试
- **快速定位**: 全量 `mvn clean install` 的输出太长，错误被吞。
  - **对策**: 用 `-pl <module> -am` 单模块编译；用 `Select-String "error:" build.log` 提取 javac 错误行，而非看 `[ERROR]` 聚合行。
- **Repackage 绑定**: 多模块项目子模块可能不继承 parent 的 `spring-boot-maven-plugin` 绑定。
  - **对策**: 每个可执行微服务的 pom.xml 中显式声明 `<goal>repackage</goal>`。
- **IDE 全局爆红幻觉**: 当所有服务同时爆红报错底层模块（如 `common`）找不到时。
  - **对策**: 绝大多数是此前的 Maven `clean` 构建因为文件被 Java 进程占用失败导致的缓存损坏。首先 `Stop-Process -Name java -Force`，然后重新跑全量 `mvn clean install -DskipTests`，编译成功即证明是幻影错误，直接重启 VS Code 的 Java Language Server。
  - 🔗 来源: `error_journal.md [2026-03-31] 全局爆红：IDE 报 com.xdr.common cannot be resolved`
