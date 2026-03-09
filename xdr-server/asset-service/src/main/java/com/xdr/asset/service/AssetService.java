package com.xdr.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xdr.asset.dto.AssetDetailDTO;
import com.xdr.asset.mapper.AssetMapper;
import com.xdr.asset.mapper.UserInfoMapper;
import com.xdr.asset.model.Asset;
import com.xdr.asset.model.UserInfo;
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
    private final UserInfoMapper userInfoMapper;
    private final com.xdr.asset.service.HostAssetRecordService hostAssetRecordService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

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

        if (asset.getId() == null) {
            assetMapper.insert(asset);
        } else {
            assetMapper.updateById(asset);
        }
        return asset;
    }

    /**
     * S-ASSET-004: 资产搜索筛选(分页)
     */
    public PageResponse<Asset> listAssets(int page, int size, String keyword,
            String osType, String status, String groupId, String unit, String responsiblePerson) {
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
        if (StringUtils.hasText(unit)) {
            query.eq(Asset::getUnit, unit);
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
     * S-ASSET-008: 保存/更新用户信息
     */
    public void saveUserInfo(String agentId, UserInfo info) {
        UserInfo existing = userInfoMapper.selectOne(
                new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getAgentId, agentId));
        info.setAgentId(agentId);
        if (existing == null) {
            userInfoMapper.insert(info);
        } else {
            info.setId(existing.getId());
            userInfoMapper.updateById(info);
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
        detail.setUserInfo(userInfoMapper.selectOne(
                new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getAgentId, agentId)));

        // 初始化列表
        detail.setProcesses(new ArrayList<>());
        detail.setPorts(new ArrayList<>());
        detail.setSoftwares(new ArrayList<>());
        detail.setUsbDevices(new ArrayList<>());
        detail.setLogins(new ArrayList<>());
        detail.setTraffic(new ArrayList<>());

        // 1. 获取基础库存快照 (PROCESS, NETWORK, SOFTWARE)
        List<com.xdr.asset.model.HostAssetRecord> inventoryRecords = hostAssetRecordService.getCurrentSnapshot(agentId);
        for (com.xdr.asset.model.HostAssetRecord record : inventoryRecords) {
            String type = record.getAssetType();
            if ("USB".equals(type) || "LOGIN".equals(type) || "TRAFFIC".equals(type)) {
                continue; // Skip event assets here, they will be handled separately
            }
            try {
                Map<String, Object> data = objectMapper.readValue(record.getAssetData(), Map.class);
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

        return detail;
    }

    private void populateEventAssets(String agentId, String type, List<Map<String, Object>> targetList,
            java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        List<com.xdr.asset.model.HostAssetRecord> eventRecords;
        if (startTime != null && endTime != null) {
            // DB-side filtering by type
            eventRecords = hostAssetRecordService.getHistoryRecords(agentId, type, startTime, endTime);
        } else {
            eventRecords = hostAssetRecordService.getRecentEvents(agentId, type, 50);
        }

        for (com.xdr.asset.model.HostAssetRecord record : eventRecords) {
            try {
                Map<String, Object> data = objectMapper.readValue(record.getAssetData(), Map.class);
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

    public List<com.xdr.asset.model.HostAssetRecord> getHistoryRecords(String agentId, LocalDateTime startTime,
            LocalDateTime endTime) {
        return hostAssetRecordService.getHistoryRecords(agentId, startTime, endTime);
    }
}
