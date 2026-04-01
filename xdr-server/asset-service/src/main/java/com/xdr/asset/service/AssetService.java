package com.xdr.asset.service;

import com.fasterxml.jackson.core.type.TypeReference;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xdr.asset.dto.AssetDetailDTO;
import com.xdr.asset.mapper.AssetMapper;
import com.xdr.asset.mapper.AssetUserMapper;
import com.xdr.asset.model.Asset;
import com.xdr.asset.model.AssetUser;
import com.xdr.common.dto.PageResponse;
import com.xdr.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetMapper assetMapper;
    private final AssetUserMapper assetUserMapper;
    private final com.xdr.asset.service.HostAssetRecordService hostAssetRecordService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private final com.xdr.asset.client.ThreatServiceClient threatServiceClient;
    private final com.xdr.asset.client.BaselineServiceClient baselineServiceClient;
    private final org.springframework.web.client.RestTemplate restTemplate;

    private static final String POLICY_SERVICE_URL = "http://localhost:8085";

    /**
     * S-ASSET-001: 资产自动注册/更新 (心跳时调用)
     */
    public Asset registerOrUpdate(String agentId, Map<String, Object> assetData) {
        Asset asset = assetMapper.selectOne(
                new LambdaQueryWrapper<Asset>().eq(Asset::getAgentId, agentId));

        if (asset == null) {
            asset = new Asset();
            asset.setAgentId(agentId);
        }

        asset.setHostname((String) assetData.getOrDefault("hostname", asset.getHostname()));
        asset.setOsType((String) assetData.getOrDefault("osType", asset.getOsType()));
        asset.setOsVersion((String) assetData.getOrDefault("osVersion", asset.getOsVersion()));
        asset.setCpuArch((String) assetData.getOrDefault("cpuArch", asset.getCpuArch()));
        asset.setCpuModel((String) assetData.getOrDefault("cpuModel", asset.getCpuModel()));
        asset.setAgentVersion((String) assetData.getOrDefault("agentVersion", asset.getAgentVersion()));
        asset.setIpAddress((String) assetData.getOrDefault("ipAddress", asset.getIpAddress()));
        asset.setMacAddress((String) assetData.getOrDefault("macAddress", asset.getMacAddress()));

        if (assetData.containsKey("memoryTotal")) {
            asset.setMemoryTotal(((Number) assetData.get("memoryTotal")).longValue());
        }
        if (assetData.containsKey("diskTotal")) {
            asset.setDiskTotal(((Number) assetData.get("diskTotal")).longValue());
        }

        asset.setStatus("ONLINE");
        asset.setLastHeartbeat(LocalDateTime.now());

        // 动态计算安全健康分 (Phase 16 核心)
        asset.setRiskScore(calculateHealthScore(agentId));

        // 补齐权属信息：如果 AssetUser 表已有数据，则同步过来 (解决注册顺序导致的缺失问题)
        AssetUser userInfo = assetUserMapper.selectOne(new LambdaQueryWrapper<AssetUser>().eq(AssetUser::getAgentId, agentId));
        if (userInfo != null) {
            asset.setUnitLevel1(userInfo.getUnitLevel1());
            asset.setUnitLevel2(userInfo.getUnitLevel2());
            asset.setDepartment(userInfo.getDepartment());
            asset.setResponsiblePerson(userInfo.getUsername());
        }

        if (asset.getId() == null) {
            assetMapper.insert(asset);
        } else {
            assetMapper.updateById(asset);
        }
        return asset;
    }

    /**
     * S-ASSET-012: 资产动态评分模型
     * 健康分 = 100 - (告警权重分 + 基线偏离分 + 下线惩罚)
     */
    private int calculateHealthScore(String agentId) {
        int score = 100;

        // 1. 告警扣分
        try {
            List<Map<String, Object>> alerts = threatServiceClient.getActiveAlerts(agentId);
            for (Map<String, Object> alert : alerts) {
                String level = (String) alert.get("level");
                score -= switch (level) {
                    case "CRITICAL" -> 40;
                    case "HIGH" -> 20;
                    case "MEDIUM" -> 10;
                    case "LOW" -> 5;
                    default -> 0;
                };
            }
        } catch (Exception e) {
            /* fallback */ }

        // 2. 基线偏离扣分
        try {
            Map<String, Object> baselineStats = baselineServiceClient.getBaselineStatus(agentId);
            if (baselineStats != null && baselineStats.containsKey("details")) {
                @SuppressWarnings("unchecked")
                Map<String, Integer> details = (Map<String, Integer>) baselineStats.get("details");
                for (Integer count : details.values()) {
                    if (count > 0) {
                        score -= 15; // 每个存在偏离的基线类型扣15分
                    }
                }
            }
        } catch (Exception e) {
            /* fallback */ }

        return Math.max(0, score);
    }

    /**
     * S-ASSET-004: 资产搜索筛选(分页)
     */
    public PageResponse<Asset> listAssets(int page, int size, String keyword,
            String osType, String status, String groupId, String unitLevel1, String unitLevel2,
            String responsiblePerson) {
        LambdaQueryWrapper<Asset> query = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            query.and(q -> q
                    .like(Asset::getHostname, keyword)
                    .or().like(Asset::getIpAddress, keyword)
                    .or().like(Asset::getAgentId, keyword));
        }
        if (StringUtils.hasText(osType)) {
            query.eq(Asset::getOsType, osType);
        }
        if (StringUtils.hasText(status)) {
            query.eq(Asset::getStatus, status);
        }
        if (StringUtils.hasText(groupId)) {
            query.eq(Asset::getGroupId, groupId);
        }
        if (StringUtils.hasText(unitLevel1)) {
            query.eq(Asset::getUnitLevel1, unitLevel1);
        }
        if (StringUtils.hasText(unitLevel2)) {
            query.eq(Asset::getUnitLevel2, unitLevel2);
        }
        if (StringUtils.hasText(responsiblePerson)) {
            query.eq(Asset::getResponsiblePerson, responsiblePerson);
        }

        query.orderByDesc(Asset::getLastHeartbeat);

        Page<Asset> p = assetMapper.selectPage(new Page<>(page, size), query);
        return PageResponse.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    /**
     * S-ASSET-002: 资产详情
     */
    public Asset getAssetDetail(String id) {
        Asset asset = assetMapper.selectById(id);
        if (asset == null)
            throw new BusinessException("资产不存在");
        return asset;
    }

    /**
     * S-ASSET-006: 心跳离线检测 - 5分钟无心跳标记OFFLINE
     */
    @Scheduled(fixedRate = 60000) // 每分钟检查
    public void checkHeartbeat() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        List<Asset> onlineAssets = assetMapper.selectList(
                new LambdaQueryWrapper<Asset>()
                        .eq(Asset::getStatus, "ONLINE")
                        .lt(Asset::getLastHeartbeat, threshold));
        for (Asset asset : onlineAssets) {
            asset.setStatus("OFFLINE");
            assetMapper.updateById(asset);
        }
    }

    /**
     * S-ASSET-008: 保存/更新资产用户信息 (联动同步至资产表)
     */
    public void saveAssetUser(String agentId, AssetUser info) {
        AssetUser existing = assetUserMapper.selectOne(
                new LambdaQueryWrapper<AssetUser>().eq(AssetUser::getAgentId, agentId));
        info.setAgentId(agentId);
        if (existing == null) {
            assetUserMapper.insert(info);
        } else {
            info.setId(existing.getId());
            assetUserMapper.updateById(info);
        }

        // 联动同步至 Asset 表，确保列表页数据准确 (冗余存储以换取查询性能)
        Asset asset = assetMapper.selectOne(new LambdaQueryWrapper<Asset>().eq(Asset::getAgentId, agentId));
        if (asset != null) {
            asset.setUnitLevel1(info.getUnitLevel1());
            asset.setUnitLevel2(info.getUnitLevel2());
            asset.setDepartment(info.getDepartment());
            asset.setResponsiblePerson(info.getUsername());
            assetMapper.updateById(asset);
        }
    }

    /** 统计数据 (仪表盘用) */
    public Map<String, Object> getStats() {
        long total = assetMapper.selectCount(null);
        long online = assetMapper.selectCount(
                new LambdaQueryWrapper<Asset>().eq(Asset::getStatus, "ONLINE"));
        long offline = total - online;
        return Map.of("total", total, "online", online, "offline", offline);
    }

    /**
     * S-ASSET-002: 资产聚合详情 (Phase 2, 6, 11 & 15)
     */
    public AssetDetailDTO getAggregatedAssetDetail(String agentId, java.time.LocalDateTime startTime,
            java.time.LocalDateTime endTime) {
        Asset asset = assetMapper.selectOne(new LambdaQueryWrapper<Asset>().eq(Asset::getAgentId, agentId));
        if (asset == null)
            throw new BusinessException("资产不存在");

        AssetDetailDTO detail = new AssetDetailDTO();
        detail.setBaseInfo(asset);

        // 获取用户信息
        detail.setAssetUser(assetUserMapper.selectOne(
                new LambdaQueryWrapper<AssetUser>().eq(AssetUser::getAgentId, agentId)));

        // 初始化列表
        detail.setProcesses(new ArrayList<>());
        detail.setPorts(new ArrayList<>());
        detail.setSoftwares(new ArrayList<>());
        detail.setUsbDevices(new ArrayList<>());
        detail.setLogins(new ArrayList<>());
        detail.setTraffic(new ArrayList<>());
        detail.setDnsQueries(new ArrayList<>());

        // 1. 获取基础库存快照 (PROCESS, NETWORK, SOFTWARE)
        List<com.xdr.asset.model.HostAssetRecord> inventoryRecords = hostAssetRecordService.getCurrentSnapshot(agentId);
        for (com.xdr.asset.model.HostAssetRecord record : inventoryRecords) {
            String type = record.getAssetType();
            if ("USB".equals(type) || "LOGIN".equals(type) || "TRAFFIC".equals(type)) {
                continue; // Skip event assets here, they will be handled separately
            }
            try {
                Map<String, Object> data = objectMapper.readValue(record.getAssetData(), new TypeReference<Map<String, Object>>() {});
                switch (type) {
                    case "PROCESS" -> detail.getProcesses().add(data);
                    case "NETWORK" -> detail.getPorts().add(data);
                    case "SOFTWARE" -> detail.getSoftwares().add(data);
                }
            } catch (Exception e) {
                /* ignore */ }
        }

        // 2. 获取事件类资产的历史记录 (USB, LOGIN, TRAFFIC) - 如果有时间范围则全量取，否则取最近 50 条
        populateEventAssets(agentId, "USB", detail.getUsbDevices(), startTime, endTime);
        populateEventAssets(agentId, "LOGIN", detail.getLogins(), startTime, endTime);
        populateEventAssets(agentId, "TRAFFIC", detail.getTraffic(), startTime, endTime);
        populateEventAssets(agentId, "DNS", detail.getDnsQueries(), startTime, endTime);
        
        // 3. 风险聚合数据 (侧边栏动态化)
        try {
            List<Map<String, Object>> alerts = threatServiceClient.getActiveAlerts(agentId);
            detail.setActiveAlerts(alerts != null ? alerts.size() : 0);
            
            Map<String, Object> baselineStats = baselineServiceClient.getBaselineStatus(agentId);
            if (baselineStats != null && baselineStats.containsKey("details")) {
                Map<String, Integer> baselineDetails = objectMapper.convertValue(
                    baselineStats.get("details"), 
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Integer>>() {}
                );
                int deviations = 0;
                for (Integer count : baselineDetails.values()) {
                    if (count != null) deviations += count;
                }
                detail.setBaselineDeviations(deviations);
            } else {
                detail.setBaselineDeviations(0);
            }
        } catch (Exception e) {
            detail.setActiveAlerts(0);
            detail.setBaselineDeviations(0);
        }

        return detail;
    }

    private void populateEventAssets(String agentId, String type, List<Map<String, Object>> targetList,
            java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        List<com.xdr.asset.model.HostAssetRecord> eventRecords;
        if (startTime != null && endTime != null) {
            // DB-side filtering by type
            eventRecords = hostAssetRecordService.findHistoryByQuery(agentId, type, startTime, endTime);
        } else {
            eventRecords = hostAssetRecordService.getRecentEvents(agentId, type, 50);
        }

        for (com.xdr.asset.model.HostAssetRecord record : eventRecords) {
            try {
                Map<String, Object> data = objectMapper.readValue(record.getAssetData(), new TypeReference<Map<String, Object>>() {});
                targetList.add(data);
            } catch (Exception e) {
                /* ignore */ }
        }
    }

    public List<com.xdr.asset.model.HostAssetRecord> getTimelineSnapshot(String agentId, LocalDateTime timestamp) {
        if (timestamp == null) {
            return hostAssetRecordService.getCurrentSnapshot(agentId);
        }
        return hostAssetRecordService.getTimelineSnapshot(agentId, timestamp);
    }

    public List<com.xdr.asset.model.HostAssetRecord> getHistoryRecords(String agentId, String assetType,
            LocalDateTime startTime, LocalDateTime endTime) {
        return hostAssetRecordService.findHistoryByQuery(agentId, assetType, startTime, endTime);
    }

    /** 获取入侵排查报告历史 */
    public List<Map<String, Object>> getIntrusionReports(String agentId) {
        List<com.xdr.asset.model.HostAssetRecord> records = hostAssetRecordService.getRecentEvents(agentId, "INTRUSION_REPORT", 10);
        List<Map<String, Object>> reports = new ArrayList<>();
        for (com.xdr.asset.model.HostAssetRecord record : records) {
            try {
                Map<String, Object> data = objectMapper.readValue(record.getAssetData(), new TypeReference<Map<String, Object>>() {});
                reports.add(data);
            } catch (Exception e) { /* ignore */ }
        }
        return reports;
    }

    /** 触发深度取证扫描指令 */
    public void triggerForensics(String agentId) {
        Map<String, Object> command = new java.util.HashMap<>();
        command.put("agentId", agentId);
        command.put("commandType", "INTRUSION_SCAN");
        command.put("commandData", "{}");

        try {
            restTemplate.postForEntity(POLICY_SERVICE_URL + "/api/v1/policies/commands", command, Void.class);
        } catch (Exception e) {
            throw new BusinessException("下发深度取证指令失败");
        }
    }
}
