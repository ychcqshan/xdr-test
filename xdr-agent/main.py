"""
XDR Agent 主入口 - Phase 2 (Advanced Detection & Enriched Collection)
"""
import os
import platform
import logging
import threading
import psutil
import socket
from apscheduler.schedulers.blocking import BlockingScheduler

from agent.core.config import load_config, get_config, get_agent_id, set_agent_id
from agent.core.logger import setup_logger
from agent.collector.process_collector import ProcessCollector
from agent.collector.host_collector import HostCollector
from agent.collector.port_collector import PortCollector
from agent.collector.extra_collectors import ExtraCollector
from agent.collector.traffic_collector import TrafficCollector
from agent.comm.comm_manager import CommManager
from agent.core.incremental_manager import IncrementalManager

from agent.detector.ransomware import RansomwareDetector
from agent.detector.command_monitor import CommandMonitor
from agent.detector.signature_matcher import SignatureMatcher

logger: logging.Logger = None
comm: CommManager = None
incremental: IncrementalManager = None
matcher: SignatureMatcher = None

# 实例列表
collectors = []
detectors = []
traffic_collector: TrafficCollector = None

def report_alert(alert):
    """告警上报回调"""
    # 在上报前先通过特征匹配引擎进行二次过滤/打标
    if alert.get("type") == "COMMAND_EXEC":
        match_res = matcher.match(alert.get("cmd", ""))
        if match_res:
            alert["severity"] = match_res.get("severity", "MEDIUM")
            alert["rule_name"] = match_res.get("name")
            alert["message"] = f"Match Attack Rule: {match_res.get('name')}"
        else:
            # 如果不是黑名单指令，可以根据策略决定是否上报（此处暂定全部上报以便审计）
            pass

    logger.warning(f"检测到风险事件: {alert}")
    comm.report_event(
        event_type="ALERT",
        event_data=alert,
        priority="HIGH"
    )

def collect_and_report():
    """执行采集并上报"""
    for collector in collectors:
        try:
            name = collector.name()
            data = collector.collect()
            if not data:
                continue

            # 1. 进程数据统一对齐 Backend (camelCase)
            if name == 'PROCESS':
                key_fields = ['pid', 'name', 'createTime'] 
                raw_items = data.get("processes", [])
                items = []
                for p in raw_items:
                    items.append({
                        "pid": p.get("pid"),
                        "name": p.get("name"),
                        "createTime": p.get("createTime"),
                        "username": p.get("username"),
                        "cpuPercent": p.get("cpuPercent"),
                        "path": p.get("path", "")
                    })
                
                report_type, diff_items = incremental.get_diff(name, items, key_fields)
                if report_type != "NONE":
                    comm.report_event(event_type=name, event_data={'items': diff_items, 'reportType': report_type}, priority='LOW')

            # 2. 网络数据统一对齐 Backend (camelCase)
            elif name == 'NETWORK':
                key_fields = ['protocol', 'localAddr', 'remoteAddr', 'pid']
                raw_items = data.get("ports", [])
                items = []
                for p in raw_items:
                    items.append({
                        "protocol": p.get("protocol"),
                        "localAddr": p.get("localAddr"),
                        "remoteAddr": p.get("remoteAddr"),
                        "pid": p.get("pid"),
                        "status": p.get("status")
                    })
                
                report_type, diff_items = incremental.get_diff(name, items, key_fields)
                if report_type != "NONE":
                    comm.report_event(event_type=name, event_data={'items': diff_items, 'reportType': report_type}, priority='LOW')
            
            elif name == 'EXTRA':
                # 拆分为独立事件并分别走增量比对
                if 'softwares' in data and data['softwares']:
                    sw_keys = ['name', 'version']
                    sw_type, sw_diff = incremental.get_diff('SOFTWARE', data['softwares'], sw_keys)
                    if sw_type != "NONE":
                        comm.report_event(event_type='SOFTWARE', event_data={'items': sw_diff, 'reportType': sw_type}, priority='LOW')
                if 'usb_devices' in data and data['usb_devices']:
                    usb_keys = ['deviceId']
                    usb_type, usb_diff = incremental.get_diff('USB', data['usb_devices'], usb_keys)
                    if usb_type != "NONE":
                        comm.report_event(event_type='USB', event_data={'items': usb_diff, 'reportType': usb_type}, priority='LOW')
                if 'logins' in data and data['logins']:
                    login_keys = ['userName', 'loginTime']
                    login_type, login_diff = incremental.get_diff('LOGIN', data['logins'], login_keys)
                    if login_type != "NONE":
                        comm.report_event(event_type='LOGIN', event_data={'items': login_diff, 'reportType': login_type}, priority='LOW')
            elif name == 'TRAFFIC':
                if 'connections' in data:
                    comm.report_event(event_type='TRAFFIC', event_data={'items': data['connections'], 'reportType': 'FULL'}, priority='LOW')
            else:
                # 不支持增量的直接全量
                comm.report_event(
                    event_type=name,
                    event_data={'items': data, 'reportType': 'FULL'},
                    priority='LOW'
                )
        except Exception as e:
            logger.error(f"采集异常: {e}")

def poll_and_execute_commands():
    """轮询并执行后端下发的处置指令"""
    try:
        commands = comm.get_pending_commands()
        for cmd in commands:
            cmd_id = cmd.get("id")
            cmd_type = cmd.get("commandType")
            try:
                data = json.loads(cmd.get("commandData", "{}"))
            except:
                data = {}

            logger.info(f"开启执行指令: {cmd_type} {data}")
            
            success = False
            error_msg = None

            try:
                if cmd_type == "TERMINATE_PROCESS" or cmd_type == "TERMINATE":
                    pid = int(data.get("pid", 0))
                    if pid > 0 and psutil.pid_exists(pid):
                        p = psutil.Process(pid)
                        p.terminate()
                        success = True
                    else:
                        error_msg = f"PID {pid} 不存在"
                elif cmd_type == "DELETE_FILE":
                    path = data.get("filePath")
                    if path and os.path.exists(path):
                        os.remove(path)
                        success = True
                    else:
                        error_msg = f"文件 {path} 不存在"
                else:
                    error_msg = f"不支持的指令类型: {cmd_type}"
            except Exception as e:
                error_msg = str(e)
            
            if success:
                comm.update_command_status(cmd_id, "EXECUTED")
                logger.info(f"指令执行成功: {cmd_id}")
            else:
                comm.update_command_status(cmd_id, "FAILED", error_msg)
                logger.error(f"指令执行失败: {cmd_id}, 错误: {error_msg}")

    except Exception as e:
        logger.error(f"执行器状态异常: {e}")

def send_heartbeat():
    """增强版心跳 - 补全资产识别字段"""
    mem = psutil.virtual_memory()
    # 获取主网卡 IP/MAC
    ip_addr = "127.0.0.1"
    mac_addr = "00:00:00:00:00:00"
    try:
        for interface, addrs in psutil.net_if_addrs().items():
            for addr in addrs:
                if addr.family == socket.AF_INET and not addr.address.startswith("127."):
                    ip_addr = addr.address
                if addr.family == psutil.AF_LINK: # MAC
                    mac_addr = addr.address
    except: pass

    data = {
        'hostname': platform.node(),
        'osType': platform.system().upper(),
        'osVersion': platform.version(),
        'cpuArch': platform.machine(),
        'memoryTotal': mem.total,
        'ipAddress': ip_addr,
        'macAddress': mac_addr,
        'agentVersion': '2.1.0',
    }
    comm.heartbeat(data)

def main():
    global logger, comm, collectors, incremental, traffic_collector, matcher

    load_config()
    logger = setup_logger()
    logger.info("=" * 50)
    logger.info("XDR Agent Phase 2 启动中...")

    # ====== 重注册逻辑 ======
    cfg = get_config()
    need_register = cfg['agent'].get('re_register', False) or not get_agent_id()

    if need_register:
        logger.info("检测到重注册标志或缺少 AgentID，开始执行全新注册流程...")

        # 1. 清除旧缓存
        import sqlite3
        cache_db = os.path.join(os.path.dirname(__file__), 'cache.db')
        if os.path.exists(cache_db):
            try:
                conn = sqlite3.connect(cache_db)
                conn.execute("DELETE FROM user_info WHERE 1=1")
                conn.execute("DELETE FROM event_cache WHERE 1=1")
                conn.commit()
                conn.close()
                logger.info("已清除本地缓存数据")
            except Exception as e:
                logger.warning(f"清除缓存时出错(可忽略): {e}")

        # 2. 生成新 AgentID 并向后端注册
        import uuid
        new_agent_id = f"AGENT-{uuid.uuid4().hex[:16].upper()}"
        logger.info(f"生成新 AgentID: {new_agent_id}")

        set_agent_id(new_agent_id)

        # 向 auth-service 注册
        import requests
        try:
            resp = requests.post(
                f"{cfg['agent']['server_url']}/api/v1/auth/agent/register",
                json={
                    'agentId': new_agent_id,
                    'hostname': platform.node(),
                    'osType': platform.system().upper()
                },
                timeout=10
            )
            if resp.status_code == 200:
                logger.info(f"后端注册成功: {new_agent_id}")
            else:
                logger.warning(f"后端注册返回异常: {resp.status_code}")
        except Exception as e:
            logger.warning(f"后端注册请求失败(将在心跳时自动补偿): {e}")

        # 3. 回写 re_register = false
        cfg['agent']['re_register'] = False
        from agent.core.config import save_config
        save_config()
        logger.info("re_register 标志已自动回写为 false")

    # ====== 初始化通信 ======
    comm = CommManager()
    incremental = IncrementalManager()

    # 4. 弹窗采集用户信息（新注册 或 本地无记录时触发）
    if need_register:
        logger.info("弹出用户信息采集窗口...")
        from agent.user_info.manager import UserInfoManager
        user_mgr = UserInfoManager(comm)
        user_mgr.check_and_collect()
    
    # 规则引擎
    rules_path = os.path.join(os.path.dirname(__file__), "agent/rules/attack_features.yaml")
    matcher = SignatureMatcher(rules_path)

    # 1. 启动实时检测模块 (线程驱动)
    logger.info("启动实时检测模块...")
    
    # 指令监控
    cmd_monitor = CommandMonitor(report_alert)
    cmd_monitor.start()
    
    # 勒索监控
    ransom_detector = RansomwareDetector(report_alert)
    ransom_detector.start()
    
    # 2. 启动流量监听 (混杂模式)
    logger.info("开启网卡混杂模式流量监听...")
    traffic_collector = TrafficCollector()
    traffic_collector.start_sniffing()

    # 3. 初始化周期采集器
    from agent.collector.host_collector import HostCollector
    from agent.collector.port_collector import PortCollector
    collectors = [
        ProcessCollector(),
        HostCollector(),
        PortCollector(),
        ExtraCollector(),
        traffic_collector
    ]

    # 4. 立即执行首次数据上报与心跳
    logger.info("执行首次心跳与资产上报...")
    try:
        send_heartbeat()
        collect_and_report()
    except Exception as e:
        logger.error(f"首次上报失败: {e}")

    # 5. 启动定时任务
    scheduler = BlockingScheduler()
    scheduler.add_job(collect_and_report, 'interval', seconds=60, id='collect')
    scheduler.add_job(send_heartbeat, 'interval', seconds=300, id='heartbeat')
    scheduler.add_job(poll_and_execute_commands, 'interval', seconds=10, id='commands')

    logger.info("XDR Agent Phase 2 运行中...")

    try:
        scheduler.start()
    except (KeyboardInterrupt, SystemExit):
        logger.info("正在停止 Agent...")
        cmd_monitor.stop()
        ransom_detector.stop()
        traffic_collector.stop()

if __name__ == '__main__':
    main()
