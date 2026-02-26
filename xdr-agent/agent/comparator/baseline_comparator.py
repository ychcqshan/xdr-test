"""
A-BL-001~007: 基线比对器
"""
import logging
from typing import List, Dict, Any, Tuple

logger = logging.getLogger('xdr-agent')


class BaselineComparator:
    """本地基线比对引擎"""

    def __init__(self):
        self.baselines: Dict[str, dict] = {}  # type -> {items: {key: data}}

    def load_baseline(self, baseline_type: str, items: List[dict], key_builder=None):
        """加载基线数据"""
        if key_builder is None:
            key_builder = self._default_key_builder(baseline_type)

        baseline_map = {}
        for item in items:
            key = key_builder(item)
            baseline_map[key] = item

        self.baselines[baseline_type] = {'items': baseline_map, 'key_builder': key_builder}
        logger.info(f"加载{baseline_type}基线, 共{len(baseline_map)}项")

    def compare(self, baseline_type: str, current_data: List[dict]) -> dict:
        """
        执行比对，返回差异结果
        A-BL-002~006: 各类型基线比对
        """
        if baseline_type not in self.baselines:
            logger.warning(f"未加载{baseline_type}基线，跳过比对")
            return {'added': [], 'removed': [], 'modified': [], 'totalDiff': 0}

        baseline = self.baselines[baseline_type]
        key_builder = baseline['key_builder']
        baseline_map = baseline['items']

        current_map = {}
        for item in current_data:
            key = key_builder(item)
            current_map[key] = item

        added = []     # 新增：当前有，基线无
        removed = []   # 缺失：基线有，当前无
        modified = []  # 修改：都有但不同

        for key, data in current_map.items():
            if key not in baseline_map:
                added.append(data)

        for key, data in baseline_map.items():
            if key not in current_map:
                removed.append(data)
            else:
                if data != current_map[key]:
                    modified.append({
                        'baseline': data,
                        'current': current_map[key]
                    })

        total = len(added) + len(removed) + len(modified)
        result = {
            'type': baseline_type,
            'added': added,
            'removed': removed,
            'modified': modified,
            'totalDiff': total,
        }

        if total > 0:
            logger.warning(f"{baseline_type}基线偏差: 新增{len(added)}, 缺失{len(removed)}, 修改{len(modified)}")

        return result

    @staticmethod
    def _default_key_builder(baseline_type: str):
        """根据基线类型返回默认的键构造函数"""
        def builder(item: dict) -> str:
            match baseline_type:
                case 'PROCESS':
                    return f"{item.get('name', '')}|{item.get('path', '')}"
                case 'PORT':
                    return f"{item.get('port', '')}|{item.get('protocol', '')}"
                case 'USB':
                    return str(item.get('serialNumber', ''))
                case 'LOGIN':
                    return f"{item.get('username', '')}|{item.get('loginType', '')}"
                case 'SOFTWARE':
                    return f"{item.get('name', '')}|{item.get('publisher', '')}"
                case _:
                    return str(hash(frozenset(item.items())))
        return builder
