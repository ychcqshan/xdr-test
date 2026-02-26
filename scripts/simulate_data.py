import requests
import time
import uuid
import json

BASE_URL = "http://localhost:8080" # API Gateway

def simulate_agent_flow():
    # 1. 注册 Agent
    agent_id = str(uuid.uuid4())
    print(f"[*] 模拟 Agent 注册: {agent_id}")
    reg_data = {
        "agentId": agent_id,
        "hostname": "test-host-01",
        "ip": "192.168.1.100",
        "os": "Windows 11",
        "version": "1.0.0"
    }
    # 模拟后端逻辑，通常注册在 auth-service
    # requests.post(f"{BASE_URL}/auth/agent/register", json=reg_data)

    # 2. 发送心跳
    print("[*] 发送心跳...")
    # requests.post(f"{BASE_URL}/asset/heartbeat", json={"agentId": agent_id})

    # 3. 模拟上报基线偏差事件
    print("[*] 上报基线偏差事件...")
    event_data = {
        "agentId": agent_id,
        "eventType": "BASELINE_DIFF",
        "totalDiff": 12,
        "type": "PROCESS",
        "details": [
            {"name": "malware.exe", "path": "C:\\temp\\", "action": "ADDED"}
        ],
        "timestamp": int(time.time() * 1000)
    }
    # 发送到 threat-service
    # requests.post(f"{BASE_URL}/threat/events", json=event_data)

    print("[!] 脚本仅作为数据格式参考，请在确保后端微服务全部启动后使用。")

if __name__ == "__main__":
    simulate_agent_flow()
