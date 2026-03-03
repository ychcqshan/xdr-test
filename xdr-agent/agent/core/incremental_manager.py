import json
import os
import logging
from typing import List, Dict, Any, Tuple

logger = logging.getLogger('xdr-agent')

class IncrementalManager:
    """
    增量上报管理器
    负责维护本地状态缓存，并计算两次采集之间的差异（ADD/REMOVE/UPDATE）
    """
    def __init__(self, cache_dir: str = "cache"):
        self.cache_dir = cache_dir
        if not os.path.exists(self.cache_dir):
            os.makedirs(self.cache_dir)
        self.state_file = os.path.join(self.cache_dir, "incremental_state.json")
        self.last_state = self._load_state()

    def _load_state(self) -> Dict[str, Dict[str, Any]]:
        if os.path.exists(self.state_file):
            try:
                with open(self.state_file, 'r', encoding='utf-8') as f:
                    return json.load(f)
            except Exception as e:
                logger.error(f"加载增量状态失败: {e}")
        return {}

    def _save_state(self):
        try:
            with open(self.state_file, 'w', encoding='utf-8') as f:
                json.dump(self.last_state, f, ensure_ascii=False, indent=2)
        except Exception as e:
            logger.error(f"保存增量状态失败: {e}")

    def get_diff(self, category: str, current_data: List[Dict[str, Any]], key_fields: List[str]) -> Tuple[str, List[Dict[str, Any]]]:
        """
        计算差异
        :param category: 类别 (PROCESS, NETWORK等)
        :param current_data: 当前采集到的数据列表
        :param key_fields: 用于生成唯一键的字段列表
        :return: (report_type, diff_items)
        """
        # 生成当前快照字典
        current_map = {}
        for item in current_data:
            key = "|".join([str(item.get(f, "")) for f in key_fields])
            current_map[key] = item

        # 获取历史快照
        category_state = self.last_state.get(category)
        
        # 如果没有历史快照，或者是第一次运行，返回全量
        if not category_state:
            self.last_state[category] = current_map
            self._save_state()
            logger.info(f"[{category}] 首次上报，执行 FULL 模式, 项数: {len(current_data)}")
            return "FULL", current_data

        history_map = category_state
        diff_items = []

        # 1. 查找新增 (ADD)
        for key, item in current_map.items():
            if key not in history_map:
                item_copy = item.copy()
                item_copy['action'] = 'ADD'
                diff_items.append(item_copy)
            else:
                # 2. 检查是否有显著更新 (UPDATE) - 目前只对比整体，后续可降精
                # 排除可能经常变动的字段如 cpuPercent, memoryPercent
                h_item = history_map[key]
                significant_change = False
                for k, v in item.items():
                    if k in ['cpuPercent', 'memoryPercent']: continue
                    if v != h_item.get(k):
                        significant_change = True
                        break
                
                if significant_change:
                    item_copy = item.copy()
                    item_copy['action'] = 'UPDATE'
                    diff_items.append(item_copy)

        # 3. 查找删除 (REMOVE)
        for key, item in history_map.items():
            if key not in current_map:
                item_copy = item.copy()
                item_copy['action'] = 'REMOVE'
                diff_items.append(item_copy)

        # 更新状态
        self.last_state[category] = current_map
        self._save_state()

        if not diff_items:
            logger.debug(f"[{category}] 无变化，跳过上报")
            return "NONE", []
            
        logger.info(f"[{category}] 计算由于增量变化: {len(diff_items)} 项, 执行 INCREMENTAL 模式")
        return "INCREMENTAL", diff_items

    def force_full_sync(self, category: str):
        """强制下次进行全量同步"""
        if category in self.last_state:
            del self.last_state[category]
            self._save_state()
