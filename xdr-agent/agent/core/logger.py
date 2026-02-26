"""
XDR Agent 日志管理
"""
import logging
import os
from logging.handlers import RotatingFileHandler

from agent.core.config import get_config


def setup_logger(name: str = 'xdr-agent') -> logging.Logger:
    config = get_config()
    log_cfg = config.get('logging', {})

    logger = logging.getLogger(name)
    logger.setLevel(getattr(logging, log_cfg.get('level', 'INFO')))

    formatter = logging.Formatter(
        '%(asctime)s [%(levelname)s] %(name)s - %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S'
    )

    # 控制台
    console = logging.StreamHandler()
    console.setFormatter(formatter)
    logger.addHandler(console)

    # 文件
    log_file = log_cfg.get('file', 'logs/agent.log')
    os.makedirs(os.path.dirname(log_file), exist_ok=True)
    file_handler = RotatingFileHandler(
        log_file,
        maxBytes=log_cfg.get('max_bytes', 10 * 1024 * 1024),
        backupCount=log_cfg.get('backup_count', 5),
        encoding='utf-8'
    )
    file_handler.setFormatter(formatter)
    logger.addHandler(file_handler)

    return logger
