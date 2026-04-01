# 🧬 Error Journal (Self-Evolution Log)

本文档记录项目开发过程中遇到的错误、根因分析和修复方案。  
通用性教训会被提纯至 `.context/memories.md`。

---

## [2026-03-15] 数据库保留字冲突导致全表查询 500
- **Context**: MySQL 表名 `alert`, `event` 在建表和查询时报语法错误
- **What happened**: `AlertMapper.selectList()` 抛 SQL 语法异常
- **Root cause**: `alert` 和 `event` 是 MySQL 8.0 保留字，MyBatis-Plus 自动生成的 SQL 未加反引号转义
- **Fix applied**: 在 Entity 的 `@TableName` 注解中显式加上反引号：`@TableName("\`alert\`")`
- **Lesson learned**: 表名/列名选择时应避免使用 SQL 保留字；若已使用则必须在 ORM 层面强制转义
- **Prevention rule**: ✅ 已提纯至 memories.md

---

## [2026-03-18] Maven 构建 "Failed to delete SNAPSHOT.jar"
- **Context**: `mvn clean install` 反复失败
- **What happened**: clean phase 报 `Failed to delete ...SNAPSHOT.jar`，重试多次仍失败
- **Root cause**: 后台有 `java` 进程锁住了 JAR 文件（Windows 文件锁）
- **Fix applied**: 先 `Stop-Process -Name java -Force` 再重建
- **Lesson learned**: Windows 下 JAR 被运行中的 JVM 锁定时 Maven 无法删除
- **Prevention rule**: ✅ 已提纯至 memories.md

---

## [2026-03-20] Gateway 401 vs 微服务 500 混淆排查
- **Context**: 新增 Agent 端点后接口返回 401
- **What happened**: 误以为是业务逻辑 500，花了很长时间排查 Controller 代码
- **Root cause**: 新路径未加入 `api-gateway/application.yml` 的 `jwt.exclude-paths`
- **Fix applied**: 将路径加入白名单
- **Lesson learned**: 401 和 500 要区分：401 先看 Gateway JWT 白名单；500 先看目标微服务的 error.log
- **Prevention rule**: ✅ 已提纯至 memories.md

---

## [2026-03-22] RestTemplate 注入失败导致跨服务调用 NPE
- **Context**: `asset-service` 调用 `threat-service` 的 REST API 时抛 NullPointerException
- **What happened**: `restTemplate` 字段为 null
- **Root cause**: Spring Boot 3 默认不自动创建 `RestTemplate` Bean，需手动 `@Bean` 声明
- **Fix applied**: 在 `AssetServiceConfig.java` 中添加 `@Bean RestTemplate restTemplate() { return new RestTemplate(); }`
- **Lesson learned**: Spring Boot 3 中 RestTemplate 不再自动注入，每个需要跨服务调用的模块都必须显式注册
- **Prevention rule**: ✅ 已提纯至 memories.md

---

## [2026-03-25] Spring Boot JAR 无法启动 "no main manifest attribute"
- **Context**: `java -jar xxx.jar` 报 `no main manifest attribute`
- **What happened**: JAR 文件内缺少 `MANIFEST.MF` 中的 `Main-Class` 属性
- **Root cause**: `spring-boot-maven-plugin` 的 `repackage` 目标未绑定到 build lifecycle
- **Fix applied**: 在 pom.xml 中显式声明 `<goal>repackage</goal>` 绑定
- **Lesson learned**: 多模块项目中子模块可能不继承 parent 的 plugin 绑定，需显式声明
- **Prevention rule**: ✅ 已提纯至 memories.md

---

## [2026-03-29] processRawEventData 未对 null items 做保护
- **Context**: Phase 4 迁移解析逻辑后，某些 eventType 的数据结构不匹配
- **What happened**: `extractItemsFromEventData` 返回 null 后，`syncAssets` 因 FULL reportType 触发了将所有记录标记为 INACTIVE 的逻辑
- **Root cause**: 未在 `processRawEventData` 入口处添加 null/empty 检查
- **Fix applied**: 增加 `if (items == null || items.isEmpty()) return;` 保护
- **Lesson learned**: 任何从外部数据源提取 List 后，都必须做 null/empty 前置校验，尤其是当下游有"全量清理"语义时
- **Prevention rule**: ✅ 已提纯至 memories.md

---

## [2026-03-29] 前端时光机切换未重置分页状态
- **Context**: 时光机切换历史快照后，列表仍显示旧分页
- **What happened**: 用户切到历史模式后看到空白数据（因为 processPage 停在第 3 页但历史数据只有 1 页）
- **Root cause**: `fetchHistoryData` 只重置了 `processPage`，遗漏了其他 5 个分页变量
- **Fix applied**: 全量重置所有分页: `processPage/networkPage/softwarePage/usbPage/loginPage/trafficPage = 1`
- **Lesson learned**: 涉及数据源切换的场景，必须重置所有 UI 状态（分页、排序、筛选）
- **Prevention rule**: ✅ 已提纯至 memories.md

---

## [2026-03-31] 全局爆红：IDE 报 `com.xdr.common cannot be resolved`
- **Context**: 获取到当前 problems 面板有 70 多条关于 common 包找不到的 Java 错误。
- **What happened**: 所有的服务微服务都报相同的找不到包或实体的红色波浪线。
- **Root cause**: 之前后端的某个微服务 Maven 构建在 clean 阶段因文件被 java 进程占用导致总体失败，VS Code 的 Java 工作空间构建缓存损坏，无法感知 `common` 模块其实已经编译成功。
- **Fix applied**: 杀掉所有 java 进程后，运行 `mvn clean install` 成功，错误实为"幻影错误"。
- **Lesson learned**: 当所有微服务同时报错某一个最底层的包找不到时，不要怀疑代码，首先检查该底层模块是否已成功 compile/install，必要时重启 IDE 的 Java Language Server。
- **Prevention rule**: ⬜ 待提纯
