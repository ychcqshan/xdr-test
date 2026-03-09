package com.xdr.asset.controller;

import com.xdr.asset.dto.AssetDetailDTO;
import com.xdr.asset.model.Asset;
import com.xdr.asset.model.UserInfo;
import com.xdr.asset.service.AssetService;
import com.xdr.common.dto.ApiResponse;
import com.xdr.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final com.xdr.asset.service.TopologyService topologyService;

    /** S-ASSET-004: 资产列表(分页+搜索) */
    @GetMapping
    public ApiResponse<PageResponse<Asset>> listAssets(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String osType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String groupId,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) String responsiblePerson) {
        return ApiResponse
                .ok(assetService.listAssets(page, size, keyword, osType, status, groupId, unit, responsiblePerson));
    }

    /** S-ASSET-002: 资产详情 */
    @GetMapping("/{id}")
    public ApiResponse<Asset> getAsset(@PathVariable String id) {
        return ApiResponse.ok(assetService.getAssetDetail(id));
    }

    /** S-ASSET-002: 资产聚合详情 (Phase 2, 11 & 15) */
    @GetMapping("/{agentId}/details")
    public ApiResponse<AssetDetailDTO> getAssetDetails(
            @PathVariable String agentId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startTime,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endTime) {
        return ApiResponse.ok(assetService.getAggregatedAssetDetail(agentId, startTime, endTime));
    }

    /** 仪表盘统计 */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        return ApiResponse.ok(assetService.getStats());
    }

    /** 网络拓扑图 (Phase 2) */
    @GetMapping("/topology")
    public ApiResponse<com.xdr.asset.dto.GraphDTO> getTopology() {
        return ApiResponse.ok(topologyService.getNetworkTopology());
    }

    /** S-ASSET-008: 用户信息上报 */
    @PostMapping("/{agentId}/user-info")
    public ApiResponse<Void> saveUserInfo(@PathVariable String agentId, @RequestBody UserInfo info) {
        assetService.saveUserInfo(agentId, info);
        return ApiResponse.ok();
    }

    /** S-ASSET-010: 获取指定时间点的资产快照 (时光机) */
    @GetMapping("/{agentId}/timeline")
    public ApiResponse<java.util.List<com.xdr.asset.model.HostAssetRecord>> getAssetTimeline(
            @PathVariable String agentId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime timestamp) {
        return ApiResponse.ok(assetService.getTimelineSnapshot(agentId, timestamp));
    }

    /** S-ASSET-011: 获取历史资产记录范围 (用于基线进化、频率分析) */
    @GetMapping("/{agentId}/history")
    public ApiResponse<java.util.List<com.xdr.asset.model.HostAssetRecord>> getAssetHistory(
            @PathVariable String agentId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startTime,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endTime) {
        return ApiResponse.ok(assetService.getHistoryRecords(agentId, startTime, endTime));
    }
}
