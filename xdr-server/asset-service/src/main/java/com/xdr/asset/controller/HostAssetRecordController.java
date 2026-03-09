package com.xdr.asset.controller;

import com.xdr.asset.dto.SyncAssetRequest;
import com.xdr.asset.service.HostAssetRecordService;
import com.xdr.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/assets/internal")
@RequiredArgsConstructor
public class HostAssetRecordController {

    private final HostAssetRecordService hostAssetRecordService;

    /** Internal API for threat-service to push asset snapshots */
    @PostMapping("/sync")
    public ApiResponse<Void> syncAssets(@RequestBody SyncAssetRequest request) {
        hostAssetRecordService.syncAssets(request);
        return ApiResponse.ok();
    }

    /** S-ASSET-010: 获取指定时间点的资产快照 (Time-travel) */
    @GetMapping("/timeline")
    public ApiResponse<java.util.List<com.xdr.asset.model.HostAssetRecord>> getTimeline(
            @RequestParam String agentId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime timestamp) {
        if (timestamp == null) {
            return ApiResponse.ok(hostAssetRecordService.getCurrentSnapshot(agentId));
        }
        return ApiResponse.ok(hostAssetRecordService.getTimelineSnapshot(agentId, timestamp));
    }
}
