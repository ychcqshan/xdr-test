package com.xdr.asset.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xdr.asset.dto.GraphDTO;
import com.xdr.asset.model.Asset;
import com.xdr.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopologyService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AssetService assetService;

    private static final String THREAT_SERVICE_URL = "http://localhost:8084";

    public GraphDTO getNetworkTopology() {
        GraphDTO graph = new GraphDTO();
        List<GraphDTO.Node> nodes = new ArrayList<>();
        List<GraphDTO.Edge> edges = new ArrayList<>();
        Set<String> nodeIds = new HashSet<>();
        Set<String> edgeIds = new HashSet<>();

        // 1. 获取所有Agent的基础IP信息（作为核心节点）
        // 这里简化处理：从 asset-service 获取所有在线/离线资产
        // 实际中可能需要分页或全量，Phase 2 先做全量演示
        List<Asset> assets = assetService.listAssets(1, 1000, null, null, null, null, null, null).getRecords();
        for (Asset asset : assets) {
            String id = asset.getIpAddress();
            if (id != null && nodeIds.add(id)) {
                nodes.add(new GraphDTO.Node(id, asset.getHostname() != null ? asset.getHostname() : id, "ASSET"));
            }

            // 2. 获取该Agent的外部连接并构建边
            try {
                ApiResponse<List<Map<String, Object>>> response = restTemplate.exchange(
                        THREAT_SERVICE_URL + "/api/v1/host-assets/" + asset.getAgentId(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ApiResponse<List<Map<String, Object>>>>() {
                        }).getBody();

                if (response != null && response.getData() != null) {
                    for (Map<String, Object> hostAsset : response.getData()) {
                        String dataJson = (String) hostAsset.get("assetData");
                        Map<String, Object> data = objectMapper.readValue(dataJson, Map.class);

                        if ("NETWORK".equals(hostAsset.get("assetType"))) {
                            // 处理监听/活跃端口 (TCP)
                            String localAddr = (String) data.get("localAddr");
                            String remoteAddr = (String) data.get("remoteAddr");

                            if (localAddr != null && remoteAddr != null) {
                                String srcIp = localAddr.split(":")[0];
                                String[] remoteParts = remoteAddr.split(":");
                                String dstIp = remoteParts[0];
                                String dstPort = remoteParts.length > 1 ? remoteParts[1] : "";
                                addNodeAndEdge(nodes, edges, nodeIds, edgeIds, srcIp, dstIp, dstPort);
                            }
                        } else if ("TRAFFIC".equals(hostAsset.get("assetType"))) {
                            // 处理混杂模式采集的流量快照 (Packet Sniffing)
                            String srcIp = (String) data.get("srcIp");
                            String dstIp = (String) data.get("dstIp");
                            String dstPort = String.valueOf(data.getOrDefault("dstPort", ""));
                            addNodeAndEdge(nodes, edges, nodeIds, edgeIds, srcIp, dstIp, dstPort);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("计算Agent {} 拓扑失败: {}", asset.getAgentId(), e.getMessage());
            }
        }

        graph.setNodes(nodes);
        graph.setEdges(edges);
        return graph;
    }

    private void addNodeAndEdge(List<GraphDTO.Node> nodes, List<GraphDTO.Edge> edges, Set<String> nodeIds,
            Set<String> edgeIds, String srcIp, String dstIp, String dstPort) {
        // 忽略回环地址
        if (dstIp == null || "127.0.0.1".equals(dstIp) || "0.0.0.0".equals(dstIp) || "::1".equals(dstIp))
            return;

        // 添加目的节点（如果是外部IP或新发现节点）
        if (nodeIds.add(dstIp)) {
            nodes.add(new GraphDTO.Node(dstIp, dstIp, "EXTERNAL"));
        }

        // 添加边
        String edgeId = srcIp + "->" + dstIp + ":" + (dstPort != null ? dstPort : "");
        if (edgeIds.add(edgeId)) {
            edges.add(new GraphDTO.Edge(srcIp, dstIp, dstPort));
        }
    }
}
