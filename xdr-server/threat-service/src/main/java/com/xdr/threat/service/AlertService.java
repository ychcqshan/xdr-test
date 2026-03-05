package com.xdr.threat.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xdr.common.dto.PageResponse;
import com.xdr.common.exception.BusinessException;
import com.xdr.threat.mapper.AlertMapper;
import com.xdr.threat.mapper.EventMapper;
import com.xdr.threat.model.Alert;
import com.xdr.threat.model.Event;
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
    private final ObjectMapper objectMapper;
    private final org.springframework.web.client.RestTemplate restTemplate;

    private static final String POLICY_SERVICE_URL = "http://localhost:8085";

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
     * 同步资产快照到 asset-service
     */
    private void syncAssetSnapshot(String agentId, String type, String reportType, List<Map<String, Object>> items) {
        if (!"PROCESS".equals(type) && !"NETWORK".equals(type) &&
                !"SOFTWARE".equals(type) && !"USB".equals(type) && !"LOGIN".equals(type) &&
                !"TRAFFIC".equals(type))
            return;

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("agentId", agentId);
            request.put("assetType", type);
            request.put("reportType", reportType);
            request.put("items", items);

            restTemplate.postForObject("http://localhost:8082/api/v1/assets/internal/sync", request, Object.class);
        } catch (Exception e) {
            log.error("Failed to sync asset snapshot to asset-service", e);
        }
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

    /** 执行响应动作 (Phase 2 & 3) */
    public void executeResponse(String alertId, String operator) {
        Alert alert = alertMapper.selectById(alertId);
        if (alert == null)
            throw new BusinessException("告警不存在");
        if (!StringUtils.hasText(alert.getResponseAction()))
            throw new BusinessException("该告警无建议响应动作");

        log.info("执行响应动作: {} 对 告警 {}", alert.getResponseAction(), alertId);

        // Phase 3: 下发指令到 policy-service
        try {
            Map<String, Object> command = new HashMap<>();
            command.put("agentId", alert.getAgentId());
            command.put("commandType", alert.getResponseAction());
            // 如果是终端进程，提取相关联的 PID (假设存储在 rawEvent 中)
            Map<String, Object> commandData = new HashMap<>();
            if ("TERMINATE_PROCESS".equals(alert.getResponseAction())) {
                // 简单示例：从描述或rawEvent中提取PID，实际应有更结构化的存储
                commandData.put("pid", extractPidFromAlert(alert));
            }
            command.put("commandData", objectMapper.writeValueAsString(commandData));

            restTemplate.postForEntity(POLICY_SERVICE_URL + "/api/v1/policies/commands", command, Void.class);

            alert.setResponseStatus("SENT");
        } catch (Exception e) {
            log.error("下发指令失败", e);
            alert.setResponseStatus("FAILED");
            alert.setResolveComment("指令下发失败: " + e.getMessage());
        }

        alertMapper.updateById(alert);
    }

    private String extractPidFromAlert(Alert alert) {
        // 模型简化处理：从 rawEvent JSON 中提取逻辑
        try {
            Map<String, Object> event = objectMapper.readValue(alert.getRawEvent(), Map.class);
            if (event.containsKey("pid"))
                return String.valueOf(event.get("pid"));
            // 嵌套逻辑
            Map<String, Object> data = (Map<String, Object>) event.get("eventData");
            if (data != null && data.containsKey("pid"))
                return String.valueOf(data.get("pid"));
        } catch (Exception e) {
        }
        return "0";
    }
}
