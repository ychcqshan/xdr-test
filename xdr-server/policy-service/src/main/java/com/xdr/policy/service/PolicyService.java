package com.xdr.policy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xdr.common.exception.BusinessException;
import com.xdr.policy.mapper.PolicyMapper;
import com.xdr.policy.model.Policy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyMapper policyMapper;
    private final com.xdr.policy.mapper.ResponseCommandMapper responseCommandMapper;

    /**
     * 获取针对特定Agent生效的策略 (优先级: AGENT > GROUP > GLOBAL)
     */
    public Policy getEffectivePolicy(String agentId, String groupId) {
        // ... (省略现有代码逻辑以便应用)
        return getPolicyLogic(agentId, groupId);
    }

    private Policy getPolicyLogic(String agentId, String groupId) {
        // 1. 检查 Agent 特定策略
        Policy agentPolicy = policyMapper.selectOne(new LambdaQueryWrapper<Policy>()
                .eq(Policy::getScope, "AGENT")
                .eq(Policy::getTargetId, agentId)
                .eq(Policy::getStatus, 1)
                .orderByDesc(Policy::getVersion)
                .last("LIMIT 1"));
        if (agentPolicy != null)
            return agentPolicy;

        // 2. 检查 Group 策略
        if (groupId != null) {
            Policy groupPolicy = policyMapper.selectOne(new LambdaQueryWrapper<Policy>()
                    .eq(Policy::getScope, "GROUP")
                    .eq(Policy::getTargetId, groupId)
                    .eq(Policy::getStatus, 1)
                    .orderByDesc(Policy::getVersion)
                    .last("LIMIT 1"));
            if (groupPolicy != null)
                return groupPolicy;
        }

        // 3. 全局策略
        return policyMapper.selectOne(new LambdaQueryWrapper<Policy>()
                .eq(Policy::getScope, "GLOBAL")
                .eq(Policy::getStatus, 1)
                .orderByDesc(Policy::getVersion)
                .last("LIMIT 1"));
    }

    public List<Policy> listPolicies() {
        return policyMapper.selectList(null);
    }

    public void savePolicy(Policy policy) {
        if (policy.getId() == null) {
            policy.setVersion(1);
            policyMapper.insert(policy);
        } else {
            Policy existing = policyMapper.selectById(policy.getId());
            policy.setVersion(existing.getVersion() + 1);
            policyMapper.updateById(policy);
        }
    }

    // --- Response Command Methods ---

    public void createCommand(com.xdr.policy.model.ResponseCommand command) {
        command.setStatus("PENDING");
        responseCommandMapper.insert(command);
    }

    public List<com.xdr.policy.model.ResponseCommand> getPendingCommands(String agentId) {
        return responseCommandMapper.selectList(new LambdaQueryWrapper<com.xdr.policy.model.ResponseCommand>()
                .eq(com.xdr.policy.model.ResponseCommand::getAgentId, agentId)
                .eq(com.xdr.policy.model.ResponseCommand::getStatus, "PENDING"));
    }

    public void updateCommandStatus(String commandId, String status, String error) {
        com.xdr.policy.model.ResponseCommand command = responseCommandMapper.selectById(commandId);
        if (command != null) {
            command.setStatus(status);
            command.setErrorMessage(error);
            responseCommandMapper.updateById(command);
        }
    }
}
