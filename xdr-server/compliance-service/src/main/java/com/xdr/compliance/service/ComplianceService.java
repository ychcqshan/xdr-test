package com.xdr.compliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xdr.compliance.mapper.ComplianceResultMapper;
import com.xdr.compliance.mapper.ComplianceStandardMapper;
import com.xdr.compliance.model.ComplianceResult;
import com.xdr.compliance.model.ComplianceStandard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ComplianceService {

    private final ComplianceStandardMapper standardMapper;
    private final ComplianceResultMapper resultMapper;

    public List<ComplianceStandard> listStandards() {
        return standardMapper.selectList(null);
    }

    public void saveStandard(ComplianceStandard standard) {
        if (standard.getId() == null) {
            standardMapper.insert(standard);
        } else {
            standardMapper.updateById(standard);
        }
    }

    public void reportResult(ComplianceResult result) {
        // 每个Agent对于每个标准只保留最新的一份结果
        ComplianceResult existing = resultMapper.selectOne(new LambdaQueryWrapper<ComplianceResult>()
                .eq(ComplianceResult::getAgentId, result.getAgentId())
                .eq(ComplianceResult::getStandardId, result.getStandardId())
                .last("LIMIT 1"));
        
        if (existing != null) {
            result.setId(existing.getId());
            resultMapper.updateById(result);
        } else {
            resultMapper.insert(result);
        }
    }

    public List<ComplianceResult> getResultsByAgent(String agentId) {
        return resultMapper.selectList(new LambdaQueryWrapper<ComplianceResult>()
                .eq(ComplianceResult::getAgentId, agentId));
    }
}
