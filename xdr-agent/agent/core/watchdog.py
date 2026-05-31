import threading
import time
import logging
import os
import hashlib

logger = logging.getLogger('xdr-agent')

class AgentWatchdog(threading.Thread):
    """
    轻量级进程守护线程 - 实现有限自保护计划 (Phase 4.7)
    """
    def __init__(self, config_path, check_interval=30):
        super().__init__(name="WatchdogThread", daemon=True)
        self.config_path = config_path
        self.check_interval = check_interval
        self.running = True
        self._config_hash = self._get_file_hash(config_path)
        self._health_check_registry = {}

    def run(self):
        logger.info("Watchdog: 守护线程已启动")
        while self.running:
            try:
                # 1. 检查关键配置文件完整性
                current_hash = self._get_file_hash(self.config_path)
                if current_hash != self._config_hash:
                    logger.warning(f"Watchdog: 检测到配置文件 {self.config_path} 被篡改！尝试锁定或告警...")
                    # 实际环境中可在此处触发还原逻辑
                
                # 2. 检查注册模块的健康度 (基于心跳时间戳)
                now = time.time()
                for name, last_seen in self._health_check_registry.items():
                    if now - last_seen > self.check_interval * 3:
                        logger.error(f"Watchdog: 模块 [{name}] 疑似僵死 (最后活跃: {time.ctime(last_seen)})")
                
                # 3. 资源水位简易监控
                import psutil
                process = psutil.Process()
                mem_mb = process.memory_info().rss / 1024 / 1024
                if mem_mb > 500: # 500MB 阈值
                    logger.warning(f"Watchdog: Agent 内存占用过高 ({mem_mb:.2f}MB)，可能存在泄漏")

            except Exception as e:
                logger.error(f"Watchdog: 内部异常: {e}")
            
            time.sleep(self.check_interval)

    def heartbeat(self, module_name):
        """由各模块调用以表明存活"""
        self._health_check_registry[module_name] = time.time()

    def stop(self):
        self.running = False

    def _get_file_hash(self, path):
        if not os.path.exists(path): return ""
        try:
            with open(path, "rb") as f:
                return hashlib.md5(f.read()).hexdigest()
        except: return ""

    def update_baseline(self):
        """更新配置基线Hash（当由Agent自行修改配置时调用）"""
        self._config_hash = self._get_file_hash(self.config_path)
