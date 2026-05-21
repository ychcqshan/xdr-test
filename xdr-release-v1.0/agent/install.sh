#!/bin/bash
# XDR Agent Linux 安装引导脚本 (v1.0)
# 用法: sudo bash install.sh

AGENT_BIN="./xdr-agent-linux"
CONFIG_FILE="./config.yaml"
INSTALL_DIR="/opt/xdr-agent"

echo "------------------------------------------"
echo "   XDR Terminal Agent Installer v1.0      "
echo "------------------------------------------"

# 1. 创建安装目录
if [ ! -d "$INSTALL_DIR" ]; then
    echo "[+] Creating installation directory: $INSTALL_DIR"
    mkdir -p "$INSTALL_DIR"
fi

# 2. 拷贝二进制文件与配置
echo "[+] Copying files..."
if [ -f "$AGENT_BIN" ]; then
    cp "$AGENT_BIN" "$INSTALL_DIR/"
    chmod +x "$INSTALL_DIR/xdr-agent-linux"
else
    echo "[!] Warning: $AGENT_BIN not found. Please ensure the binary is in current directory."
fi

if [ -f "$CONFIG_FILE" ]; then
    cp "$CONFIG_FILE" "$INSTALL_DIR/"
else
    echo "[!] Warning: $CONFIG_FILE not found. Using default internal config."
fi

# 3. 注册为 Systemd 服务
echo "[+] Registering systemd service: xdr-agent.service"
cat > /etc/systemd/system/xdr-agent.service <<EOF
[Unit]
Description=XDR Terminal Agent Service
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=$INSTALL_DIR
ExecStart=$INSTALL_DIR/xdr-agent-linux
Restart=always
RestartSec=5
StartLimitInterval=0

[Install]
WantedBy=multi-user.target
EOF

# 4. 启动服务
echo "[+] Reloading systemd and starting service..."
systemctl daemon-reload
systemctl enable xdr-agent
systemctl restart xdr-agent

echo "------------------------------------------"
echo "   Installation Completed Successfully    "
echo "   Status: systemctl status xdr-agent     "
echo "------------------------------------------"
