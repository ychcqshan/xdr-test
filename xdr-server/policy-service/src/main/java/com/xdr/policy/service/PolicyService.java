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

    /**
     * 获取对特定Agent生效的策略 (优先级: AGENT > GROUP > GLOBAL)
     */
    public Policy getEffectivePolicy(String agentId, String groupId) {
        // 1. 检查 Agent 特定策略
        Policy agentPolicy = policyMapper.selectOne(new LambdaQueryWrapper<Policy>()
                .eq(Policy::getScope, "AGENT")
                .eq(Policy::getTargetId, agentId)
                .eq(Policy::getStatus, 1)
                .orderByDesc(Policy::getVersion)
                .last("LIMIT 1"));
        if (agentPolicy != null) return agentPolicy;

        // 2. 检查 Group 策略
        if (groupId != null) {
            Policy groupPolicy = policyMapper.selectOne(new LambdaQueryWrapper<Policy>()
                    .eq(Policy::getScope, "GROUP")
                    .eq(Policy::getTargetId, groupId)
                    .eq(Policy::getStatus, 1)
                    .orderByDesc(Policy::getVersion)
                    .last("LIMIT 1"));
            if (groupPolicy != null) return groupPolicy;
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
}
