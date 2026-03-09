package com.xdr.baseline.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xdr.baseline.mapper.BaselineItemMapper;
import com.xdr.baseline.mapper.BaselineMapper;
import com.xdr.baseline.model.Baseline;
import com.xdr.baseline.model.BaselineItem;
import com.xdr.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaselineService {

    private final BaselineMapper baselineMapper;
    private final BaselineItemMapper baselineItemMapper;
    private final ObjectMapper objectMapper;
    private final com.xdr.baseline.client.AssetServiceClient assetServiceClient;

    private static final double FREQUENCY_THRESHOLD = 0.5; // 50% occurrence threshold

    /**
     * S-BL-001/002/003: 启动基线学习
     */
    public Baseline startLearning(String agentId, String type, int durationHours) {
        // 检查是否已存在学习中的基线
        Baseline existing = baselineMapper.selectOne(
                new LambdaQueryWrapper<Baseline>()
                        .eq(Baseline::getAgentId, agentId)
                        .eq(Baseline::getType, type)
                        .eq(Baseline::getStatus, "LEARNING"));
        if (existing != null) {
            throw new BusinessException("该Agent的" + type + "基线正在学习中");
        }

        Baseline baseline = new Baseline();
        baseline.setAgentId(agentId);
        baseline.setType(type);
        baseline.setStatus("LEARNING");
        baseline.setVersion(1);
        baseline.setLearningStart(LocalDateTime.now());
        baseline.setLearningEnd(LocalDateTime.now().plusHours(durationHours));
        baseline.setLearningDurationHours(durationHours);
        baselineMapper.insert(baseline);
        return baseline;
    }

    /**
     * S-BL-004: 导入当前系统快照为基线
     */
    @Transactional
    public Baseline importBaseline(String agentId, String type, List<Map<String, Object>> items) {
        Baseline baseline = getOrCreateBaseline(agentId, type);
        baseline.setStatus("PENDING_REVIEW");
        baselineMapper.updateById(baseline);

        saveItems(baseline.getId(), type, items);
        return baseline;
    }

    /**
     * S-BL-010: 获取基线列表(分页演示，Phase 2 暂做全量)
     */
    public List<Baseline> listBaselines(String type, String unit, String responsiblePerson) {
        LambdaQueryWrapper<Baseline> query = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty() && !"ALL".equals(type)) {
            query.eq(Baseline::getType, type);
        }
        if (unit != null && !unit.isEmpty()) {
            query.eq(Baseline::getUnit, unit);
        }
        if (responsiblePerson != null && !responsiblePerson.isEmpty()) {
            query.eq(Baseline::getResponsiblePerson, responsiblePerson);
        }
        query.orderByDesc(Baseline::getLearningEnd);
        return baselineMapper.selectList(query);
    }

    private void saveItems(String baselineId, String type, List<Map<String, Object>> items) {
        // 清除旧的基线项
        baselineItemMapper.delete(
                new LambdaQueryWrapper<BaselineItem>().eq(BaselineItem::getBaselineId, baselineId));

        // 插入新的基线项
        for (Map<String, Object> item : items) {
            BaselineItem bi = new BaselineItem();
            bi.setBaselineId(baselineId);
            bi.setItemKey(buildItemKey(type, item));
            try {
                bi.setItemData(objectMapper.writeValueAsString(item));
            } catch (Exception e) {
                throw new BusinessException("基线数据序列化失败");
            }
            baselineItemMapper.insert(bi);
        }
    }

    /**
     * S-BL-009: 时序频率分析学习 (Phase 6 核心)
     */
    @Transactional
    public void learnFromHistory(String agentId, String type) {
        Baseline baseline = baselineMapper.selectOne(
                new LambdaQueryWrapper<Baseline>()
                        .eq(Baseline::getAgentId, agentId)
                        .eq(Baseline::getType, type)
                        .eq(Baseline::getStatus, "LEARNING"));

        if (baseline == null)
            return;

        LocalDateTime startTime = baseline.getLearningStart();
        LocalDateTime endTime = LocalDateTime.now();

        // 1. 获取历史记录
        List<Map<String, Object>> rawRecords = assetServiceClient.getAssetHistory(agentId, startTime, endTime);
        if (rawRecords.isEmpty())
            return;

        // 2. 按时间戳分组 (模拟快照次数)
        Map<String, List<Map<String, Object>>> snapshots = new HashMap<>();
        for (Map<String, Object> record : rawRecords) {
            String updateTime = (String) record.get("lastUpdated");
            if (updateTime != null) {
                snapshots.computeIfAbsent(updateTime, k -> new ArrayList<>()).add(record);
            }
        }

        int totalSnapshots = snapshots.size();
        if (totalSnapshots == 0)
            return;

        // 3. 统计每个项的出现频率
        Map<String, Integer> keyCountMap = new HashMap<>();
        Map<String, Map<String, Object>> keyDataMap = new HashMap<>();

        for (List<Map<String, Object>> snapshotItems : snapshots.values()) {
            Set<String> keysInSnapshot = new HashSet<>();
            for (Map<String, Object> itemRecord : snapshotItems) {
                try {
                    String assetDataStr = (String) itemRecord.get("assetData");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = objectMapper.readValue(assetDataStr, Map.class);
                    String key = buildItemKey(type, data);
                    keysInSnapshot.add(key);
                    keyDataMap.put(key, data);
                } catch (Exception ignored) {
                }
            }
            for (String key : keysInSnapshot) {
                keyCountMap.put(key, keyCountMap.getOrDefault(key, 0) + 1);
            }
        }

        // 4. 筛选高频项
        List<Map<String, Object>> filteredItems = new ArrayList<>();
        for (var entry : keyCountMap.entrySet()) {
            double frequency = (double) entry.getValue() / totalSnapshots;
            if (frequency >= FREQUENCY_THRESHOLD) {
                filteredItems.add(keyDataMap.get(entry.getKey()));
            }
        }

        // 5. 更新基线
        saveItems(baseline.getId(), type, filteredItems);
        baseline.setStatus("PENDING_REVIEW");
        baseline.setLearningEnd(endTime);
        baselineMapper.updateById(baseline);

        log.info("Baseline learning completed for {} types {}. total snapshots: {}, items: {}",
                agentId, type, totalSnapshots, filteredItems.size());
    }

    /**
     * S-BL-008: 基线比对
     */
    public Map<String, Object> compare(String agentId, String type, List<Map<String, Object>> currentData) {
        Baseline baseline = baselineMapper.selectOne(
                new LambdaQueryWrapper<Baseline>()
                        .eq(Baseline::getAgentId, agentId)
                        .eq(Baseline::getType, type)
                        .eq(Baseline::getStatus, "ACTIVE"));
        if (baseline == null) {
            throw new BusinessException("未找到生效的" + type + "基线");
        }

        List<BaselineItem> baselineItems = baselineItemMapper.selectList(
                new LambdaQueryWrapper<BaselineItem>().eq(BaselineItem::getBaselineId, baseline.getId()));

        Map<String, String> baselineMap = baselineItems.stream()
                .collect(Collectors.toMap(BaselineItem::getItemKey, BaselineItem::getItemData));

        Map<String, Map<String, Object>> currentMap = currentData.stream()
                .collect(Collectors.toMap(
                        item -> buildItemKey(type, item),
                        item -> item,
                        (a, b) -> a));

        List<Map<String, Object>> added = new ArrayList<>(); // 新增
        List<Map<String, Object>> removed = new ArrayList<>(); // 缺失
        List<Map<String, Object>> modified = new ArrayList<>(); // 修改

        // 检测新增项（当前有，基线无）
        for (var entry : currentMap.entrySet()) {
            if (!baselineMap.containsKey(entry.getKey())) {
                added.add(entry.getValue());
            }
        }

        // 检测缺失和修改项
        for (var entry : baselineMap.entrySet()) {
            if (!currentMap.containsKey(entry.getKey())) {
                try {
                    removed.add(objectMapper.readValue(entry.getValue(), Map.class));
                } catch (Exception ignored) {
                }
            } else {
                try {
                    Map<String, Object> baselineData = objectMapper.readValue(entry.getValue(), Map.class);
                    if (!baselineData.equals(currentMap.get(entry.getKey()))) {
                        Map<String, Object> diff = new HashMap<>();
                        diff.put("baseline", baselineData);
                        diff.put("current", currentMap.get(entry.getKey()));
                        modified.add(diff);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return Map.of(
                "agentId", agentId,
                "type", type,
                "added", added,
                "removed", removed,
                "modified", modified,
                "totalDiff", added.size() + removed.size() + modified.size());
    }

    /**
     * S-BL-007: 审核基线（通过）
     */
    @Transactional
    public Baseline approveBaseline(String agentId, String type) {
        Baseline baseline = baselineMapper.selectOne(
                new LambdaQueryWrapper<Baseline>()
                        .eq(Baseline::getAgentId, agentId)
                        .eq(Baseline::getType, type)
                        .eq(Baseline::getStatus, "PENDING_REVIEW"));
        if (baseline == null)
            throw new BusinessException("未找到待审核的基线");
        baseline.setStatus("ACTIVE");
        baselineMapper.updateById(baseline);
        return baseline;
    }

    /** 查询基线 */
    public Baseline getBaseline(String agentId, String type) {
        return baselineMapper.selectOne(
                new LambdaQueryWrapper<Baseline>()
                        .eq(Baseline::getAgentId, agentId)
                        .eq(Baseline::getType, type)
                        .orderByDesc(Baseline::getVersion)
                        .last("LIMIT 1"));
    }

    /** 查询基线项列表 */
    public List<BaselineItem> getBaselineItems(String baselineId) {
        return baselineItemMapper.selectList(
                new LambdaQueryWrapper<BaselineItem>().eq(BaselineItem::getBaselineId, baselineId));
    }

    private Baseline getOrCreateBaseline(String agentId, String type) {
        Baseline baseline = baselineMapper.selectOne(
                new LambdaQueryWrapper<Baseline>()
                        .eq(Baseline::getAgentId, agentId)
                        .eq(Baseline::getType, type));
        if (baseline == null) {
            baseline = new Baseline();
            baseline.setAgentId(agentId);
            baseline.setType(type);
            baseline.setVersion(1);
            baseline.setStatus("PENDING_REVIEW");
            baselineMapper.insert(baseline);
        } else {
            baseline.setVersion(baseline.getVersion() + 1);
        }
        return baseline;
    }

    /**
     * S-BL-005: 复制基线
     */
    @Transactional
    public Baseline copyBaseline(String sourceAgentId, String targetAgentId, String type) {
        Baseline source = baselineMapper.selectOne(
                new LambdaQueryWrapper<Baseline>()
                        .eq(Baseline::getAgentId, sourceAgentId)
                        .eq(Baseline::getType, type)
                        .eq(Baseline::getStatus, "ACTIVE"));
        if (source == null)
            throw new BusinessException("源Agent未找到生效的" + type + "基线");

        Baseline target = getOrCreateBaseline(targetAgentId, type);
        target.setStatus("PENDING_REVIEW"); // 复制过来默认待审核
        baselineMapper.updateById(target);

        // 清除目标旧项
        baselineItemMapper.delete(
                new LambdaQueryWrapper<BaselineItem>().eq(BaselineItem::getBaselineId, target.getId()));

        // 复制项
        List<BaselineItem> sourceItems = baselineItemMapper.selectList(
                new LambdaQueryWrapper<BaselineItem>().eq(BaselineItem::getBaselineId, source.getId()));
        for (BaselineItem item : sourceItems) {
            BaselineItem newItem = new BaselineItem();
            newItem.setBaselineId(target.getId());
            newItem.setItemKey(item.getItemKey());
            newItem.setItemData(item.getItemData());
            baselineItemMapper.insert(newItem);
        }
        return target;
    }

    /**
     * S-BL-006: 手动添加基线项
     */
    @Transactional
    public void addManualItem(String agentId, String type, Map<String, Object> itemData) {
        Baseline baseline = getOrCreateBaseline(agentId, type);

        BaselineItem bi = new BaselineItem();
        bi.setBaselineId(baseline.getId());
        bi.setItemKey(buildItemKey(type, itemData));
        try {
            bi.setItemData(objectMapper.writeValueAsString(itemData));
        } catch (Exception e) {
            throw new BusinessException("序列化失败");
        }

        // 如果key已存在则更新，不存在则插入
        BaselineItem existing = baselineItemMapper.selectOne(
                new LambdaQueryWrapper<BaselineItem>()
                        .eq(BaselineItem::getBaselineId, baseline.getId())
                        .eq(BaselineItem::getItemKey, bi.getItemKey()));

        if (existing != null) {
            bi.setId(existing.getId());
            baselineItemMapper.updateById(bi);
        } else {
            baselineItemMapper.insert(bi);
        }
    }

    /** 根据基线类型构造比对键 */
    private String buildItemKey(String type, Map<String, Object> item) {
        return switch (type) {
            case "PROCESS" -> item.getOrDefault("name", "") + "|" + item.getOrDefault("path", "");
            case "PORT" -> item.getOrDefault("port", "") + "|" + item.getOrDefault("protocol", "");
            case "USB" -> String.valueOf(item.getOrDefault("serialNumber", ""));
            case "LOGIN" -> item.getOrDefault("username", "") + "|" + item.getOrDefault("loginType", "");
            case "SOFTWARE" -> item.getOrDefault("name", "") + "|" + item.getOrDefault("publisher", "");
            default -> String.valueOf(item.hashCode());
        };
    }
}
