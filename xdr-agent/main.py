"""
XDR Agent 主入口
"""
import os
import platform
import logging

import psutil
from apscheduler.schedulers.blocking import BlockingScheduler

from agent.core.config import load_config, get_config, get_agent_id, set_agent_id
from agent.core.logger import setup_logger
from agent.collector import ProcessCollector, NetworkCollector, LoginCollector
from agent.comm.comm_manager import CommManager
from agent.comparator.baseline_comparator import BaselineComparator

logger: logging.Logger = None
comm: CommManager = None
comparator: BaselineComparator = None

# 采集器实例
collectors = []


def register_agent():
    """首次运行时注册Agent"""
    if get_agent_id():
        logger.info(f"Agent已注册: {get_agent_id()}")
        return

    logger.info("首次运行，注册Agent...")
    data = {
        'hostname': platform.node(),
        'osType': 'WINDOWS' if platform.system() == 'Windows' else platform.system().upper(),
        'cpuArch': platform.machine(),
        'macAddress': '',
        'agentVersion': '1.0.0',
    }
    result = comm.post('/api/v1/auth/agent/register', data, compress=False)
    if result and result.get('data'):
        agent_id = result['data']['agentId']
        set_agent_id(agent_id)
        logger.info(f"Agent注册成功: {agent_id}")
    else:
        logger.error("Agent注册失败")


def collect_and_report():
    """执行一次采集并上报"""
    for collector in collectors:
        try:
            data = collector.collect()
            if data:
                comm.report_event(
                    event_type=collector.name(),
                    event_data={'items': data, 'count': len(data)},
                    priority='LOW'
                )
        except Exception as e:
            logger.error(f"{collector.name()}采集异常: {e}")


def send_heartbeat():
    """心跳上报"""
    import shutil
    mem = psutil.virtual_memory()
    try:
        if platform.system() == 'Windows':
            disk_path = psutil.disk_partitions()[0].mountpoint
        else:
            disk_path = '/'
        disk_total = shutil.disk_usage(disk_path).total
    except Exception as e:
        logger.error(f"无法获取磁盘大小: {e}")
        disk_total = 0
    data = {
        'hostname': platform.node(),
        'osType': 'WINDOWS' if platform.system() == 'Windows' else platform.system().upper(),
        'osVersion': platform.version(),
        'cpuArch': platform.machine(),
        'cpuModel': platform.processor(),
        'memoryTotal': mem.total,
        'diskTotal': disk_total,
        'agentVersion': '1.0.0',
        'ipAddress': '',
    }
    comm.heartbeat(data)


def try_flush_cache():
    """尝试补传缓存数据"""
    comm.flush_cache()


def main():
    global logger, comm, comparator, collectors

    # 初始化
    load_config()
    logger = setup_logger()
    logger.info("=" * 50)
    logger.info("XDR Agent 启动中...")
    logger.info(f"平台: {platform.system()} {platform.release()} ({platform.machine()})")

    # 初始化通信
    comm = CommManager()

    # 注册Agent
    register_agent()

    # 初始化采集器
    collectors = [
        ProcessCollector(),
        NetworkCollector(),
        LoginCollector(),
    ]

    # 初始化基线比对器
    comparator = BaselineComparator()

    # 初始化用户信息管理
    from agent.user_info.manager import UserInfoManager
    user_manager = UserInfoManager(comm)
    user_manager.check_and_collect()

    # 获取配置
    config = get_config()
    collect_interval = config.get('collector', {}).get('process_interval', 60)
    heartbeat_interval = config.get('heartbeat', {}).get('interval', 300)

    # 启动定时任务
    scheduler = BlockingScheduler()
    scheduler.add_job(collect_and_report, 'interval', seconds=collect_interval, id='collect')
    scheduler.add_job(send_heartbeat, 'interval', seconds=heartbeat_interval, id='heartbeat')
    scheduler.add_job(try_flush_cache, 'interval', seconds=600, id='flush_cache')

    # 立即执行一次
    send_heartbeat()
    collect_and_report()

    logger.info(f"定时任务已启动: 采集间隔{collect_interval}s, 心跳间隔{heartbeat_interval}s")
    logger.info("XDR Agent 运行中...")

    try:
        scheduler.start()
    except (KeyboardInterrupt, SystemExit):
        logger.info("XDR Agent 正在停止...")


if __name__ == '__main__':
    main()
