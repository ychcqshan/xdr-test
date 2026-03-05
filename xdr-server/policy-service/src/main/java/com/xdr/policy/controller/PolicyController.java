package com.xdr.policy.controller;

import com.xdr.common.dto.ApiResponse;
import com.xdr.policy.model.Policy;
import com.xdr.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    /** 获取所有策略列表 (管理端用) */
    @GetMapping
    public ApiResponse<List<Policy>> listPolicies() {
        return ApiResponse.ok(policyService.listPolicies());
    }

    /** 保存/更新策略 */
    @PostMapping
    public ApiResponse<Void> savePolicy(@RequestBody Policy policy) {
        policyService.savePolicy(policy);
        return ApiResponse.ok();
    }

    /** 获取针对特定Agent生效的策略 (Agent或Heartbeat转发调用) */
    @GetMapping("/effective/{agentId}")
    public ApiResponse<Policy> getEffectivePolicy(
            @PathVariable String agentId,
            @RequestParam(required = false) String groupId) {
        return ApiResponse.ok(policyService.getEffectivePolicy(agentId, groupId));
    }

    /** 创建处置指令 (由 ThreatService 调用) */
    @PostMapping("/commands")
    public ApiResponse<Void> createCommand(@RequestBody com.xdr.policy.model.ResponseCommand command) {
        policyService.createCommand(command);
        return ApiResponse.ok();
    }

    /** 获取待处理指令 (由 Agent 调用) */
    @GetMapping("/commands/pending/{agentId}")
    public ApiResponse<List<com.xdr.policy.model.ResponseCommand>> getPendingCommands(@PathVariable String agentId) {
        return ApiResponse.ok(policyService.getPendingCommands(agentId));
    }

    /** 更新指令执行状态 (由 Agent 调用) */
    @PutMapping("/commands/{commandId}/status")
    public ApiResponse<Void> updateCommandStatus(
            @PathVariable String commandId,
            @RequestParam String status,
            @RequestParam(required = false) String error) {
        policyService.updateCommandStatus(commandId, status, error);
        return ApiResponse.ok();
    }
}
