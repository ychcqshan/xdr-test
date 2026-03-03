package com.xdr.upgrade.controller;

import com.xdr.common.dto.ApiResponse;
import com.xdr.upgrade.model.UpgradePackage;
import com.xdr.upgrade.model.UpgradeTask;
import com.xdr.upgrade.service.UpgradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/upgrades")
@RequiredArgsConstructor
public class UpgradeController {

    private final UpgradeService upgradeService;

    @GetMapping("/packages")
    public ApiResponse<List<UpgradePackage>> listPackages() {
        return ApiResponse.ok(upgradeService.listPackages());
    }

    @PostMapping("/packages")
    public ApiResponse<Void> savePackage(@RequestBody UpgradePackage pkg) {
        upgradeService.savePackage(pkg);
        return ApiResponse.ok();
    }

    /** 获取Agent待处理的升级任务 */
    @GetMapping("/tasks/pending/{agentId}")
    public ApiResponse<UpgradeTask> getPendingTask(@PathVariable String agentId) {
        return ApiResponse.ok(upgradeService.getPendingUpgrade(agentId));
    }

    /** Agent回传升级状态 */
    @PostMapping("/tasks/status")
    public ApiResponse<Void> updateStatus(
            @RequestParam String agentId,
            @RequestParam String status,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) Integer progress) {
        upgradeService.updateTaskStatus(agentId, status, error, progress);
        return ApiResponse.ok();
    }
}
