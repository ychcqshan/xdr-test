package com.xdr.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xdr.asset.dto.SyncAssetRequest;
import com.xdr.asset.mapper.HostAssetRecordMapper;
import com.xdr.asset.model.HostAssetRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HostAssetRecordService {

    private final HostAssetRecordMapper hostAssetRecordMapper;
    private final ObjectMapper objectMapper;

    private boolean isEventAsset(String type) {
        return "USB".equals(type) || "LOGIN".equals(type) || "TRAFFIC".equals(type);
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncAssets(SyncAssetRequest request) {
        String agentId = request.getAgentId();
        String type = request.getAssetType();
        String reportType = request.getReportType(); // FULL or INCREMENTAL
        List<Map<String, Object>> items = request.getItems();

        if (items == null || items.isEmpty()) {
            // Even if empty, a FULL report might mean current state is empty
            if ("FULL".equals(reportType) && !isEventAsset(type)) {
                hostAssetRecordMapper.markInactiveBefore(agentId, type, LocalDateTime.now());
            }
            return;
        }

        LocalDateTime syncTime = LocalDateTime.now();

        if ("FULL".equals(reportType)) {
            log.info("Processing FULL asset sync for agent: {}, type: {}, count: {}", agentId, type, items.size());
            // 1. Process all items as current active state
            for (Map<String, Object> item : items) {
                upsertAsset(agentId, type, item, syncTime);
            }
            // 2. Batch mark others of the SAME TYPE as INACTIVE that weren't updated
            // Skip deactivation for event-like assets (USB, LOGIN, TRAFFIC)
            if (!isEventAsset(type)) {
                hostAssetRecordMapper.markInactiveBefore(agentId, type, syncTime.minusSeconds(1));
            }
        } else {
            // INCREMENTAL
            for (Map<String, Object> item : items) {
                String action = (String) item.getOrDefault("action", "ADD");
                switch (action) {
                    case "REMOVE" -> {
                        // For event assets, we might want to keep the record but maybe tag it?
                        // For now, let's keep the standard REMOVE -> INACTIVE but ONLY for inventory
                        // assets.
                        // Event assets (USB, LOGIN) usually don't send REMOVE for historical records.
                        if (!isEventAsset(type)) {
                            removeAsset(agentId, type, item, syncTime);
                        }
                    }
                    case "UPDATE", "ADD" -> upsertAsset(agentId, type, item, syncTime);
                    default -> upsertAsset(agentId, type, item, syncTime);
                }
            }
        }
    }

    private void upsertAsset(String agentId, String type, Map<String, Object> item, LocalDateTime syncTime) {
        String fingerprint = buildFingerprint(type, item);
        HostAssetRecord record = hostAssetRecordMapper.selectOne(
                new LambdaQueryWrapper<HostAssetRecord>()
                        .eq(HostAssetRecord::getAgentId, agentId)
                        .eq(HostAssetRecord::getAssetType, type)
                        .eq(HostAssetRecord::getAssetFingerprint, fingerprint));

        if (record == null) {
            record = new HostAssetRecord();
            record.setAgentId(agentId);
            record.setAssetType(type);
            record.setAssetFingerprint(fingerprint);
            record.setFirstSeen(syncTime);
        }

        record.setStatus("ACTIVE");
        record.setLastUpdated(syncTime);
        try {
            record.setAssetData(objectMapper.writeValueAsString(item));
        } catch (Exception e) {
            log.error("Failed to serialize asset data", e);
        }

        if (record.getId() == null) {
            hostAssetRecordMapper.insert(record);
        } else {
            hostAssetRecordMapper.updateById(record);
        }
    }

    private void removeAsset(String agentId, String type, Map<String, Object> item, LocalDateTime syncTime) {
        String fingerprint = buildFingerprint(type, item);
        HostAssetRecord record = hostAssetRecordMapper.selectOne(
                new LambdaQueryWrapper<HostAssetRecord>()
                        .eq(HostAssetRecord::getAgentId, agentId)
                        .eq(HostAssetRecord::getAssetType, type)
                        .eq(HostAssetRecord::getAssetFingerprint, fingerprint));
        if (record != null && "ACTIVE".equals(record.getStatus())) {
            record.setStatus("INACTIVE");
            record.setLastUpdated(syncTime);
            hostAssetRecordMapper.updateById(record);
        }
    }

    private String buildFingerprint(String type, Map<String, Object> item) {
        if ("PROCESS".equals(type)) {
            return item.get("pid") + "|" + item.get("name") + "|" + item.get("createTime");
        } else if ("NETWORK".equals(type)) {
            return item.get("protocol") + "|" + item.get("localAddr") + "|" + item.get("remoteAddr") + "|"
                    + item.get("pid");
        } else if ("SOFTWARE".equals(type)) {
            return item.get("name") + "|" + item.get("version");
        } else if ("USB".equals(type)) {
            // For USB, add timestamp to the fingerprint if we want to record every plug
            // event
            // Or just keep deviceId if we want to track "Last Seen" for that unique device
            // To record "Plug-in history", the agent should ideally provide a unique
            // session or timestamp
            String base = String.valueOf(item.getOrDefault("deviceId", ""));
            String ts = String.valueOf(item.getOrDefault("lastSeen", ""));
            return ts.isEmpty() ? base : base + "|" + ts;
        } else if ("LOGIN".equals(type)) {
            return item.get("userName") + "|" + item.get("terminal") + "|" + item.get("loginTime");
        } else if ("TRAFFIC".equals(type)) {
            String base = item.get("srcIp") + "|" + item.get("dstIp") + "|" + item.get("dstPort") + "|"
                    + item.get("protocol");
            String ts = String.valueOf(item.getOrDefault("timestamp", ""));
            return ts.isEmpty() ? base : base + "|" + ts;
        }
        return String.valueOf(item.hashCode());
    }

    /**
     * Delete INACTIVE records older than 30 days
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void cleanupOldRecords() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        int deleted = hostAssetRecordMapper.deleteInactiveBefore(threshold);
        log.info("Cleaned up {} old inactive host_asset_record entries before {}", deleted, threshold);
    }

    public List<HostAssetRecord> getCurrentSnapshot(String agentId) {
        return hostAssetRecordMapper.selectList(new LambdaQueryWrapper<HostAssetRecord>()
                .eq(HostAssetRecord::getAgentId, agentId)
                .eq(HostAssetRecord::getStatus, "ACTIVE"));
    }

    /**
     * Gets active inventory + recent events for USB/LOGIN/TRAFFIC
     */
    public List<HostAssetRecord> getDetailedSnapshot(String agentId) {
        // 1. Get all ACTIVE records (Processes, Software, Network etc.)
        return hostAssetRecordMapper.selectList(new LambdaQueryWrapper<HostAssetRecord>()
                .eq(HostAssetRecord::getAgentId, agentId)
                .eq(HostAssetRecord::getStatus, "ACTIVE"));
    }

    public List<HostAssetRecord> getRecentEvents(String agentId, String type, int limit) {
        return hostAssetRecordMapper.selectList(new LambdaQueryWrapper<HostAssetRecord>()
                .eq(HostAssetRecord::getAgentId, agentId)
                .eq(HostAssetRecord::getAssetType, type)
                .orderByDesc(HostAssetRecord::getLastUpdated)
                .last("LIMIT " + limit));
    }

    public List<HostAssetRecord> getTimelineSnapshot(String agentId, LocalDateTime timestamp) {
        return hostAssetRecordMapper.selectList(new LambdaQueryWrapper<HostAssetRecord>()
                .eq(HostAssetRecord::getAgentId, agentId)
                .le(HostAssetRecord::getFirstSeen, timestamp)
                .ge(HostAssetRecord::getLastUpdated, timestamp));
    }

    public List<HostAssetRecord> getHistoryRecords(String agentId, String assetType, LocalDateTime startTime,
            LocalDateTime endTime) {
        LambdaQueryWrapper<HostAssetRecord> query = new LambdaQueryWrapper<HostAssetRecord>()
                .eq(HostAssetRecord::getAgentId, agentId)
                .ge(HostAssetRecord::getLastUpdated, startTime)
                .le(HostAssetRecord::getLastUpdated, endTime);
        if (assetType != null) {
            query.eq(HostAssetRecord::getAssetType, assetType);
        }
        return hostAssetRecordMapper.selectList(query);
    }
}
