package com.xdr.threat.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xdr.common.dto.PageResponse;
import com.xdr.common.exception.BusinessException;
import com.xdr.threat.mapper.AlertMapper;
import com.xdr.threat.mapper.EventMapper;
import com.xdr.threat.mapper.HostAssetMapper;
import com.xdr.threat.model.Alert;
import com.xdr.threat.model.Event;
import com.xdr.threat.model.HostAsset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertMapper alertMapper;
    private final EventMapper eventMapper;
    private final HostAssetMapper hostAssetMapper;
    private final ObjectMapper objectMapper;

    /**
     * S-THR-001: 接收Agent上报事件并存储
     */
    public Event receiveEvent(Map<String, Object> eventData) {
        Event event = new Event();
        event.setAgentId((String) eventData.get("agentId"));
        event.setEventType((String) eventData.get("eventType"));
        event.setPriority((String) eventData.getOrDefault("priority", "LOW"));
        event.setProcessed(0);
        try {
            event.setEventData(objectMapper.writeValueAsString(eventData));
        } catch (Exception e) {
            log.error("事件数据序列化失败", e);
        }
        eventMapper.insert(event);

        // 同步资产快照 (增量逻辑)
        Object rawNestedData = eventData.get("eventData");
        Map<String, Object> nestedData = null;
        if (rawNestedData instanceof Map) {
            nestedData = (Map<String, Object>) rawNestedData;
        } else if (rawNestedData instanceof String) {
            try {
                nestedData = objectMapper.readValue((String) rawNestedData, Map.class);
            } catch (Exception e) {
                log.error("解析嵌套eventData失败: {}", rawNestedData);
            }
        }

        if (nestedData != null) {
            String reportType = (String) nestedData.getOrDefault("reportType", "FULL");
            List<Map<String, Object>> items = (List<Map<String, Object>>) nestedData.get("items");
            if (items != null && !items.isEmpty()) {
                syncAssetSnapshot(event.getAgentId(), event.getEventType(), reportType, items);
            }
        }

        // S-THR-002/003: 简单规则匹配 - 基线偏差事件直接生成告警
        if ("BASELINE_DIFF".equals(event.getEventType())) {
            generateBaselineAlert(event, eventData);
        }

        return event;
    }

    /**
     * S-THR-004: 告警状态管理
     */
    public Alert updateAlertStatus(String alertId, String status, String operator, String comment) {
        Alert alert = alertMapper.selectById(alertId);
        if (alert == null)
            throw new BusinessException("告警不存在");

        alert.setStatus(status);
        if ("RESOLVED".equals(status) || "IGNORED".equals(status)) {
            alert.setResolvedAt(LocalDateTime.now());
            alert.setResolvedBy(operator);
            alert.setResolveComment(comment);
        }
        alertMapper.updateById(alert);
        return alert;
    }

    /**
     * S-THR-012: 告警列表(分页+筛选)
     */
    public PageResponse<Alert> listAlerts(int page, int size, String level,
            String status, String agentId, String threatType) {
        QueryWrapper<Alert> query = new QueryWrapper<>();

        if (StringUtils.hasText(level))
            query.eq("level", level);
        if (StringUtils.hasText(status))
            query.eq("status", status);
        if (StringUtils.hasText(agentId))
            query.eq("agent_id", agentId);
        if (StringUtils.hasText(threatType))
            query.eq("threat_type", threatType);

        query.orderByDesc("created_at");

        Page<Alert> p = alertMapper.selectPage(new Page<>(page, size), query);
        return PageResponse.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    /** 告警详情 */
    public Alert getAlert(String id) {
        Alert alert = alertMapper.selectById(id);
        if (alert == null)
            throw new BusinessException("告警不存在");
        return alert;
    }

    /** 告警统计(仪表盘用) */
    public Map<String, Object> getStats() {
        long total = alertMapper.selectCount(null);
        long newCount = alertMapper.selectCount(
                new QueryWrapper<Alert>().eq("status", "NEW"));
        List<Map<String, Object>> byLevel = alertMapper.countByLevel();
        List<Map<String, Object>> trend = alertMapper.countByDay(7);

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("new", newCount);
        stats.put("byLevel", byLevel);
        stats.put("trend7d", trend);
        return stats;
    }

    /**
     * 同步资产快照
     */
    private void syncAssetSnapshot(String agentId, String type, String reportType, List<Map<String, Object>> items) {
        if (!"PROCESS".equals(type) && !"NETWORK".equals(type))
            return;

        if ("FULL".equals(reportType)) {
            // 全量上报：删除旧的，插入新的
            hostAssetMapper.delete(new QueryWrapper<HostAsset>()
                    .eq("agent_id", agentId)
                    .eq("asset_type", type));
            for (Map<String, Object> item : items) {
                saveAsset(agentId, type, item);
            }
        } else if ("INCREMENTAL".equals(reportType)) {
            // 增量上报：按 action 处理
            for (Map<String, Object> item : items) {
                String action = (String) item.getOrDefault("action", "ADD");
                String assetId = buildAssetId(type, item);
                if ("REMOVE".equals(action)) {
                    hostAssetMapper.delete(new QueryWrapper<HostAsset>()
                            .eq("agent_id", agentId)
                            .eq("asset_type", type)
                            .eq("asset_id", assetId));
                } else {
                    // ADD 或 UPDATE
                    saveAsset(agentId, type, item);
                }
            }
        }
    }

    private void saveAsset(String agentId, String type, Map<String, Object> item) {
        String assetId = buildAssetId(type, item);
        HostAsset asset = hostAssetMapper.selectOne(new QueryWrapper<HostAsset>()
                .eq("agent_id", agentId)
                .eq("asset_type", type)
                .eq("asset_id", assetId));

        if (asset == null) {
            asset = new HostAsset();
            asset.setAgentId(agentId);
            asset.setAssetType(type);
            asset.setAssetId(assetId);
        }

        try {
            asset.setAssetData(objectMapper.writeValueAsString(item));
        } catch (Exception e) {
            log.error("资产数据序列化失败", e);
        }

        if (asset.getId() == null) {
            hostAssetMapper.insert(asset);
        } else {
            hostAssetMapper.updateById(asset);
        }
    }

    private String buildAssetId(String type, Map<String, Object> item) {
        if ("PROCESS".equals(type)) {
            return item.get("pid") + "|" + item.get("name") + "|" + item.get("createTime");
        } else if ("NETWORK".equals(type)) {
            return item.get("protocol") + "|" + item.get("localAddr") + "|" + item.get("remoteAddr") + "|"
                    + item.get("pid");
        }
        return String.valueOf(item.hashCode());
    }

    /** 基线偏差生成告警 */
    private void generateBaselineAlert(Event event, Map<String, Object> data) {
        int totalDiff = ((Number) data.getOrDefault("totalDiff", 0)).intValue();
        if (totalDiff == 0)
            return;

        Alert alert = new Alert();
        alert.setAgentId(event.getAgentId());
        alert.setLevel(totalDiff > 10 ? "HIGH" : "MEDIUM");
        alert.setStatus("NEW");
        alert.setThreatType("BASELINE_VIOLATION");
        alert.setTitle("基线偏差告警: " + data.getOrDefault("type", "UNKNOWN"));
        alert.setDescription("检测到" + totalDiff + "项基线偏差");
        alert.setRawEvent(event.getEventData());
        alert.setPriority(totalDiff > 10 ? 80 : 50);
        alertMapper.insert(alert);
    }

    /** 执行响应动作 (Phase 2) */
    public void executeResponse(String alertId, String operator) {
        Alert alert = alertMapper.selectById(alertId);
        if (alert == null)
            throw new BusinessException("告警不存在");
        if (!StringUtils.hasText(alert.getResponseAction()))
            throw new BusinessException("该告警无建议响应动作");

        log.info("执行响应动作: {} 对 告警 {}", alert.getResponseAction(), alertId);
        // 此处逻辑应当下发指令给 Agent，Phase 2 暂做状态变更展示
        alert.setResponseStatus("EXECUTED");
        alertMapper.updateById(alert);
    }
}
