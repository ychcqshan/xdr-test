package com.xdr.threat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertMapper alertMapper;
    private final EventMapper eventMapper;
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
            throw new BusinessException("事件数据序列化失败");
        }
        eventMapper.insert(event);

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
        if (alert == null) throw new BusinessException("告警不存在");

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
        LambdaQueryWrapper<Alert> query = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(level)) query.eq(Alert::getLevel, level);
        if (StringUtils.hasText(status)) query.eq(Alert::getStatus, status);
        if (StringUtils.hasText(agentId)) query.eq(Alert::getAgentId, agentId);
        if (StringUtils.hasText(threatType)) query.eq(Alert::getThreatType, threatType);

        query.orderByDesc(Alert::getCreatedAt);

        Page<Alert> p = alertMapper.selectPage(new Page<>(page, size), query);
        return PageResponse.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    /** 告警详情 */
    public Alert getAlert(String id) {
        Alert alert = alertMapper.selectById(id);
        if (alert == null) throw new BusinessException("告警不存在");
        return alert;
    }

    /** 告警统计(仪表盘用) */
    public Map<String, Object> getStats() {
        long total = alertMapper.selectCount(null);
        long newCount = alertMapper.selectCount(
                new LambdaQueryWrapper<Alert>().eq(Alert::getStatus, "NEW")
        );
        List<Map<String, Object>> byLevel = alertMapper.countByLevel();
        List<Map<String, Object>> trend = alertMapper.countByDay(7);

        return Map.of(
                "total", total,
                "new", newCount,
                "byLevel", byLevel,
                "trend7d", trend
        );
    }

    /** 基线偏差生成告警 */
    private void generateBaselineAlert(Event event, Map<String, Object> data) {
        int totalDiff = ((Number) data.getOrDefault("totalDiff", 0)).intValue();
        if (totalDiff == 0) return;

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
}
