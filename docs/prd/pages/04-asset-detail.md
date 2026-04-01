# 资产详情

> **路由:** `/assets/:id`
> **模块:** 资产管理
> **组件:** `AssetDetailView.vue`

## 概述
资产详情页是单台终端的360度安全全景视图。展示该终端的基本信息、实时采集数据（进程、网络连接、服务等）、入侵痕迹报告，以及历史快照时间机器功能。

## 布局
- **页面头部**: 返回箭头 + 主机名 + 状态标签 + 最后心跳时间
- **Tab 信息区**: 多标签页展示各维度数据
- **时间机器**: 历史快照选择器

## 核心功能

### 实时视图
- 进程列表、网络连接、系统服务、计划任务等采集数据展示
- 数据来源: `GET /api/v1/assets/:agentId/details`

### 入侵痕迹报告
- 勒索软件检测、敏感指令、提权/横向移动特征展示
- 数据来源: `GET /api/v1/assets/:agentId/intrusion-reports`
- 组件: `IntrusionReport.vue`

### 历史快照时间机器
- 选择历史时间点回溯终端状态
- 数据来源: `GET /api/v1/assets/:agentId/timeline?timestamp=xxx`
- 切换快照时全量重置 UI 状态

## API 依赖

| API | 方法 | 路径 | 触发 |
|:---|:---|:---|:---|
| 资产详情 | GET | `/api/v1/assets/:id` | 页面加载 |
| 采集数据 | GET | `/api/v1/assets/:agentId/details` | Tab 切换/刷新 |
| 历史时间线 | GET | `/api/v1/assets/:agentId/timeline` | 时间机器操作 |
| 入侵报告 | GET | `/api/v1/assets/:agentId/intrusion-reports` | 入侵Tab加载 |
| 网络拓扑 | GET | `/api/v1/assets/topology` | 拓扑Tab加载 |

## 页面关系
- **流入**: 资产列表 → 点击行跳转，携带 `{id, agentId}`
- **流出**: ← 返回资产列表
