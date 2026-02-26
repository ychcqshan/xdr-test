# XDR 系统部署与运维指南

**文档版本**：V1.0
**更新日期**：2026-02-24

---

## 目录
1. [环境要求](#1-环境要求)
2. [数据库初始化](#2-数据库初始化)
3. [后端微服务部署](#3-后端微服务部署)
4. [前端控制台部署](#4-前端控制台部署)
5. [终端 Agent 部署](#5-终端-agent-部署)
6. [日常运维与脚本使用](#6-日常运维与脚本使用)
7. [常见故障排查](#7-常见故障排查)

---

## 1. 环境要求

### 1.1 硬件要求
- **管理平台服务器（单机最小要求）**：8核 CPU，16GB 内存，100GB 硬盘空间。
- **终端 Agent**：50MB 磁盘空间，运行时内存 ≤100MB，CPU 占用 ≤5%。

### 1.2 软件依赖
- **操作系统**：Windows Server 2012+ / Linux (Ubuntu 20.04+, CentOS 7+)
- **JDK**：Java 17 及其以上版本
- **数据库**：MySQL 8.0 及其以上版本
- **缓存**：Redis 6.0 及其以上版本
- **Node.js**：v18 及其以上版本（用于前端编译）
- **Python**：Python 3.8+（用于 Agent 运行与打包）

---

## 2. 数据库初始化

系统包含四个独立的业务数据库：`xdr_auth`, `xdr_asset`, `xdr_baseline`, `xdr_threat`。

### 操作步骤
1. 登录 MySQL 数据库：
   ```bash
   mysql -u root -p
   ```
2. 执行初始化脚本，创建数据库与基础表结构，并写入初始默认管理员（用户名：`admin`，密码：`admin123`）：
   ```bash
   mysql> source e:/project/xdr-test/xdr-server/sql/init.sql;
   ```
3. 确保各个后端的 `application.yml` 中的数据库配置（用户名、密码、端口）与实际 MySQL 环境匹配。

---

## 3. 后端微服务部署

后端微服务基于 Spring Boot 和 Spring Cloud Gateway 构建。

### 3.1 模块与端口说明
| 模块名称 | 默认端口 | 职责说明 |
| :--- | :--- | :--- |
| `api-gateway` | 8080 | 统一API入口、路由分发、JWT鉴权拦截 |
| `auth-service` | 8081 | 系统登录、Agent认证、Token颁发 |
| `asset-service` | 8082 | 资产信息管理与心跳维护 |
| `baseline-service` | 8083 | 系统合规与基线状态比对 |
| `threat-service` | 8084 | 安全事件告警与分析 |

### 3.2 编译与打包
进入后端项目根目录 `xdr-server`，执行 Maven 构建：
```bash
cd e:\project\xdr-test\xdr-server
mvn clean install -DskipTests
```
构建成功后，各个子模块的 `target/` 目录下将生成类似 `xxx-service-1.0.0-SNAPSHOT.jar` 的文件。

### 3.3 启动服务
你可以使用预置的 PowerShell 脚本来管理所有服务：
```powershell
# 启动所有后端服务
.\scripts\start-backend.ps1
```

---

## 4. 前端控制台部署

前端基于 Vue 3 + Vite 构建。

### 4.1 本地开发运行
```bash
cd e:\project\xdr-test\xdr-frontend
npm install
npm run dev
```
将自动在 `http://localhost:5173` （或相近端口）启动服务。前端会将 `/api` 请求代理到 网关 `http://localhost:8080`。

### 4.2 生产环境编译
```bash
cd e:\project\xdr-test\xdr-frontend
npm run build
```
编译产物会生成在 `dist/` 目录下。可以将该目录放置于 Nginx 静态资源目录下进行部署。

**Nginx 代理配置示例**：
```nginx
server {
    listen 80;
    server_name xdr.example.com;

    location / {
        root /path/to/xdr-frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 5. 终端 Agent 部署

Agent 依赖 Python 环境和 `psutil` 等系统相关库。

### 5.1 环境准备
```bash
cd e:\project\xdr-test\xdr-agent
pip install -r requirements.txt
```

### 5.2 运行 Agent
由于采集系统信息，建议使用**管理员/root权限**运行：
```bash
# Windows (使用管理员身份运行PowerShell或CMD)
python main.py

# Linux
sudo python3 main.py
```

### 5.3 打包为可执行程序（可选）
可使用 PyInstaller 将应用打包为无依赖单文件：
```bash
pip install pyinstaller
pyinstaller --onefile main.py
```
生成的 `main.exe` 或可执行文件即可直接在目标机器分发运行。

---

## 6. 日常运维与脚本使用

项目在 `scripts/` 目录下预置了用于日常运维的批处理/Powershell脚本：

### 6.1 `start-backend.ps1`
**功能**：一键检查 Java 状态、执行 Maven 编译缺失构建，并按顺序为 5 个微服务开启独立窗口启动。
**用法**：
```powershell
.\scripts\start-backend.ps1
```

### 6.2 `stop-backend.ps1`
**功能**：一键检测 8080-8084 占用端口，并通过强杀进程 (PID) 干净地关闭所有运行中的 XDR 后端服务。
**用法**：
```powershell
.\scripts\stop-backend.ps1
```
### 6.3 典型重启与日志查看流程
启动脚本会将各服务的标准输出和错误输出重定向到 `logs/` 目录下的日志文件中。典型操作流程如下：
```powershell
# 先关掉旧的后端
.\scripts\stop-backend.ps1

# 重新启动（日志将输出到 logs 目录）
.\scripts\start-backend.ps1

# 查看日志
Get-Content .\logs\api-gateway.log -Tail 50 -Wait
Get-Content .\logs\auth-service.log -Tail 50 -Wait
Get-Content .\logs\auth-service-error.log -Tail 50 -Wait
```

**日志文件说明**：
| 日志文件 | 说明 |
| :--- | :--- |
| `logs/api-gateway.log` | 网关标准输出 |
| `logs/api-gateway-error.log` | 网关错误输出 |
| `logs/auth-service.log` | 认证服务标准输出 |
| `logs/auth-service-error.log` | 认证服务错误输出 |
| `logs/asset-service.log` | 资产服务标准输出 |
| `logs/baseline-service.log` | 基线服务标准输出 |
| `logs/threat-service.log` | 威胁服务标准输出 |

---

## 7. 常见故障排查

### 7.1 前端提示 "网络错误" 或 "ECONNREFUSED"
**原因**：网关 (`api-gateway`) 未启动，或 API 路由代理目标服务（如 `auth-service`）不可用。
**解决**：
1. 运行 `Get-NetTCPConnection -LocalPort 8080,8081` 检查端口状态。
2. 确保 MySQL 已经启动。数据库未启动会导致应用初始化失败而退出。
3. 检查后端窗口日志是否有 `SQLException` 或者 `RedisConnectionException`。

### 7.2 MySQL "拒绝访问" 或服务无法启动
**原因**：权限不足，或数据库存在损坏/冲突。
**解决**：
1. 必须以管理员权限启动终端运行 `net start MySQL80`。
2. 检查 `services.msc` 中 MySQL80 的运行状态。

### 7.3 Maven 编译报错 `LifecyclePhaseNotFoundException`
**原因**：在使用命令行 `mvn` 覆盖参数时，参数格式不正确或被 shell 解释错误。
**解决**：
推荐通过 `application.yml` 调整配置。若必需传入参数，注意引号包裹：
```bash
mvn spring-boot:run -pl auth-service -D "spring-boot.run.arguments=--server.port=8081"
```
