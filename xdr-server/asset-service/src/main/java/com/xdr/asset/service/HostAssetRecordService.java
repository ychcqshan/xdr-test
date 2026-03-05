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

    @Transactional(rollbackFor = Exception.class)
    public void syncAssets(SyncAssetRequest request) {
        String agentId = request.getAgentId();
        String type = request.getAssetType();
        String reportType = request.getReportType();
        List<Map<String, Object>> items = request.getItems();

        if (items == null || items.isEmpty()) {
            return;
        }

        LocalDateTime syncTime = LocalDateTime.now();

        if ("FULL".equals(reportType)) {
            // Full sync: upsert current items, then mark others as INACTIVE
            for (Map<String, Object> item : items) {
                upsertAsset(agentId, type, item, syncTime);
            }
            // Everything that was not updated in this sync run (last_updated < syncTime)
            // becomes INACTIVE
            hostAssetRecordMapper.markInactiveBefore(agentId, syncTime);
        } else {
            // Incremental
            for (Map<String, Object> item : items) {
                String action = (String) item.getOrDefault("action", "ADD");
                if ("REMOVE".equals(action)) {
                    removeAsset(agentId, type, item, syncTime);
                } else {
                    upsertAsset(agentId, type, item, syncTime);
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
            return (String) item.getOrDefault("raw", String.valueOf(item.hashCode()));
        } else if ("LOGIN".equals(type)) {
            return item.get("userName") + "|" + item.get("terminal") + "|" + item.get("loginTime");
        } else if ("TRAFFIC".equals(type)) {
            return item.get("srcIp") + "|" + item.get("dstIp") + "|" + item.get("dstPort") + "|" + item.get("protocol");
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
}
