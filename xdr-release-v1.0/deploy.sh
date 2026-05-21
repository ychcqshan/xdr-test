#!/bin/bash
# XDR 平台一键初始化引导脚本 (v1.0)
# 用法: bash deploy.sh

set -e

echo "=========================================="
echo "    XDR Platform Deployment Suite v1.0    "
echo "=========================================="

# 1. 权限与环境检查
if [ "$EUID" -ne 0 ]; then 
    echo "[!] 请以 root 权限或使用 sudo 运行此脚本"
    exit 1
fi

if ! command -v docker &> /dev/null; then
    echo "[!] 未检测到 Docker，请先安装 Docker 环境"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "[!] 未检测到 Docker Compose，请先安装"
    exit 1
fi

# 2. 启动后端集群
echo "[+] 正在拉起后端微服务集群 (MySQL/Redis/Microservices)..."
cd backend
docker-compose up -d

echo "[+] 后端服务已在后台启动。网关入口: http://localhost:8000"

# 3. 前端部署提示
echo ""
echo "[+] 前端部署说明:"
echo "    1. 请将 frontend/dist 目录下的所有文件拷贝至你的 Nginx 静态资源根目录 (如 /var/www/html/)"
echo "    2. 请将 frontend/nginx.conf 覆盖至 Nginx 配置目录并重启 Nginx"
echo "    3. 访问服务器端口 80 即可打开控制台"

echo ""
echo "=========================================="
echo "    部署指令已提交，请执行 'docker ps' 查看状态    "
echo "=========================================="
