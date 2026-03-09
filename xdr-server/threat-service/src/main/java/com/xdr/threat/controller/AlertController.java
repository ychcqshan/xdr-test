package com.xdr.threat.controller;

import com.xdr.common.dto.ApiResponse;
import com.xdr.common.dto.PageResponse;
import com.xdr.threat.model.Alert;
import com.xdr.threat.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    /** S-THR-012: 告警列表 */
    @GetMapping
    public ApiResponse<PageResponse<Alert>> listAlerts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) String threatType,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) String responsiblePerson) {
        return ApiResponse
                .ok(alertService.listAlerts(page, size, level, status, agentId, threatType, unit, responsiblePerson));
    }

    /** 告警详情 */
    @GetMapping("/{id}")
    public ApiResponse<Alert> getAlert(@PathVariable String id) {
        return ApiResponse.ok(alertService.getAlert(id));
    }

    /** S-THR-004: 更新告警状态 */
    @PutMapping("/{id}/status")
    public ApiResponse<Alert> updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        return ApiResponse.ok(alertService.updateAlertStatus(
                id,
                body.get("status"),
                body.get("operator"),
                body.get("comment")));
    }

    /** 告警统计(仪表盘) */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        return ApiResponse.ok(alertService.getStats());
    }

    /** 执行针对性响应动作 (Phase 2) */
    @PostMapping("/{id}/respond")
    public ApiResponse<Void> executeResponse(
            @PathVariable String id,
            @RequestParam String operator) {
        alertService.executeResponse(id, operator);
        return ApiResponse.ok();
    }
}
