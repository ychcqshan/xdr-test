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
