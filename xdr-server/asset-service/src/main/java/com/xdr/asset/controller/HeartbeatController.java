package com.xdr.asset.controller;

import com.xdr.asset.service.AssetService;
import com.xdr.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/heartbeat")
@RequiredArgsConstructor
public class HeartbeatController {

    private final AssetService assetService;

    /**
     * A-COMM-004: Agent心跳上报
     * 心跳数据同时用于 S-ASSET-001 资产自动注册/更新 和 S-ASSET-006 在线状态管理
     */
    @PostMapping
    public ApiResponse<Map<String, String>> heartbeat(@RequestBody Map<String, Object> data) {
        String agentId = (String) data.get("agentId");
        var asset = assetService.registerOrUpdate(agentId, data);
        return ApiResponse.ok(Map.of("agentId", asset.getAgentId(), "status", "OK"));
    }
}
