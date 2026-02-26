"""
XDR Agent 核心配置管理
"""
import os
import yaml

_config = None
_config_path = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), 'config.yaml')


def load_config(path: str = None) -> dict:
    global _config
    config_file = path or _config_path
    with open(config_file, 'r', encoding='utf-8') as f:
        _config = yaml.safe_load(f)
    return _config


def get_config() -> dict:
    if _config is None:
        load_config()
    return _config


def save_config(config: dict = None, path: str = None):
    cfg = config or _config
    config_file = path or _config_path
    with open(config_file, 'w', encoding='utf-8') as f:
        yaml.dump(cfg, f, default_flow_style=False, allow_unicode=True)


def get_server_url() -> str:
    return get_config()['agent']['server_url']


def get_agent_id() -> str:
    return get_config()['agent'].get('agent_id', '')


def set_agent_id(agent_id: str):
    get_config()['agent']['agent_id'] = agent_id
    save_config()
