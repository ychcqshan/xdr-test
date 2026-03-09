package com.xdr.baseline.controller;

import com.xdr.baseline.model.Baseline;
import com.xdr.baseline.model.BaselineItem;
import com.xdr.baseline.service.BaselineService;
import com.xdr.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/baselines")
@RequiredArgsConstructor
public class BaselineController {

    private final BaselineService baselineService;

    /** S-BL-010: 获取基线列表 */
    @GetMapping
    public ApiResponse<List<Baseline>> listBaselines(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) String responsiblePerson) {
        return ApiResponse.ok(baselineService.listBaselines(type, unit, responsiblePerson));
    }

    /** S-BL-001/002/003: 启动基线学习 */
    @PostMapping("/{agentId}/{type}/learn")
    public ApiResponse<Baseline> startLearning(
            @PathVariable String agentId,
            @PathVariable String type,
            @RequestParam(defaultValue = "168") int durationHours) {
        return ApiResponse.ok(baselineService.startLearning(agentId, type, durationHours));
    }

    /** S-BL-009: 强制完成学习并从历史记录生成基线 (Phase 6) */
    @PostMapping("/{agentId}/{type}/learn/complete")
    public ApiResponse<Void> completeLearning(
            @PathVariable String agentId,
            @PathVariable String type) {
        baselineService.learnFromHistory(agentId, type);
        return ApiResponse.ok();
    }

    /** S-BL-004: 导入当前系统为基线 */
    @PostMapping("/{agentId}/{type}/import")
    public ApiResponse<Baseline> importBaseline(
            @PathVariable String agentId,
            @PathVariable String type,
            @RequestBody List<Map<String, Object>> items) {
        return ApiResponse.ok(baselineService.importBaseline(agentId, type, items));
    }

    /** S-BL-008: 基线比对 */
    @PostMapping("/{agentId}/{type}/compare")
    public ApiResponse<Map<String, Object>> compare(
            @PathVariable String agentId,
            @PathVariable String type,
            @RequestBody List<Map<String, Object>> currentData) {
        return ApiResponse.ok(baselineService.compare(agentId, type, currentData));
    }

    /** S-BL-007: 审核基线(通过) */
    @PostMapping("/{agentId}/{type}/approve")
    public ApiResponse<Baseline> approve(
            @PathVariable String agentId,
            @PathVariable String type) {
        return ApiResponse.ok(baselineService.approveBaseline(agentId, type));
    }

    /** 查询基线 */
    @GetMapping("/{agentId}/{type}")
    public ApiResponse<Baseline> getBaseline(
            @PathVariable String agentId,
            @PathVariable String type) {
        return ApiResponse.ok(baselineService.getBaseline(agentId, type));
    }

    /** 查询基线项列表 */
    @GetMapping("/{baselineId}/items")
    public ApiResponse<List<BaselineItem>> getItems(@PathVariable String baselineId) {
        return ApiResponse.ok(baselineService.getBaselineItems(baselineId));
    }

    /** S-BL-005: 复制基线 (Phase 2) */
    @PostMapping("/copy")
    public ApiResponse<Baseline> copyBaseline(
            @RequestParam String sourceAgentId,
            @RequestParam String targetAgentId,
            @RequestParam String type) {
        return ApiResponse.ok(baselineService.copyBaseline(sourceAgentId, targetAgentId, type));
    }

    /** S-BL-006: 手动维护基线条目 (Phase 2) */
    @PostMapping("/{agentId}/{type}/items/manual")
    public ApiResponse<Void> addManualItem(
            @PathVariable String agentId,
            @PathVariable String type,
            @RequestBody Map<String, Object> itemData) {
        baselineService.addManualItem(agentId, type, itemData);
        return ApiResponse.ok();
    }
}
