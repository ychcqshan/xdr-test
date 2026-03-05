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
}
