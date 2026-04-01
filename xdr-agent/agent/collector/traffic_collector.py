from scapy.all import sniff, IP, UDP, DNS, DNSQR
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
        self.dns_queries = []  # []
        self.lock = threading.Lock()
        self._stop_event = threading.Event()
        self._sniffer_thread = None

    def _packet_callback(self, pkt):
        if IP in pkt:
            src = pkt[IP].src
            dst = pkt[IP].dst
            proto = pkt[IP].proto
            sport = pkt.sport if hasattr(pkt, 'sport') else 0
            dport = pkt.dport if hasattr(pkt, 'dport') else 0
            
            # DNS 检测
            if pkt.haslayer(DNSQR) and pkt.haslayer(UDP) and dport == 53:
                qname = pkt[DNSQR].qname
                qname_str = qname.decode('utf-8', errors='ignore') if isinstance(qname, bytes) else str(qname)
                if qname_str.endswith('.'): qname_str = qname_str[:-1]
                with self.lock:
                    if len(self.dns_queries) < 100:
                        self.dns_queries.append({
                            "querydnsname": qname_str,
                            "timestamp": datetime.now().isoformat()
                        })
            
            key = (src, sport, dst, dport, proto)
            with self.lock:
                self.connections[key] = self.connections.get(key, 0) + 1

    def start_sniffing(self, iface=None):
        def run():
            # promisc=True 开启混杂模式
            sniff(iface=iface, prn=self._packet_callback, store=0, promisc=True, stop_filter=lambda x: self._stop_event.is_set())
        
        self._sniffer_thread = threading.Thread(target=run, daemon=True)
        self._sniffer_thread.start()

    def collect(self):
        now_ts = datetime.now().isoformat()
        with self.lock:
            snapshot = []
            for (src, sport, dst, dport, proto), count in self.connections.items():
                snapshot.append({
                    "srcIp": src,
                    "srcPort": sport,
                    "dstIp": dst,
                    "dstPort": dport,
                    "protocol": proto,
                    "count": count,
                    "timestamp": now_ts
                })
            # 重置计数，保证每次上报增量或近期活跃连接
            self.connections.clear()
            
            dns_snapshot = self.dns_queries[:]
            self.dns_queries.clear()
            
        return {
            "connections": snapshot,
            "dns": dns_snapshot,
            "timestamp": now_ts
        }

    def stop(self):
        self._stop_event.set()
