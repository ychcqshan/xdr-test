"""
XDR Agent 主入口 - Phase 2 (Advanced Detection & Enriched Collection)
"""
import os
import platform
import logging
import threading
import psutil
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
            name = collector.__class__.__name__.replace("Collector", "").upper()
            data = collector.collect()
            if not data:
                continue

            # 定义增量识别键
            key_fields = []
            if name == 'PROCESS':
                key_fields = ['pid', 'name', 'create_time']
            elif name == 'PORT':
                key_fields = ['protocol', 'local_addr', 'remote_addr', 'pid']
            
            if key_fields:
                report_type, items = incremental.get_diff(name, data.get(name.lower() + "es", data.get(name.lower() + "s", [])), key_fields)
                if report_type == "NONE":
                    continue
                
                comm.report_event(
                    event_type=name,
                    event_data={
                        'items': items, 
                        'count': len(items),
                        'reportType': report_type
                    },
                    priority='LOW'
                )
            else:
                # 不支持增量的直接全量
                comm.report_event(
                    event_type=name,
                    event_data={'items': data, 'reportType': 'FULL'},
                    priority='LOW'
                )
        except Exception as e:
            logger.error(f"采集异常: {e}")

def send_heartbeat():
    """增强版心跳"""
    mem = psutil.virtual_memory()
    data = {
        'hostname': platform.node(),
        'osType': platform.system().upper(),
        'osVersion': platform.version(),
        'cpuArch': platform.machine(),
        'memoryTotal': mem.total,
        'agentVersion': '2.0.0',
    }
    comm.heartbeat(data)

def main():
    global logger, comm, collectors, incremental, traffic_collector, matcher

    load_config()
    logger = setup_logger()
    logger.info("=" * 50)
    logger.info("XDR Agent Phase 2 启动中...")

    comm = CommManager()
    incremental = IncrementalManager()
    
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

    # 4. 启动定时任务
    scheduler = BlockingScheduler()
    scheduler.add_job(collect_and_report, 'interval', seconds=60, id='collect')
    scheduler.add_job(send_heartbeat, 'interval', seconds=300, id='heartbeat')

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
