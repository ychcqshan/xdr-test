from scapy.all import sniff, IP
import threading
from datetime import datetime
from .base import BaseCollector

class TrafficCollector(BaseCollector):
    """
    使用 Scapy 开启网卡混杂模式采集流量元数据，辅助拓扑生成
    """
    def name(self) -> str:
        return "TRAFFIC"

    def __init__(self):
        self.connections = {}  # {(src, dst, dport): count}
        self.lock = threading.Lock()
        self._stop_event = threading.Event()
        self._sniffer_thread = None

    def _packet_callback(self, pkt):
        if IP in pkt:
            src = pkt[IP].src
            dst = pkt[IP].dst
            proto = pkt[IP].proto
            dport = pkt.dport if hasattr(pkt, 'dport') else 0
            
            key = (src, dst, dport, proto)
            with self.lock:
                self.connections[key] = self.connections.get(key, 0) + 1

    def start_sniffing(self, iface=None):
        def run():
            # promisc=True 开启混杂模式
            sniff(iface=iface, prn=self._packet_callback, store=0, promisc=True, stop_filter=lambda x: self._stop_event.is_set())
        
        self._sniffer_thread = threading.Thread(target=run, daemon=True)
        self._sniffer_thread.start()

    def collect(self):
        with self.lock:
            snapshot = []
            for (src, dst, dport, proto), count in self.connections.items():
                snapshot.append({
                    "srcIp": src,
                    "dstIp": dst,
                    "dstPort": dport,
                    "protocol": proto,
                    "count": count
                })
            # 重置计数，保证每次上报增量或近期活跃连接
            self.connections.clear()
            
        return {
            "traffic": snapshot,
            "timestamp": datetime.now().isoformat()
        }

    def stop(self):
        self._stop_event.set()
