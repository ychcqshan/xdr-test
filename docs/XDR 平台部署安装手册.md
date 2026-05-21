# XDR 平台一键部署安装手册 (v1.0)

本手册详细介绍了如何在一台全新的 Linux 服务器及 Windows 终端上快速部署 XDR 可视化决策系统（v1.0）。

---

## 1. 快速开始 (Linux 生产环境)

### 1.1 环境检查
- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **内存**: 建议 16GB+
- **磁盘**: 建议 100GB+

### 1.2 后端集群一键启动
1. 进入部署包 `backend` 目录。
2. 执行以下命令：
   ```bash
   # 启动基础设施 (MySQL, Redis) 与 8 个微服务
   docker-compose up -d --build
   ```
3. 验证服务状态：
   ```bash
   docker ps  # 确认 10 个容器（9服务+1网关）均处于 Up 状态
   ```

### 1.3 前端服务配置
1. 安装 Nginx 并将 `dist` 目录内容拷贝至 `/var/www/html`。
2. 替换 `/etc/nginx/conf.d/default.conf` 为本包提供的 `nginx.conf`。
3. 重启 Nginx：`systemctl restart nginx`。
4. 访问服务器 IP 即可进入 XDR 控制台。

---

## 2. 端口映射规范 (内网防火墙参考)

| 服务名称 | 内部端口 | 暴露端口 | 用途 |
| :--- | :--- | :--- | :--- |
| **API Gateway** | 8000 | 8000 | **唯一入口/网关** |
| Auth Service | 8001 | - | 认证与权限 |
| Asset Service | 8002 | - | 资产管理与心跳 |
| Baseline Service | 8003 | - | 安全基线检测 |
| Threat Service | 8004 | - | 威胁告警与取证 |
| Policy Service | 8005 | - | 策略下发 |
| Upgrade Service | 8006 | - | OTA 升级 |
| Compliance Service | 8007 | - | 合规审查 |

---

## 3. 终端 Agent 部署

### 3.1 Windows 终端
1. 将 `agent/xdr-agent-win.exe` 拷贝至目标机器。
2. 修改同目录下的 `config.yaml`，将 `server_url` 指向服务器 IP:8000。
3. **右键以管理员身份运行**即可。

### 3.2 Linux 终端
1. 将 `agent/xdr-agent-linux` 拷贝至目标机器。
2. 授予执行权限：`chmod +x xdr-agent-linux`。
3. 运行 `./install.sh` 脚本，它会自动配置 Systemd 服务并启动。

---

## 4. 常见问题排查 (FAQ)
- **数据库无法连接？**: 检查 `xdr-mysql` 容器是否启动成功，初次启动可能需要 30-60s 进行初始化。
- **Agent 无法上线？**: 确保 8000 端口在服务器防火墙中已开放，且 `config.yaml` 中配置的 IP 正确。
- **前端 404？**: 确保 Nginx 配置文件中的 `try_files` 设置正确。
