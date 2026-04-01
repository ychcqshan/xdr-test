---
description: 多智能体协作开发全流程（需求→架构→开发→测试→审查→部署→文档）
---
// turbo-all

# 🤖 多智能体协作开发工作流 (Multi-Agent Dev Pipeline)

## 概述
本工作流定义了从需求到部署的完整智能体协作链。每个阶段由专职 Skill 承担。

---

## Stage 0: 上下文加载
1. 读取 `gemini.md` 获取项目架构约定
2. 读取 `.context/memories.md` 获取避坑指南
3. 读取 `artifacts/error_journal.md` 获取历史 Bug 记录

## Stage 1: 需求分析与设计 (Architect Agent)
1. 触发 `brainstorming` skill 进行需求探索和意图确认
2. 触发 `writing-plans` skill 编写 `implementation_plan.md`
3. 如涉及 API 变更，触发 `api-design-reviewer` 进行契约审查
4. 如涉及数据库变更，触发 `database-schema-designer` 进行 schema 设计
5. 等待用户 Approve 计划

## Stage 2: 任务拆解与分发 (Orchestrator Agent)
1. 将 `implementation_plan.md` 拆解为可执行子任务
2. 触发 `dispatching-parallel-agents` 分发独立的子任务
3. 触发 `using-git-worktrees` 为每个子任务创建隔离工作区（如需并行开发）

## Stage 3: 编码实现 (Developer Agent)
1. 触发 `subagent-driven-development` 或 `executing-plans` 逐任务执行
2. 如采用 TDD 模式，触发 `test-driven-development` 执行 RED-GREEN-REFACTOR
3. 如涉及 Docker，触发 `docker-development` 进行容器编排
4. 遵守 `rules.md` 中的编码标准

## Stage 4: 测试验证 (Tester Agent)
1. 触发 `verification-before-completion` 确认代码编译通过
2. 触发 `api-test-suite-builder` 执行后端 API 冒烟测试
3. 使用浏览器测试 skill (`webapp-testing`) 验证前端页面功能
4. 触发 `performance-profiler` 进行性能基准测试（可选）

## Stage 5: 代码审查 (Reviewer Agent)
1. 触发 `requesting-code-review` 提交审查请求
2. 触发 `pr-review-expert` 执行自动化代码审查
3. 如收到反馈，触发 `receiving-code-review` 评估并应用修改
4. 触发 `systematic-debugging` 处理审查中发现的 Bug

## Stage 6: 版本管理与发布 (Release Agent)
1. 触发 `finishing-a-development-branch` 完成开发分支（merge/PR/cleanup）
2. 触发 `changelog-generator` 生成变更日志
3. 触发 `release-manager` 管理版本号和发布流程
4. 触发 `dependency-auditor` 审计依赖安全性
5. 触发 `env-secrets-manager` 检查环境变量安全

## Stage 7: 部署 (DevOps Agent)
1. 触发 `ci-cd-pipeline-builder` 构建/更新 CI/CD 流水线
2. 触发 `migration-architect` 管理数据库迁移（如需）
3. 执行 `scripts/build-backend.ps1` 构建
4. 执行 `scripts/start-backend.ps1` 部署
5. 使用 `netstat -ano | findstr ":808"` 验证端口

## Stage 8: 文档与自我进化 (Documentation Agent)
1. 更新 `docs/api_design.md`、`docs/roadmap.md`、`gemini.md` 等文档
2. 如遇 Bug，按 `rules.md` 的自我进化流程执行：
   - 根因分析 → 写 `error_journal.md` → 提纯至 `memories.md` → 反标已提纯
3. 触发 `tech-debt-tracker` 记录技术债务
4. 创建 `walkthrough.md` 记录本次开发成果

---

## 快速触发指南

| 场景 | 命令 |
|:---|:---|
| 完整开发流程 | `/multi-agent-dev` (本工作流) |
| 仅架构设计 | 触发 `brainstorming` + `writing-plans` |
| 仅代码开发 | 触发 `executing-plans` |
| 仅测试 | 触发 `api-test-suite-builder` + `verification-before-completion` |
| 仅发布 | 触发 `release-manager` + `changelog-generator` |
| Bug 修复 | 触发 `systematic-debugging` + `test-driven-development` |
