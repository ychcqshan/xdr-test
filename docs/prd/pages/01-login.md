# 登录

> **路由:** `/login`
> **模块:** 认证
> **组件:** `LoginView.vue`

## 概述
系统登录页，用户输入账号密码进行身份验证。认证通过后获取 JWT Token 并跳转至安全概览。

## 交互

### 登录流程
- **触发**: 用户输入用户名+密码后点击登录
- **API**: `POST /api/v1/auth/login` → `{username, password}`
- **成功**: 存储 `accessToken` 至 localStorage → 跳转 `/dashboard`
- **失败**: 显示错误信息

### Token 管理
- 登录成功后写入 `localStorage.accessToken`
- 后续所有请求自动附加 `Authorization: Bearer <token>`
- Token 过期时自动清空并跳转登录

## API 依赖

| API | 方法 | 路径 | 参数 |
|:---|:---|:---|:---|
| 登录 | POST | `/api/v1/auth/login` | username, password |
| 登出 | POST | `/api/v1/auth/logout` | — |
| 刷新 Token | POST | `/api/v1/auth/refresh` | refreshToken |
